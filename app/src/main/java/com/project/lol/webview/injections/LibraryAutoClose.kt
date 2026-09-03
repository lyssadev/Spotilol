package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Library Auto-Close
 *
 * Closes the library whenever the user interacts with anything
 * that isn't the library itself - same pattern as QueueAutoClose.
 *
 * Two library shapes are handled:
 *  1. Native side panel "Your Library" (PanelHeader_CloseButton whose
 *     header h1 reads "Your Library") - closed via its own close button.
 *  2. CssHack expanded left sidebar (fullscreen overlay, zIndex 20) -
 *     collapsed via the library toggle button (lBtn), the same click
 *     switchLs listens to. Grid items keep their existing close-on-click
 *     handler (lbit), so track picks still collapse + play.
 *
 * Nav links (Home/Search) inside the expanded sidebar also collapse it,
 * since they navigate away and would otherwise leave the overlay
 * stranded on top of the new page.
 */

object LibraryAutoClose {
    const val CONTENT = """
        (function () {
            if (window.__splLibraryAutoClose) return;
            window.__splLibraryAutoClose = true;
        
            var LIBRARY_SET = new Set(['your library','pustaka kamu','pustakamu','tu biblioteca','la tua libreria','deine mediathek','ваша библиотека','twoja biblioteka','sua biblioteca','votre bibliothèque','ta bibliothèque','jouw bibliotheek','ditt bibliotek','kitaplığın','ライブラリ','라이브러리']);
            var CLOSE_SEL   = '[data-testid="PanelHeader_CloseButton"]';
            var HIDDEN_SEL  = '[aria-hidden="true"]';
            var IGNORE_SEL  = 'button[data-testid="library-button"], [role="menu"], [role="dialog"], [data-tippy-root]';
            var SIDEBAR_SEL = '#Desktop_LeftSidebar_Id';
            var SKIP_SEL    = HIDDEN_SEL + ', ' + SIDEBAR_SEL;
        
            var cache = null;
        
            function isLibraryHeader(h1) {
                return LIBRARY_SET.has((h1.textContent || '').trim().toLowerCase());
            }
        
            function findLibraryPanel() {
                var btns = document.querySelectorAll(CLOSE_SEL);
                if (!btns.length) return null;
        
                var mains = [], m;
                if ((m = document.querySelector('main'))) mains.push(m);
                if ((m = document.getElementById('main-view'))) mains.push(m);
        
                var h1s = document.querySelectorAll('h1'), libH1s = [];
                for (var i = 0; i < h1s.length; i++) {
                    if (isLibraryHeader(h1s[i])) libH1s.push(h1s[i]);
                }
                if (!libH1s.length) return null;
        
                function overMainScope(node) {
                    for (var i = 0; i < mains.length; i++) {
                        if (node !== mains[i] && node.contains(mains[i])) return true;
                    }
                    return false;
                }
        
                for (var b = 0; b < btns.length; b++) {
                    var btn = btns[b];
                    if (btn.closest(SKIP_SEL)) continue;
                    var r = btn.getBoundingClientRect();
                    if (r.width < 2 || r.height < 2) continue;
        
                    var node = btn.parentElement, headerRoot = null;
                    for (var h = 0; h < 8 && node && node !== document.body; h++, node = node.parentElement) {
                        if (overMainScope(node)) break;
                        for (var j = 0; j < libH1s.length; j++) {
                            if (node.contains(libH1s[j])) { headerRoot = node; break; }
                        }
                        if (headerRoot) break;
                    }
                    if (!headerRoot) continue;
        
                    var panel = headerRoot, p = headerRoot.parentElement;
                    while (p && p !== document.body && !overMainScope(p)) {
                        panel = p;
                        p = p.parentElement;
                    }
                    return { panel: panel, closeBtn: panel.querySelector(CLOSE_SEL + ' button') || btn };
                }
                return null;
            }
        
            function getLibraryPanel() {
                if (cache) {
                    if (cache.panel.isConnected &&
                        cache.closeBtn.isConnected &&
                        !cache.closeBtn.closest(HIDDEN_SEL)) {
                        var r = cache.closeBtn.getBoundingClientRect();
                        if (r.width >= 2 && r.height >= 2) return cache;
                    }
                    cache = null;
                }
                return (cache = findLibraryPanel());
            }
        
            function sidebarEl() {
                return document.querySelector(SIDEBAR_SEL);
            }
        
            function sidebarExpanded(ls) {
                return ls.style.zIndex === '20' ||
                       (ls.style.position === 'fixed' && ls.style.width === '100%');
            }
        
            function collapseSidebar() {
                var lb = window.lBtn, ls;
                if (!lb && (ls = sidebarEl())) {
                    lb = ls.querySelector('header>div>div:first-child button');
                }
                if (lb) { try { lb.click(); } catch (e) {} }
            }
        
            function closeLibrary() {
                var lib = getLibraryPanel();
                if (lib) { try { lib.closeBtn.click(); } catch (e) {} }
                var ls = sidebarEl();
                if (ls && sidebarExpanded(ls)) collapseSidebar();
            }
        
            document.addEventListener('click', function (e) {
                if (window.__splBg || !e.isTrusted) return;
                var t = e.target;
                if (!t || !t.closest) return;
                if (t.closest(IGNORE_SEL)) return;
        
                var ls = sidebarEl();
        
                var lib = getLibraryPanel();
                if (lib && !lib.panel.contains(t)) {
                    try { lib.closeBtn.click(); } catch (err) {}
                }
        
                if (ls && sidebarExpanded(ls)) {
                    var fold = !ls.contains(t) ||
                               (!t.closest('div[role=grid]') &&
                                t.closest(SIDEBAR_SEL + ' nav a[href]'));
                    if (fold) setTimeout(collapseSidebar, 0);
                }
            }, true);
        
            window.addEventListener('popstate', function () {
                if (window.__splBg) return;
                closeLibrary();
            });
        })();
    """
}