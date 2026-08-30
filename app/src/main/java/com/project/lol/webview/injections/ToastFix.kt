package com.project.lol.webview.injections

/*
 * CREDIT: Spotilol - Toast Restyle.
 * GitHub: https://github.com/AldySan
 */
object ToastFix {
    const val CONTENT = """
        (function(){
            if (window.__splToastFixInit) return;
            window.__splToastFixInit = true;

            var GAP = 10;
            var STACK = 56;
            var GHOST_MS = 300;
            var POLL_DEAD = 2;
            var POLL_GRACE = 600;
            var PIN_ARM_MS = 600;
            var SWIPE_MIN = 72;

            var st = document.createElement('style');
            st.id = 'spl-toast-fix';
            st.textContent = [
                '.notistack-SnackbarContainer{',
                '  transform:none!important;',
                '  z-index:2147483647!important;',
                '  pointer-events:none!important',
                '}',
                '.notistack-Snackbar{',
                '  transform:none!important;',
                '  margin:0!important;',
                '  padding:0!important;',
                '  pointer-events:none!important',
                '}',
                'div[role="alert"][aria-live]{',
                '  transition:bottom .25s cubic-bezier(.2,.8,.2,1),transform .25s cubic-bezier(.2,.8,.2,1),opacity .2s ease!important;',
                '  touch-action:none!important',
                '}',
                'div[role="alert"][aria-live].spl-live{',
                '  opacity:1!important;',
                '  visibility:visible!important;',
                '  transform:translate(-50%,0)!important',
                '}',
                'div[role="alert"][aria-live] img[data-testid="entity-image"]{',
                '  border-radius:50%!important;',
                '  object-fit:cover!important',
                '}',
                'div[role="alert"][aria-live] [data-encore-id="text"]{',
                '  color:#fff!important;',
                '  font-weight:600!important;',
                '  font-size:13px!important;',
                '  letter-spacing:.2px!important;',
                '  white-space:nowrap!important;',
                '  max-width:56vw!important;',
                '  overflow:hidden!important;',
                '  text-overflow:ellipsis!important',
                '}',
                'div[role="alert"][aria-live] button{',
                '  background:rgba(255,255,255,.1)!important;',
                '  color:#fff!important;',
                '  border:none!important;',
                '  border-radius:9999px!important;',
                '  padding:8px 14px!important;',
                '  font-size:12px!important;',
                '  font-weight:700!important;',
                '  text-transform:uppercase!important;',
                '  letter-spacing:.5px!important;',
                '  cursor:pointer!important;',
                '  min-height:36px!important;',
                '  margin-left:4px!important;',
                '  transition:background .15s,transform .1s!important',
                '}',
                'div[role="alert"][aria-live] button:hover{background:rgba(var(--spl-accent-rgb,29,185,84),.3)!important}',
                'div[role="alert"][aria-live] button:active{transform:scale(.95)!important}',
                '@keyframes splToastIn{',
                '  0%{opacity:0;transform:translate(-50%,16px) scale(.94)}',
                '  60%{opacity:1;transform:translate(-50%,-3px) scale(1.02)}',
                '  100%{opacity:1;transform:translate(-50%,0) scale(1)}',
                '}',
                '@keyframes splToastOut{',
                '  0%{opacity:1;transform:translate(-50%,0) scale(1)}',
                '  100%{opacity:0;transform:translate(-50%,16px) scale(.94)}',
                '}'
            ].join('\n');

            function splAppend(){
                var t = document.head || document.documentElement;
                if (t && !document.getElementById('spl-toast-fix')) t.appendChild(st);
            }
            splAppend();

            function splLiberate(el){
                var p = el.parentElement, hops = 0;
                while (p && p !== document.body && hops++ < 15) {
                    var pcs = getComputedStyle(p);
                    if (pcs.zIndex !== 'auto') {
                        p.style.setProperty('z-index','2147483647','important');
                    }
                    if (pcs.transform !== 'none') {
                        p.style.setProperty('transform','none','important');
                    }
                    p = p.parentElement;
                }
            }

            var tracked = [];
            function track(el){ tracked.push(el); }
            function untrack(el){ var i = tracked.indexOf(el); if (i >= 0) tracked.splice(i, 1); }

            function dressedCount(){
                var n = 0, all = document.querySelectorAll('div[role="alert"][aria-live]');
                for (var i = 0; i < all.length; i++) {
                    if (all[i].__splDressed && !all[i].__splGhost) n++;
                }
                return n;
            }

            function floatedSyncCount(){
                var b = document.querySelectorAll('.spl-sync-btn'), n = 0;
                for (var i = 0; i < b.length; i++) {
                    if (b[i].__splFloated && b[i].isConnected && !b[i].__splGhost) n++;
                }
                return n;
            }

            function place(el, idx){
                var vh = (typeof window.splViewH === 'function') ? window.splViewH()
                        : (document.documentElement.clientHeight || window.innerHeight || 600);
                var pt = (typeof window.splPlayerTop === 'function') ? window.splPlayerTop() : vh;
                var above = Math.max(10, vh - pt) + GAP + floatedSyncCount() * 52 + idx * STACK;
                var roof = vh - 60;
                if (above > roof) above = roof;
                el.style.setProperty('bottom', Math.round(above) + 'px', 'important');
            }

            function armLivePin(el){
                if (el.__splPinWired) return;
                el.__splPinWired = true;
                function arm(){
                    if (el.__splGhost) return;
                    el.classList.add('spl-live');
                }
                el.addEventListener('animationend', function(e){
                    if (e.animationName === 'splToastIn') arm();
                });
                setTimeout(arm, PIN_ARM_MS);
            }

            function ghostOpts(el){
                var dx = el.__splSwipeDX || 0;
                if (Math.abs(dx) > 30) return { dir: dx < 0 ? 'left' : 'right', fromDX: dx };
                return {};
            }

            function mountGhost(g, opts){
                opts = opts || {};
                g.classList.remove('spl-live');
                document.body.appendChild(g);
                var startOp = 1;
                try {
                    startOp = parseFloat(getComputedStyle(g).opacity);
                    if (isNaN(startOp) || startOp <= 0.02) startOp = 1;
                } catch (e) { startOp = 1; }
                g.style.removeProperty('transform');
                g.style.removeProperty('opacity');
                g.style.removeProperty('animation');
                g.style.setProperty('transition','none','important');
                g.style.setProperty('pointer-events','none','important');
                g.style.setProperty('visibility','visible','important');
                var killed = false;
                function kill(){ if (killed) return; killed = true; try { g.remove(); } catch (e) {} }
                if ((opts.dir === 'left' || opts.dir === 'right') && g.animate){
                    var dx = Math.round(opts.fromDX || 0);
                    var sign = opts.dir === 'left' ? -1 : 1;
                    var vw = document.documentElement.clientWidth || 800;
                    var anim = g.animate([
                        { opacity: startOp, transform: 'translate(calc(-50% + ' + dx + 'px),0)' },
                        { opacity: 0, transform: 'translate(calc(-50% + ' + (dx + sign * (vw * 0.7 + 200)) + 'px),0)' }
                    ], { duration: 300, easing: 'cubic-bezier(.4,0,.2,1)', fill: 'both' });
                    anim.addEventListener('finish', kill);
                    setTimeout(kill, 800);
                } else {
                    g.style.setProperty('animation','splToastOut .28s cubic-bezier(.4,0,.2,1) forwards','important');
                    g.addEventListener('animationend', kill);
                    setTimeout(kill, GHOST_MS + 150);
                }
            }

            function takeover(el){
                if (!el || el.__splGhost) return;
                el.__splGhost = true;
                untrack(el);
                detachWatchers(el);
                try {
                    var g = el.cloneNode(true);
                    g.__splGhost = true;
                    g.__splDressed = true;
                    g.style.cssText = el.style.cssText;
                    mountGhost(g, ghostOpts(el));
                } catch (e) {}
                el.style.setProperty('display','none','important');
            }

            function splGhost(el){
                if (!el || el.__splGhost) return;
                el.__splGhost = true;
                untrack(el);
                detachWatchers(el);
                try { mountGhost(el, ghostOpts(el)); } catch (e) {}
            }

            function extractGhost(node){
                if (!node || node.nodeType !== 1 || node.__splGhost) return;
                var t = null;
                if (node.matches && node.matches('div[role="alert"][aria-live]') && node.__splDressed) {
                    t = node;
                } else if (node.querySelectorAll) {
                    var found = node.querySelectorAll('div[role="alert"][aria-live]');
                    for (var i = 0; i < found.length; i++) {
                        if (found[i].__splDressed && !found[i].__splGhost) t = found[i];
                    }
                }
                if (t) splGhost(t);
            }

            function stompedInline(node){
                if (node.style.visibility === 'hidden') return true;
                if (node.style.display === 'none') return true;
                var o = node.style.opacity;
                if (o !== '' && parseFloat(o) < 0.02) return true;
                return false;
            }
            function watchForExit(el){
                if (el.__splWatch) return;
                var w = el.parentElement;
                var obs = new MutationObserver(function(){
                    if (el.__splGhost) return;
                    if (Date.now() - (el.__splBorn || 0) < POLL_GRACE) return;
                    if (stompedInline(el)) { takeover(el); return; }
                    if (w && stompedInline(w)) takeover(el);
                });
                obs.observe(el, { attributes: true, attributeFilter: ['style','class'] });
                if (w) obs.observe(w, { attributes: true, attributeFilter: ['style','class'] });
                el.__splWatch = obs;
            }
            function detachWatchers(el){
                try { if (el.__splWatch) { el.__splWatch.disconnect(); el.__splWatch = null; } } catch (e) {}
            }

            function wireSwipe(el){
                if (el.__splSwipeWired) return;
                el.__splSwipeWired = true;
                var sx = 0, sy = 0, dx = 0, dragging = false;
                function snapBack(){
                    dragging = false; sx = 0; dx = 0;
                    el.__splSwipeDX = 0;
                    el.style.removeProperty('transition');
                    el.style.removeProperty('transform');
                    el.style.removeProperty('opacity');
                    if (!el.__splGhost) el.classList.add('spl-live');
                }
                el.addEventListener('touchstart', function(e){
                    if (e.touches.length !== 1) return;
                    sx = e.touches[0].clientX; sy = e.touches[0].clientY;
                    dx = 0; dragging = false;
                }, {passive:true});
                el.addEventListener('touchmove', function(e){
                    if (!sx) return;
                    var t = e.touches[0];
                    dx = t.clientX - sx;
                    if (!dragging){
                        var dy = t.clientY - sy;
                        if (Math.abs(dx) > 12 && Math.abs(dx) > Math.abs(dy)) {
                            dragging = true;
                            el.classList.remove('spl-live');
                            el.style.setProperty('transition','none','important');
                        } else return;
                    }
                    if (e.cancelable) e.preventDefault();
                    el.__splSwipeDX = dx;
                    el.style.setProperty('transform','translate(calc(-50% + ' + Math.round(dx) + 'px),0)','important');
                    el.style.setProperty('opacity', String(Math.max(0.35, 1 - Math.abs(dx) / 420)), 'important');
                }, {passive:false});
                el.addEventListener('touchend', function(){
                    if (!dragging){ sx = 0; return; }
                    sx = 0;
                    var w = el.offsetWidth || 220;
                    if (Math.abs(dx) > Math.max(SWIPE_MIN, w * 0.35)) takeover(el);
                    else snapBack();
                });
                el.addEventListener('touchcancel', function(){
                    if (dragging) snapBack();
                    sx = 0;
                });
            }

            function splDress(el){
                if (!el || el.__splDressed || el.__splGhost) return;
                var idx = tracked.length;
                el.__splDressed = true;
                el.__splBorn = Date.now();
                track(el);

                splLiberate(el);

                el.style.setProperty('position','fixed','important');
                el.style.setProperty('left','50%','important');
                el.style.setProperty('top','auto','important');
                el.style.setProperty('right','auto','important');
                el.style.setProperty('z-index','2147483647','important');
                el.style.setProperty('width','max-content','important');
                el.style.setProperty('max-width','calc(100vw - 24px)!important','important');
                el.style.setProperty('margin','0','important');
                el.style.setProperty('pointer-events','auto','important');
                el.style.setProperty('visibility','visible','important');
                el.style.setProperty('animation','splToastIn .38s cubic-bezier(.2,.8,.2,1) both','important');
                armLivePin(el);
                watchForExit(el);
                wireSwipe(el);
                place(el, idx);

                var box = el.querySelector('[data-encore-id="box"]');
                if (box) {
                    box.style.setProperty('background','rgba(24,24,24,.92)','important');
                    box.style.setProperty('backdrop-filter','blur(14px)','important');
                    box.style.setProperty('-webkit-backdrop-filter','blur(14px)','important');
                    box.style.setProperty('color','#fff','important');
                    box.style.setProperty('border','1px solid rgba(255,255,255,.09)','important');
                    box.style.setProperty('border-radius','9999px','important');
                    box.style.setProperty('padding','8px 18px 8px 10px','important');
                    box.style.setProperty('box-shadow','0 8px 32px rgba(0,0,0,.7), 0 0 0 1px rgba(255,255,255,.03) inset','important');
                    box.style.setProperty('display','inline-flex','important');
                    box.style.setProperty('align-items','center','important');
                    box.style.setProperty('gap','10px','important');
                }
            }

            function splScan(node){
                if (!node || node.nodeType !== 1) return;
                if (node.matches && node.matches('div[role="alert"][aria-live]')) splDress(node);
                if (node.querySelectorAll) {
                    var found = node.querySelectorAll('div[role="alert"][aria-live]');
                    for (var i = 0; i < found.length; i++) splDress(found[i]);
                }
            }

            splScan(document.body);

            var splToastObs = new MutationObserver(function(muts){
                for (var i = 0; i < muts.length; i++) {
                    var added = muts[i].addedNodes;
                    for (var j = 0; j < added.length; j++) splScan(added[j]);
                    var rem = muts[i].removedNodes;
                    for (var k = 0; k < rem.length; k++) {
                        if (rem[k].nodeType === 1) extractGhost(rem[k]);
                    }
                }
            });
            splToastObs.observe(document.body, { childList: true, subtree: true });

            function splRestack(){
                var idx = 0;
                for (var i = 0; i < tracked.length; i++){
                    var el = tracked[i];
                    if (el.isConnected && !el.__splGhost) place(el, idx++);
                }
            }

            function checkDeaths(){
                var now = Date.now();
                for (var i = tracked.length - 1; i >= 0; i--){
                    var el = tracked[i];
                    if (!el.isConnected){ splGhost(el); continue; }
                    if (now - (el.__splBorn || 0) < POLL_GRACE) continue;
                    var cs = getComputedStyle(el);
                    var hidden = cs.display === 'none' || el.getBoundingClientRect().height < 2;
                    var faded = parseFloat(cs.opacity) < 0.02;
                    if (!hidden && !faded){
                        var p = el.parentElement, hops = 0;
                        while (p && hops++ < 5){
                            var pcs = getComputedStyle(p);
                            if (pcs.display === 'none'){ hidden = true; break; }
                            if (parseFloat(pcs.opacity) < 0.02){ faded = true; break; }
                            p = p.parentElement;
                        }
                    }
                    if (hidden){
                        el.__splHiddenPolls = (el.__splHiddenPolls || 0) + 1;
                        if (el.__splHiddenPolls >= POLL_DEAD) takeover(el);
                    } else if (faded){
                        el.__splGhost = true;
                        untrack(el);
                        detachWatchers(el);
                    } else {
                        el.__splHiddenPolls = 0;
                    }
                }
            }

            function splTick(){
                if (window.__splBg) return;
                if (tracked.length === 0) return;
                splRestack();
                checkDeaths();
            }
            if (window.__splFloaters) window.__splFloaters.push(splTick);
            else setInterval(splTick, 300);

            window.addEventListener('beforeunload', function(){
                try { splToastObs.disconnect(); } catch (e) {}
            });
        })();
    """
}