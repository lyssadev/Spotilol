package com.project.lol.webview.injections

object DownloadProgress {
    const val CONTENT = """
            (function(){
                var dlEl = null;
                var dlVisible = false;
                var dlHideTimer = null;
                function createEl(){
                    var player = document.getElementById('spotilolPlayerControls');
                    if(!player) return null;
                    var el = document.createElement('div');
                    el.id = 'spl-dl-progress';
                    el.innerHTML = '<div id="spl-dl-label" style="color:#fff;font-size:11px;font-weight:600;font-family:-apple-system,Roboto,sans-serif;margin-bottom:5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;"></div>' +
                        '<div style="height:3px;background:rgba(255,255,255,0.14);border-radius:2px;overflow:hidden;"><div id="spl-dl-fill" style="height:100%;width:0%;background:var(--spl-accent,#1DB954);border-radius:2px;transition:width 0.2s linear;"></div></div>';
                    el.style.cssText = 'position:absolute;bottom:100%;left:0;right:0;margin-bottom:8px;background:rgba(24,24,24,0.97);border:1px solid rgba(255,255,255,0.1);border-radius:10px;box-shadow:0 6px 20px rgba(0,0,0,0.5);padding:8px 12px;opacity:0;transform:translateY(14px);transition:opacity 0.22s ease, transform 0.22s ease;pointer-events:none;display:none;z-index:1;';
                    player.insertBefore(el, player.firstChild);
                    return el;
                }
                function popIn(){
                    if(!dlEl){
                        dlEl = createEl();
                        if(!dlEl) return;
                    }
                    dlEl.style.display = '';
                    if(!dlVisible){
                        dlVisible = true;
                        requestAnimationFrame(function(){
                            requestAnimationFrame(function(){
                                dlEl.style.opacity = '1';
                                dlEl.style.transform = 'translateY(0)';
                            });
                        });
                    }
                }
                function popOut(){
                    if(!dlEl || !dlVisible) return;
                    dlVisible = false;
                    dlEl.style.opacity = '0';
                    dlEl.style.transform = 'translateY(14px)';
                    clearTimeout(dlHideTimer);
                    dlHideTimer = setTimeout(function(){ if(!dlVisible) dlEl.style.display = 'none'; }, 240);
                }
                window.splDownloadProgress = function(pct, label){
                    if(!dlEl) dlEl = createEl();
                    var labelEl = document.getElementById('spl-dl-label');
                    var fillEl = document.getElementById('spl-dl-fill');
                    if(labelEl) labelEl.textContent = label || '';
                    clearTimeout(dlHideTimer);
                    if(pct < 0){
                        fillEl.style.width = '0%';
                        fillEl.style.background = '#e57373';
                        popIn();
                        dlHideTimer = setTimeout(popOut, 4000);
                    } else if(pct >= 100){
                        fillEl.style.width = '100%';
                        fillEl.style.background = 'var(--spl-accent,#1DB954)';
                        popIn();
                        dlHideTimer = setTimeout(popOut, 2500);
                    } else {
                        fillEl.style.width = pct + '%';
                        fillEl.style.background = 'var(--spl-accent,#1DB954)';
                        popIn();
                    }
                };
            })();
        
    """
}