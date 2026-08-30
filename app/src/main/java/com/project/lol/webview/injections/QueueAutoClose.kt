package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Queue Panel Auto-Close
 *
 * Closes the queue side panel whenever the user interacts with anything
 * that isn't the queue itself (lyrics, library, home, track links, etc.).
 * Detection: PanelHeader_CloseButton whose nearby h1 reads "Queue".
 * Panel state survives React re-renders since we re-scan on every click.
 */

object QueueAutoClose {
    const val CONTENT = """
        (function(){
            if(window.__splQueueAutoClose) return;
            window.__splQueueAutoClose = true;
        
            var QUEUE_SET = new Set(['queue','antrean','cola','coda','warteschlange','очередь','kolejka','fila']);
            var CLOSE_SEL  = '[data-testid="PanelHeader_CloseButton"]';
            var IGNORE_SEL = 'button[data-testid="control-button-queue"], #spl-queue, [role="menu"], [role="dialog"], [data-tippy-root]';
        
            var cache = null;
        
            function isQueueHeader(h1){
                return QUEUE_SET.has((h1.textContent || '').trim().toLowerCase());
            }
        
            function findQueuePanel(){
                var btns = document.querySelectorAll(CLOSE_SEL);
                if(!btns.length) return null;
        
                var mains = [];
                var m = document.querySelector('main');          if(m) mains.push(m);
                m = document.getElementById('main-view');        if(m) mains.push(m);
        
                var queueH1s = [], h1s = document.querySelectorAll('h1');
                for(var i = 0; i < h1s.length; i++){
                    if(isQueueHeader(h1s[i])) queueH1s.push(h1s[i]);
                }
                if(!queueH1s.length) return null;
        
                function overMainScope(node){
                    for(var i = 0; i < mains.length; i++){
                        if(node !== mains[i] && node.contains(mains[i])) return true;
                    }
                    return false;
                }
                function hasQueueH1(node){
                    for(var i = 0; i < queueH1s.length; i++){
                        if(node.contains(queueH1s[i])) return true;
                    }
                    return false;
                }
        
                for(var b = 0; b < btns.length; b++){
                    var btn = btns[b];
                    if(btn.closest('[aria-hidden="true"]')) continue;
                    var r = btn.getBoundingClientRect();
                    if(r.width < 2 || r.height < 2) continue;
        
                    var node = btn.parentElement, headerRoot = null;
                    for(var h = 0; h < 8 && node && node !== document.body; h++){
                        if(overMainScope(node)) break;
                        if(hasQueueH1(node)){ headerRoot = node; break; }
                        node = node.parentElement;
                    }
                    if(!headerRoot) continue;
        
                    var panel = headerRoot, p = headerRoot.parentElement;
                    while(p && p !== document.body && !overMainScope(p)){
                        panel = p;
                        p = p.parentElement;
                    }
                    var closeBtn = panel.querySelector(CLOSE_SEL + ' button') || btn;
                    return { panel: panel, closeBtn: closeBtn };
                }
                return null;
            }
        
            function getQueuePanel(){
                if(cache){
                    if(cache.panel.isConnected &&
                       cache.closeBtn.isConnected &&
                       !cache.closeBtn.closest('[aria-hidden="true"]')){
                        var r = cache.closeBtn.getBoundingClientRect();
                        if(r.width >= 2 && r.height >= 2) return cache;
                    }
                    cache = null;
                }
                cache = findQueuePanel();
                return cache;
            }
        
            function closeQueue(){
                var q = getQueuePanel();
                if(q){ try{ q.closeBtn.click(); }catch(e){} }
            }
        
            document.addEventListener('click', function(e){
                if(window.__splBg || !e.isTrusted) return;
                var t = e.target;
                if(!t || !t.closest) return;
        
                if(t.closest(IGNORE_SEL)) return;
        
                var q = getQueuePanel();
                if(!q || q.panel.contains(t)) return;
        
                try{ q.closeBtn.click(); }catch(err){}
            }, true);
        
            window.addEventListener('popstate', function(){
                if(window.__splBg) return;
                closeQueue();
            });
        })();
    """
}