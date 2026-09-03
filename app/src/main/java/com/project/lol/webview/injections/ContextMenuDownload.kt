package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Context Menu Download
 *
 * Adds a "Download" item to Spotify's track context menu (the "..."
 * button on a tracklist row, or right-click on the row itself).
 *
 * - The track (id/title/artists/album/cover) is harvested from the row
 *   DOM at menu-open time via capture-phase listeners on contextmenu
 *   and menu-trigger clicks. Non-track menus reset the capture, so a
 *   stale track can never leak into the wrong menu.
 * - Menus that must NEVER receive the item:
 *     1. The profile / user-widget dropdown - it ALSO carries
 *        data-testid="context-menu", so the observer used to inject a
 *        stale-track Download item into it (the wrapper
 *        [data-testid="user-widget-menu"] sits INSIDE the context-menu,
 *        so it's detected with a descendant query, not .closest()).
 *     2. Nested submenus opened from menuitems (Add to playlist,
 *        Share) - their trigger carries aria-expanded, and the capture
 *        is dropped the moment one is clicked.
 * - The item is built by cloning an existing plain menu entry at
 *   runtime, so hashed Encore classes, hover styling and icon sizing
 *   survive Spotify deploys. Only the icon path (the player's download
 *   glyph) and the label text are swapped.
 * - Clicking it hands the payload to AndBridge.downloadTrack - the
 *   exact same pipeline as the player's download button - then closes
 *   the menu with a synthesized Escape keydown (tippy-root detach as
 *   last-resort fallback).
 * - Injection runs from a MutationObserver (menu arriving in the DOM)
 *   plus a mouseover safety net that re-injects if a React re-render
 *   wipes the item mid-session. Visibility filtering skips the stale
 *   hidden context menus tippy leaves lying around in the DOM.
 */

object ContextMenuDownload {
    const val CONTENT = """
        (function(){
            if(window.__splCtxDlInit) return;
            window.__splCtxDlInit = true;
        
            var DL_ICON = 'M8 1a1 1 0 0 1 1 1v6.586l2.293-2.293a1 1 0 1 1 1.414 1.414l-4 4a1 1 0 0 1-1.414 0l-4-4a1 1 0 1 1 1.414-1.414L7 8.586V2a1 1 0 0 1 1-1zM2 13a1 1 0 0 1 1-1h10a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1z';
        
            window.__splMenuTrack = null;

            function splUpCover(url){
                if(!url) return '';
                return url.replace(/ab67616d0000[0-9a-f]{4}/i, 'ab67616d0000b273');
            }
        
            function splGrabRowTrack(row){
                if(!row) return null;
                var link = row.querySelector('a[data-testid="internal-track-link"]') ||
                           row.querySelector('a[href*="/track/"]');
                if(!link) return null;
                var m = (link.getAttribute('href')||'').match(/\/track\/([A-Za-z0-9]+)/);
                if(!m || !m[1]) return null;
                var artists = [];
                var arts = row.querySelectorAll('a[href*="/artist/"]');
                for(var i=0;i<arts.length;i++){
                    var nm = (arts[i].textContent||'').trim();
                    if(nm && artists.indexOf(nm) === -1) artists.push(nm);
                }
                var al = row.querySelector('a[href*="/album/"]');
                var img = row.querySelector('img');
                return {
                    trackId: m[1],
                    title: (link.textContent||'').trim(),
                    artist: artists.join(', '),
                    album: al ? (al.textContent||'').trim() : '',
                    cover: img ? splUpCover(img.src||'') : ''
                };
            }
        
            function splActiveMenu(){
                var menus = document.querySelectorAll('[data-testid="context-menu"]');
                for(var i=0;i<menus.length;i++){
                    var el = menus[i];
                    var r = el.getBoundingClientRect();
                    if(r.width < 2 || r.height < 2) continue;
                    var cs;
                    try{ cs = getComputedStyle(el); }catch(e){ continue; }
                    if(cs.visibility === 'hidden' || parseFloat(cs.opacity) < 0.02) continue;
                    return el;
                }
                return null;
            }

            function splIsUserMenu(root){
                try{
                    return !!(root && root.querySelector && root.querySelector('[data-testid="user-widget-menu"]'));
                }catch(e){ return false; }
            }
        
            function splDoMenuDownload(track){
                if(!track || !track.trackId){
                    try{ AndBridge.deferMessage('Track not ready'); }catch(e){}
                    return;
                }
                try{
                    AndBridge.downloadTrack(JSON.stringify({
                        trackId: track.trackId,
                        title: track.title || '',
                        artist: track.artist || '',
                        album: track.album || '',
                        cover: track.cover || ''
                    }));
                }catch(e){}
            }
        
            function splCloseMenu(){
                try{
                    var root = splActiveMenu();
                    var ul = root ? root.querySelector('ul[role="menu"]') : null;
                    if(ul){
                        ul.dispatchEvent(new KeyboardEvent('keydown', {
                            key: 'Escape', code: 'Escape',
                            keyCode: 27, which: 27,
                            bubbles: true, cancelable: true
                        }));
                    }
                }catch(e){}
                setTimeout(function(){
                    try{
                        var m = splActiveMenu();
                        if(m){
                            var tr = m.closest('[data-tippy-root]') || m.parentElement;
                            if(tr && tr.parentElement) tr.remove();
                        }
                    }catch(e){}
                }, 250);
            }
        
            function splInjectItem(ul, track){
                var src = null;
                var lis = ul.querySelectorAll('li[role="presentation"]');
                for(var i=0;i<lis.length;i++){
                    var li = lis[i];
                    if(li.classList.contains('spl-menu-dl')) continue;
                    var b = li.querySelector('button[role="menuitem"]');
                    if(!b) continue;
                    if(b.hasAttribute('aria-expanded')) continue;
                    if(li.querySelector('img')) continue;
                    src = li;
                    break;
                }
                if(!src) return false;
        
                var clone = src.cloneNode(true);
                clone.classList.add('spl-menu-dl');
        
                var btn = clone.querySelector('button[role="menuitem"]');
                btn.removeAttribute('id');
                btn.setAttribute('aria-label', 'Download');
        
                var svg = clone.querySelector('svg[data-encore-id="icon"]');
                if(svg){
                    while(svg.firstChild) svg.removeChild(svg.firstChild);
                    var p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                    p.setAttribute('d', DL_ICON);
                    svg.appendChild(p);
                }
        
                var spans = clone.querySelectorAll('span[data-encore-id]');
                if(spans.length){
                    spans[0].textContent = 'Download';
                } else {
                    var f = clone.querySelector('span');
                    if(f) f.textContent = 'Download';
                }
        
                btn.addEventListener('click', function(e){
                    e.preventDefault();
                    e.stopPropagation();
                    splDoMenuDownload(track);
                    splCloseMenu();
                }, true);
        
                ul.insertBefore(clone, ul.firstChild);
                return true;
            }
        
            function splTryInject(attempt){
                if(attempt > 25) return;
                if(window.__splBg) return;
                var t = window.__splMenuTrack;
                if(!t || !t.trackId) return;
                var root = splActiveMenu();
                if(!root){
                    setTimeout(function(){ splTryInject(attempt+1); }, 120);
                    return;
                }
                if(splIsUserMenu(root)){
                    window.__splMenuTrack = null;
                    return;
                }
                var ul = root.querySelector('ul[role="menu"]');
                if(!ul){
                    setTimeout(function(){ splTryInject(attempt+1); }, 120);
                    return;
                }
                if(ul.querySelector('.spl-menu-dl')) return;
                if(!splInjectItem(ul, t)){
                    setTimeout(function(){ splTryInject(attempt+1); }, 120);
                }
            }
        
            function splCaptureFromEvent(e){
                var row = null;
                try{
                    if(e.target && e.target.closest){
                        row = e.target.closest('div[data-testid="tracklist-row"]');
                    }
                }catch(err){}
                window.__splMenuTrack = splGrabRowTrack(row);
            }
        
            document.addEventListener('contextmenu', splCaptureFromEvent, true);
        
            document.addEventListener('click', function(e){
                var target = e.target;
                if(!target || !target.closest) return;

                try{
                    if(target.closest('button[data-testid="user-widget-link"], [data-testid="user-widget-menu"]')){
                        window.__splMenuTrack = null;
                        return;
                    }
                }catch(err){}

                try{
                    if(target.closest('[data-testid="context-menu"]') &&
                       target.closest('button[aria-expanded]')){
                        window.__splMenuTrack = null;
                        return;
                    }
                }catch(err){}

                var trig = null;
                try{
                    if(e.target && e.target.closest){
                        trig = e.target.closest('[aria-haspopup="menu"]');
                    }
                }catch(err){}
                if(!trig) return;
                splCaptureFromEvent(e);
            }, true);
        
            var obsPending = false;
            var obs = new MutationObserver(function(muts){
                if(window.__splBg) return;
                var hit = false;
                for(var i=0;i<muts.length && !hit;i++){
                    var a = muts[i].addedNodes;
                    for(var j=0;j<a.length;j++){
                        var n = a[j];
                        if(n.nodeType !== 1) continue;
                        if(n.id === 'context-menu' ||
                           n.getAttribute('data-testid') === 'context-menu' ||
                           n.querySelector('[data-testid="context-menu"]')){
                            hit = true;
                            break;
                        }
                    }
                }
                if(hit && !obsPending){
                    obsPending = true;
                    setTimeout(function(){
                        obsPending = false;
                        splTryInject(0);
                    }, 60);
                }
            });
            obs.observe(document.body, { childList: true, subtree: true });
        
            var splLastHover = 0;
            document.addEventListener('mouseover', function(e){
                if(Date.now() - splLastHover < 150) return;
                try{
                    if(e.target && e.target.closest && e.target.closest('[data-testid="context-menu"]')){
                        splLastHover = Date.now();
                        splTryInject(0);
                    }
                }catch(err){}
            }, true);
        })();
    """
}