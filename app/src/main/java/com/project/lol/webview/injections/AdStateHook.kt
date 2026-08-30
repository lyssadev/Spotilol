package com.project.lol.webview.injections

/**
 * Page-level ad-state hook, ported from Blockify's page-hook.js.
 *
 * Injected from SpotifyWebViewClient.onPageStarted(), right after
 * FetchOverride, so the wrappers compose: this hook sits OUTSIDE the
 * FetchOverride wrapper and observes whatever comes back through it - in
 * proxy mode too (mngFetch's response shim exposes .json()/.clone(), so
 * cloning still works).
 *
 * What it does:
 *  1. Wraps window.fetch - any response whose URL touches the player state
 *     endpoints ("/state", "connect-state") gets cloned and parsed.
 *  2. Wraps window.WebSocket - every message (string / Blob / ArrayBuffer)
 *     is parsed. Dealer pushes arrive here in BOTH connection modes because
 *     WebSockets are never routed through mngFetch.
 *  3. Walks state_machine payloads, finds tracks flagged as ads
 *     (content_type / is_ad markers), and harvests their audio file IDs from
 *     the manifest (file_ids_mp3 / file_ids_external / file_ids /
 *     alternatives).
 *  4. Publishes the bounded ID list to the native layer via
 *     AndBridge.recAdContentIds() whenever it changes (max 32, deduped).
 *
 * The native side (AdIdStore + shouldInterceptRequest) then redirects any
 * media request whose URL contains one of those IDs to silent.mp3.
 *
 * No DOM dependency (unlike Blockify's DOM-attribute bridge): Android's
 * WebView main-world injection can call the JS bridge directly.
 */

