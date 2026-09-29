package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Power Save Mode.
 * GitHub: https://github.com/AldySan
 */
object PowerSave {
    const val CONTENT = """
        (function(){
            if(window.__splPsInstalled) return;
            window.__splPsInstalled = true;

            var videoObs = null;
            var psStyle = null;
            var origSI = null;

            function isCanvasVideo(v){
                try{
                    if(v.muted) return true;
                    if(v.hasAttribute && v.hasAttribute('loop')) return true;
                    if(v.style && v.style.objectFit === 'cover') return true;
                    if(v.closest && (v.closest('[data-testid=canvas-container]') || v.closest('[data-testid=canvas]'))) return true;
                }catch(e){}
                return false;
            }

            function freezeVideo(v){
                if(!isCanvasVideo(v)) return;
                if(v.__splFrozen) return;
                v.__splFrozen = true;
                try{ v.removeAttribute('autoplay'); }catch(e){}
                try{ v.pause(); }catch(e){}
                try{ v.preload = 'none'; }catch(e){}
                try{
                    Object.defineProperty(v, 'play', {
                        value: function(){ return Promise.resolve(); },
                        configurable: true,
                        writable: true
                    });
                }catch(e){}
            }

            function sweepVideos(){
                try{
                    var vs = document.querySelectorAll('video');
                    for(var i=0;i<vs.length;i++) freezeVideo(vs[i]);
                }catch(e){}
            }

            function unfreezeAll(){
                try{
                    var vs = document.querySelectorAll('video');
                    for(var i=0;i<vs.length;i++){
                        var v = vs[i];
                        if(!v.__splFrozen) continue;
                        v.__splFrozen = false;
                        try{ delete v.play; }catch(e){}
                        try{ v.preload = 'auto'; }catch(e){}
                        var src = v.currentSrc || v.src || '';
                        if(/^https?:/i.test(src)){
                            try{ v.load(); }catch(e){}
                        }
                        try{
                            var p = v.play();
                            if(p && p.catch) p.catch(function(){});
                        }catch(e){}
                    }
                }catch(e){}
            }

            window.__splApplyPowerSave = function(on){
                window.__splPowerSavePref = !!on;
                if(on){
                    if(window.__splPsActive) return;
                    window.__splPsActive = true;

                    sweepVideos();
                    videoObs = new MutationObserver(function(muts){
                        for(var i=0;i<muts.length;i++){
                            var a = muts[i].addedNodes;
                            for(var j=0;j<a.length;j++){
                                var n = a[j];
                                if(n.nodeType !== 1) continue;
                                if(n.tagName === 'VIDEO'){ freezeVideo(n); }
                                else if(n.querySelectorAll){
                                    var vs = n.querySelectorAll('video');
                                    for(var k=0;k<vs.length;k++) freezeVideo(vs[k]);
                                }
                            }
                        }
                    });
                    try{ videoObs.observe(document.body || document.documentElement, {childList:true, subtree:true}); }catch(e){}

                    psStyle = document.createElement('style');
                    psStyle.id = 'spl-power-save';
                    psStyle.textContent = '[data-testid=canvas-container],[data-testid=canvas],[class*=CanvasPlayer],[class*=CanvasVideo]{display:none!important}';
                    function appendStyle(){
                        var t = document.head || document.documentElement;
                        if(t && !document.getElementById('spl-power-save')) t.appendChild(psStyle);
                    }
                    appendStyle();
                    document.addEventListener('DOMContentLoaded', appendStyle);

                    origSI = window.setInterval.bind(window);
                    window.setInterval = function(fn, delay){
                        if(window.__splPsActive){
                            var d = Number(delay) || 0;
                            if(d >= 100 && d < 5000){ delay = Math.max(d*3, 1500); }
                            else if(d >= 5000 && d < 10000){ delay = 10000; }
                        }
                        return origSI(fn, delay);
                    };

                    window.__splPsRafMs = 500;
                } else {
                    if(!window.__splPsActive) return;
                    window.__splPsActive = false;
                    try{ if(videoObs) videoObs.disconnect(); }catch(e){}
                    videoObs = null;
                    try{ if(psStyle) psStyle.remove(); }catch(e){}
                    psStyle = null;
                    if(origSI){ try{ window.setInterval = origSI; }catch(e){} }
                    unfreezeAll();
                    window.__splPsRafMs = 0;
                }
            };

            window.__splApplyPowerSave(!!window.__splPowerSavePref);
        })();
    """
}