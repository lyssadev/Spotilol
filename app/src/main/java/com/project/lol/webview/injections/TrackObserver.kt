package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Track Observer.
 * Watches the now-playing widget and exposes the current track globally:
 *   window.splTrackId      -> "6rqhFgbbKwnb9MLmUQDhG6" or null
 *   window.splTrackUri     -> "spotify:track:6rqhF..." or null
 *   window.splOnTrackChange(fn) -> subscribe; fn(uri, id) fires immediately
 *                                   if a track is already playing. Returns unsubscribe.
 *   'trackchange' CustomEvent (detail = uri) kept for event-style consumers.
 * Cheap path: anchor href + regex. Fallback: React Fiber props walk (throttled).
 * Watchdog handles widget mount/unmount/remount (fullscreen etc.).
 */

object TrackObserver {
    const val CONTENT = """
        (function(){
            window.__npStop && window.__npStop();

            const SEL   = '[data-testid="now-playing-widget"]';
            const LINK  = 'a[href*="/track/"]';
            const ID_RE = /\/track\/([a-zA-Z0-9]{22})/;
            const FIBER = '__reactFiber$';

            window.splTrackId = null;
            window.splTrackUri = null;
            window.__splTrackListeners = [];
            window.splOnTrackChange = function(fn){
                if(typeof fn !== 'function') return function(){};
                window.__splTrackListeners.push(fn);
                if(window.splTrackUri){
                    try{ fn(window.splTrackUri, window.splTrackId); }catch(e){}
                }
                return function(){
                    var i = window.__splTrackListeners.indexOf(fn);
                    if(i >= 0) window.__splTrackListeners.splice(i, 1);
                };
            };

            let root = null, anchor = null, last = null, lastFiberAt = 0, watchdog = null, fiberKey = null;

            function currentId() {
                if (!root) return null;
                if (!anchor || !anchor.isConnected) anchor = root.querySelector(LINK);
                if (!anchor) return null;
                const href = anchor.getAttribute('href');
                if (!href) return null;
                const m = ID_RE.exec(href);
                return m ? m[1] : null;
            }

            function uriFromProps(p) {
                if (!p || typeof p !== 'object') return null;
                for (const k in p) {
                    const v = p[k];
                    if (typeof v === 'string' && v.startsWith('spotify:track:')) return v;
                    if (v && typeof v.uri === 'string' && v.uri.startsWith('spotify:track:')) return v.uri;
                }
                return null;
            }

            function fiberURI() {
                if (!root) return null;
                if (!fiberKey) {
                    fiberKey = Object.keys(root).find(k => k.startsWith(FIBER));
                    if (!fiberKey) return null;
                }
                const f = root[fiberKey];
                if (!f) return null;

                const stack = [f];
                while (stack.length) {
                    const n = stack.pop();
                    if (!n) continue;
                    const hit = uriFromProps(n.memoizedProps);
                    if (hit) return hit;
                    if (n.child) stack.push(n.child);
                    if (n.sibling) stack.push(n.sibling);
                }

                for (let n = f.return; n; n = n.return) {
                    const hit = uriFromProps(n.memoizedProps);
                    if (hit) return hit;
                }
                return null;
            }

            function emit(id, via) {
                const uri = 'spotify:track:' + id;
                if (uri === last) return;
                last = uri;
                window.splTrackId = id;
                window.splTrackUri = uri;
                try{ AndBridge.dbg('s','[track] ' + uri + ' (' + via + ')'); }catch(e){}
                window.dispatchEvent(new CustomEvent('trackchange', { detail: uri }));
                const listeners = window.__splTrackListeners;
                for(var i = 0; i < listeners.length; i++){
                    try{ listeners[i](uri, id); }catch(e){}
                }
            }

            function check() {
                const id = currentId();
                if (id) return emit(id, 'href');

                const now = performance.now();
                if (now - lastFiberAt < 250) return;
                lastFiberAt = now;

                const uri = fiberURI();
                if (uri) emit(uri.split(':').pop(), 'fiber');
            }

            const obs = new MutationObserver(check);

            function attach(el) {
                root = el; anchor = null; fiberKey = null;
                obs.disconnect();
                obs.observe(el, {
                    subtree: true,
                    childList: true,
                    attributes: true,
                    attributeFilter: ['href']
                });
                let id = currentId();
                if (!id) {
                    const uri = fiberURI();
                    if (uri) id = uri.split(':').pop();
                }
                if (id) emit(id, 'init');
            }

            function detach() { obs.disconnect(); root = anchor = fiberKey = null; }

            function stop() {
                detach();
                clearInterval(watchdog);
                window.__npStop = undefined;
                window.splTrackId = null;
                window.splTrackUri = null;
            }

            watchdog = setInterval(function(){
                const el = document.querySelector(SEL);
                if (el && el !== root) attach(el);
                else if (!el && root) detach();
            }, 1000);

            window.__npStop = stop;

            const el0 = document.querySelector(SEL);
            if (el0) {
                attach(el0);
                try{ AndBridge.dbg('s','np-observer active'); }catch(e){}
            } else {
                try{ AndBridge.dbg('s','np-observer armed, waiting for widget'); }catch(e){}
            }
        })();
    """
}