object AdStateHook {
    const val CONTENT = """
        (function(){
            if (window.__splAdStateHook) return;
            window.__splAdStateHook = 1;
            var MAX_IDS = 32;
            var MAX_JSON = 8000000;
            var knownIds = {};
            var idOrder = [];
            var lastPub = '';
            var dirty = false;
            var textDecoder = null;
            var ID_RE = /^[a-zA-Z0-9_-]{8,128}$/;

            function safeId(v){
                return typeof v === 'string' && ID_RE.test(v);
            }
            function remember(v){
                if (!safeId(v) || knownIds[v]) return;
                knownIds[v] = 1;
                idOrder.push(v);
                dirty = true;
                while (idOrder.length > MAX_IDS) {
                    delete knownIds[idOrder.shift()];
                }
            }
            function publish(){
                if (!dirty) return;
                var ser;
                try { ser = JSON.stringify(idOrder); } catch(e){ return; }
                dirty = false;
                if (ser === lastPub) return;
                lastPub = ser;
                try { AndBridge.recAdContentIds(ser); } catch(e){}
                try { AndBridge.dbg('i', 'adIds=' + idOrder.length); } catch(e){}
            }
            function isAdTrack(t){
                if (!t) return false;
                var m = t.metadata || {};
                var cands = [
                    t.content_type, t.contentType, t.type,
                    m.content_type, m.contentType,
                    m.is_ad, m.isAd,
                    t.is_ad, t.isAd
                ];
                for (var i = 0; i < cands.length; i++) {
                    var v = cands[i];
                    if (v === true) return true;
                    if (typeof v === 'string') {
                        var u = v.toUpperCase();
                        if (u === 'AD' || u === 'ADVERTISEMENT' || u === 'TRUE') return true;
                    }
                }
                return false;
            }
            function harvestManifest(man){
                if (!man || typeof man !== 'object') return;
                var groups = [man.file_ids_mp3, man.file_ids_external, man.file_ids, man.alternatives];
                for (var g = 0; g < groups.length; g++) {
                    var group = groups[g];
                    if (!group || !group.length) continue;
                    for (var i = 0; i < group.length; i++) {
                        var e = group[i];
                        if (typeof e === 'string') { remember(e); }
                        else if (e) { remember(e.file_id); remember(e.fileId); remember(e.id); }
                    }
                }
            }
            function inspectTracks(tracks){
                if (!tracks || !tracks.length) return;
                for (var i = 0; i < tracks.length; i++) {
                    var t = tracks[i];
                    if (!isAdTrack(t)) continue;
                    harvestManifest(t.manifest);
                    remember(t.file_id);
                    remember(t.fileId);
                }
            }
            function inspectStateMachine(sm){
                if (!sm || typeof sm !== 'object') return;
                inspectTracks(sm.tracks);
                inspectTracks(sm.track_list);
                if (sm.queue && typeof sm.queue === 'object') inspectTracks(sm.queue.tracks);
            }
            function inspectPayload(p){
                if (!p || typeof p !== 'object') return;
                inspectStateMachine(p.state_machine);
                inspectStateMachine(p.stateMachine);
                if (p.payloads && p.payloads.length) {
                    for (var i = 0; i < p.payloads.length; i++) {
                        var e = p.payloads[i];
                        if (e && typeof e === 'object') {
                            inspectStateMachine(e.state_machine);
                            inspectStateMachine(e.stateMachine);
                        }
                    }
                }
                publish();
            }
            function parseJson(v){
                if (typeof v !== 'string' || v.length > MAX_JSON) return null;
                try { return JSON.parse(v); } catch(e){ return null; }
            }
            function inspectSocketData(v){
                if (typeof v === 'string') {
                    // Cheap gate: inspectPayload only reads state_machine /
                    // stateMachine keys, so a message without either substring
                    // can never yield IDs - skip JSON.parse entirely.
                    if (v.indexOf('state_machine') === -1 && v.indexOf('stateMachine') === -1) return;
                    var p = parseJson(v);
                    if (p) inspectPayload(p);
                    return;
                }
                if (typeof Blob === 'function' && v instanceof Blob) {
                    if (v.size <= MAX_JSON) v.text().then(inspectSocketData).catch(function(){});
                    return;
                }
                if (typeof ArrayBuffer === 'function' && v instanceof ArrayBuffer) {
                    if (v.byteLength <= MAX_JSON) {
                        try {
                            if (!textDecoder) textDecoder = new TextDecoder();
                            inspectSocketData(textDecoder.decode(v));
                        } catch(e){}
                    }
                }
            }

            // fetch wrapper - sits OUTSIDE FetchOverride's wrapper, so state
            // responses are visible in normal AND proxy mode (mngFetch shim
            // exposes .json()). The original promise is returned untouched.
            var pageFetch = window.fetch;
            if (typeof pageFetch === 'function') {
                window.fetch = function(input, init){
                    var p = pageFetch.apply(this, arguments);
                    try {
                        var url = (typeof input === 'string') ? input : ((input && input.url) || '');
                        if (url.indexOf('/state') !== -1 || url.indexOf('connect-state') !== -1) {
                            Promise.resolve(p).then(function(res){
                                try {
                                    var c = res.clone();
                                    c.json().then(inspectPayload).catch(function(){});
                                } catch(e){}
                            }).catch(function(){});
                        }
                    } catch(e){}
                    return p;
                };
            }

            // WebSocket wrapper - dealer deltas for every track change arrive
            // here regardless of connection mode. Plain constructor function
            // instead of Proxy for maximum WebView compatibility.
            var NativeWS = window.WebSocket;
            if (typeof NativeWS === 'function' && !NativeWS.__splWrapped) {
                var WrappedWS = function(url, protocols){
                    var sock = protocols === undefined ? new NativeWS(url) : new NativeWS(url, protocols);
                    sock.addEventListener('message', function(ev){
                        try { inspectSocketData(ev.data); } catch(e){}
                    });
                    return sock;
                };
                WrappedWS.prototype = NativeWS.prototype;
                WrappedWS.CONNECTING = NativeWS.CONNECTING;
                WrappedWS.OPEN = NativeWS.OPEN;
                WrappedWS.CLOSING = NativeWS.CLOSING;
                WrappedWS.CLOSED = NativeWS.CLOSED;
                WrappedWS.__splWrapped = true;
                window.WebSocket = WrappedWS;
            }
        })();
    """
}