package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Worker Neutralizer.
 * GitHub: https://github.com/AldySan
 *
 * Injection is now gated by the "BlockServiceWorker" SharedPreferences
 * flag (default: true). When disabled, this injection is skipped entirely
 * in SpotifyWebViewClient.onPageStarted(), allowing Spotify's service worker
 * to register and operate normally.
 *
 * Video park/restore logic has been moved to VideoPark.kt and is injected
 * unconditionally - it is a battery optimization, not an SW feature.
 *
 * Unregisters Spotify's service worker and prevents re-registration.
 * The SW intercepts ALL network requests to check its cache map - pure
 * overhead on a mobile WebView that always has internet and has its own
 * HTTP cache + optional MITM proxy.
 *
 * Injected in onPageStarted so it runs before Spotify's scripts try to
 * register the SW via Workbox. The register() override returns a rejected
 * promise - Workbox catches this and continues without SW, falling back
 * to network-first behavior (which is what we want).
 *
 * Also throttles aggressive 250ms setInterval polling to 500ms.
 * The 250ms timers from web-player.js:215359 are non-core UI polling
 * that gets cleared/recreated in bursts. A gentle 2x throttle reduces
 * CPU wakeups by 50% with zero perceptible UX impact. Core intervals
 * (progress at 500ms, Connect at 1000ms) have different delays and
 * are untouched. PowerSave mode applies its own throttle upstream,
 * so this check never fires when PowerSave is active.
 */
object WorkerNeutralize {
    const val CONTENT = """
        (function(){
            if(window.__splWorkerNeutralized) return;
            window.__splWorkerNeutralized = true;

            /* === Service Worker: unregister + prevent re-registration === */
            if(navigator.serviceWorker){
                try {
                    navigator.serviceWorker.register = function(){
                        return Promise.reject(new Error('SW blocked by Spotilol'));
                    };
                } catch(e){}
                try {
                    navigator.serviceWorker.getRegistrations().then(function(regs){
                        regs.forEach(function(reg){
                            reg.unregister().then(function(ok){
                                if(ok){
                                    try{AndBridge.dbg('s','SW unregistered: '+reg.scope)}catch(e){}
                                }
                            }).catch(function(){});
                        });
                    }).catch(function(){});
                } catch(e){}
            }

            /* === Interval throttle: 250ms -> 500ms ===
               Gentle 2x throttle for burst polling timers.
               Core intervals (500ms progress, 1000ms Connect) 
               have different delays and pass through untouched.
               PowerSave modifies delay before this check fires,
               so no compounding occurs. */
            try {
                var origSI = window.setInterval.bind(window);
                window.setInterval = function(fn, delay){
                    if(delay === 250) delay = 500;
                    return origSI(fn, delay);
                };
            } catch(e){}
        })();
    """
}