package com.project.lol.webview.injections

object DownloadProgress {
    const val CONTENT = """
        (function () {
            var ACCENT = 'var(--spl-accent,#1DB954)';
            var ERROR_BG = '#e57373';
            var BOX_CSS = 'background:rgba(24,24,24,0.97);border:1px solid rgba(255,255,255,0.1);border-radius:10px;box-shadow:0 6px 20px rgba(0,0,0,0.5);padding:8px 12px;opacity:0;transition:opacity 0.22s ease,transform 0.22s ease;pointer-events:none;display:none;';
        
            var dlEl = null, labelEl = null, fillEl = null;
            var dlVisible = false;
            var dlHideTimer = null;
            var dlFixed = false;
        
            function expandedPlayer() {
                var p = document.getElementById('spotilolPlayerControls');
                return (p && !p.classList.contains('spl-mini')) ? p : null;
            }
        
            function baseTransform() {
                return dlFixed ? 'translateX(-50%) ' : '';
            }
        
            function createEl(player) {
                var el = document.createElement('div');
                el.id = 'spl-dl-progress';
                el.innerHTML =
                    '<div id="spl-dl-label" style="color:#fff;font-size:11px;font-weight:600;font-family:-apple-system,Roboto,sans-serif;margin-bottom:5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;"></div>' +
                    '<div style="height:3px;background:rgba(255,255,255,0.14);border-radius:2px;overflow:hidden;">' +
                        '<div id="spl-dl-fill" style="height:100%;width:0%;background:' + ACCENT + ';border-radius:2px;transition:width 0.2s linear;"></div>' +
                    '</div>';
        
                if (player) {
                    dlFixed = false;
                    el.style.cssText = BOX_CSS + 'position:absolute;bottom:100%;left:0;right:0;margin-bottom:8px;transform:translateY(14px);z-index:1;';
                    player.insertBefore(el, player.firstChild);
                } else {
                    dlFixed = true;
                    el.style.cssText = BOX_CSS + 'position:fixed;bottom:92px;left:50%;width:min(420px,calc(100vw - 32px));transform:translateX(-50%) translateY(14px);z-index:2147483647;';
                    document.body.appendChild(el);
                }
        
                labelEl = el.querySelector('#spl-dl-label');
                fillEl = el.querySelector('#spl-dl-fill');
                return el;
            }
        
            function popIn() {
                if (dlVisible) return;
                dlVisible = true;
                var el = dlEl;
                el.style.display = '';
                requestAnimationFrame(function () {
                    requestAnimationFrame(function () {
                        el.style.opacity = '1';
                        el.style.transform = baseTransform() + 'translateY(0)';
                    });
                });
            }
        
            function popOut() {
                if (!dlEl || !dlVisible) return;
                dlVisible = false;
                dlEl.style.opacity = '0';
                dlEl.style.transform = baseTransform() + 'translateY(14px)';
                dlHideTimer = setTimeout(function () {
                    if (!dlVisible) dlEl.style.display = 'none';
                }, 240);
            }
        
            window.splDownloadProgress = function (pct, label) {
                var player = expandedPlayer();
                var wantFixed = !player;
        
                if (dlEl && dlFixed !== wantFixed) {
                    clearTimeout(dlHideTimer);
                    dlEl.remove();
                    dlEl = labelEl = fillEl = null;
                    dlVisible = false;
                }
                if (!dlEl) dlEl = createEl(player);
        
                window.__splDlActive = (pct >= 0 && pct < 100);
                labelEl.textContent = label || '';
                clearTimeout(dlHideTimer);
        
                if (pct < 0) {
                    fillEl.style.width = '0%';
                    fillEl.style.background = ERROR_BG;
                    popIn();
                    dlHideTimer = setTimeout(popOut, 4000);
                } else {
                    fillEl.style.width = Math.min(pct, 100) + '%';
                    fillEl.style.background = ACCENT;
                    popIn();
                    if (pct >= 100) dlHideTimer = setTimeout(popOut, 2500);
                }
            };
        })();
    """
}