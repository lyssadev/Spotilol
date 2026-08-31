package com.project.lol.webview.injections
/*
 * CREDIT: Spotilol - Custom Player.
 *
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣾⠙⠻⢶⣄⡀⠀⠀⠀⢀⣤⠶⠛⠛⡇⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣇⠀⠀⣙⣿⣦⣤⣴⣿⣁⠀⠀⣸⠇⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⣡⣾⣿⣿⣿⣿⣿⣿⣿⣷⣌⠋⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣴⣿⣷⣄⡈⢻⣿⡟⢁⣠⣾⣿⣦⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢹⣿⣿⣿⣿⠘⣿⠃⣿⣿⣿⣿⡏⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⠀⠈⠛⣰⠿⣆⠛⠁⠀⡀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣼⣿⣦⠀⠘⠛⠋⠀⣴⣿⠁⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣤⣶⣾⣿⣿⣿⣿⡇⠀⠀⠀⢸⣿⣏⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⣠⣶⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⠀⠀⠀⠾⢿⣿⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⣠⣿⣿⣿⣿⣿⣿⡿⠟⠋⣁⣠⣤⣤⡶⠶⠶⣤⣄⠈⠀⠀⠀⠀⠀⠀
⠀⠀⠀⢰⣿⣿⣮⣉⣉⣉⣤⣴⣶⣿⣿⣋⡥⠄⠀⠀⠀⠀⠉⢻⣄⠀⠀⠀⠀⠀
⠀⠀⠀⠸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣟⣋⣁⣤⣀⣀⣤⣤⣤⣤⣄⣿⡄⠀⠀⠀⠀
⠀⠀⠀⠀⠙⠿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠛⠋⠉⠁⠀⠀⠀⠀⠈⠛⠃⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠉⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
 */


object SpotilolPlayer {
    const val CONTENT = """
            window.initSpotilolPlayer=function(){
                if(document.getElementById('spotilolPlayerControls')) return;
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(!npb) return;
                npb.style.display='none';

                function splFindShuffle(){
                    var b=document.querySelector('button[data-testid="control-button-shuffle"]');
                    if(b) return b;
                    var all=document.querySelectorAll('button');
                    for(var i=0;i<all.length;i++){
                        var al=all[i].getAttribute('aria-label')||'';
                        if(/shuffle/i.test(al)&&!/spl-btn/.test(all[i].className||'')) return all[i];
                    }
                    return null;
                }
                function splFindRepeat(){
                    var b=document.querySelector('button[data-testid="control-button-repeat"]');
                    if(b) return b;
                    var all=document.querySelectorAll('button');
                    for(var i=0;i<all.length;i++){
                        var al=all[i].getAttribute('aria-label')||'';
                        if(/repeat/i.test(al)&&!/spl-btn/.test(all[i].className||'')) return all[i];
                    }
                    return null;
                }
                function splShuffleState(){
                    var b=splFindShuffle();
                    if(!b) return 'off';
                    if(b.getAttribute('aria-disabled')==='true') return 'disabled';
                    if(b.getAttribute('aria-checked')==='true') return 'shuffle';
                    var al=b.getAttribute('aria-label')||'';
                    if(/smart shuffle/i.test(al)) return /^disable/i.test(al)?'smart':'shuffle';
                    return /^disable/i.test(al)?'shuffle':'off';
                }

                var pl=document.createElement('div');
                pl.id='spotilolPlayerControls';
                pl.innerHTML=''
                    +'<div class="spl-top">'
                    +'<div class="spl-cover"><img id="spl-cover-img" src="" alt=""></div>'
                    +'<div class="spl-info"><div class="spl-track" id="spl-track">No track</div>'
                    +'<div class="spl-artist" id="spl-artist">\u2014</div></div>'
                    +'<div class="spl-mini-transport">'
                    +'<button class="spl-btn" id="spl-prev-mini" aria-label="Previous"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 9.149V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7z"/></svg></button>'
                    +'<button class="spl-btn spl-play" id="spl-play-mini" aria-label="Play"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg></button>'
                    +'<button class="spl-btn" id="spl-next-mini" aria-label="Next"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M12.7 1a.7.7 0 0 0-.7.7v5.15L2.05 1.107A.7.7 0 0 0 1 1.712v12.575a.7.7 0 0 0 1.05.607L12 9.149V14.3a.7.7 0 0 0 .7.7h1.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg></button>'
                    +'</div>'
                    +'</div>'
                    +'<div class="spl-row2">'
                    +'<div class="spl-actions-left">'
                    +'<button class="spl-btn spl-btn-sm" id="spl-timer" aria-label="Timer"><svg viewBox="0 0 20 20"><path fill="currentColor" d="M16.32 7.1A8 8 0 1 1 9 4.06V2h2v2.06c1.46.18 2.8.76 3.9 1.62l1.46-1.46l1.42 1.42l-1.46 1.45zM10 18a6 6 0 1 0 0-12a6 6 0 0 0 0 12zM7 0h6v2H7V0zm5.12 8.46l1.42 1.42L10 13.4L8.59 12l3.53-3.54z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-pip" aria-label="Picture in Picture"><svg viewBox="0 0 24 24"><path fill="currentColor" d="M19 11h-8v6h8v-6zm4 8V4.98C23 3.88 22.1 3 21 3H3c-1.1 0-2 .88-2 1.98V19c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2zm-2 .02H3V4.97h18v14.05z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-nptoggle" aria-label="Now Playing"><svg viewBox="0 0 16 17"><rect x="1" y="0.75" width="14" height="15.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M 6 5 L 6 5.9160156 L 9.6933594 8.5 L 6 11.080078 L 6 12 L 11 8.5 L 6 5 z" stroke="currentColor" stroke-width="1.2"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-lyrics" aria-label="Lyrics"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M13.426 2.574a2.831 2.831 0 0 0-4.797 1.55l3.247 3.247a2.831 2.831 0 0 0 1.55-4.797M10.5 8.118l-2.619-2.62L4.74 9.075 2.065 12.12a1.287 1.287 0 0 0 1.816 1.816l3.06-2.688 3.56-3.129zM7.12 4.094a4.331 4.331 0 1 1 4.786 4.786l-3.974 3.493-3.06 2.689a2.787 2.787 0 0 1-3.933-3.933l2.676-3.045z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-queue" aria-label="Queue"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M15 15H1v-1.5h14zm0-4.5H1V9h14zm-14-7A2.5 2.5 0 0 1 3.5 1h9a2.5 2.5 0 0 1 0 5h-9A2.5 2.5 0 0 1 1 3.5m2.5-1a1 1 0 0 0 0 2h9a1 1 0 1 0 0-2z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-download" aria-label="Download"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M8 1a1 1 0 0 1 1 1v6.586l2.293-2.293a1 1 0 1 1 1.414 1.414l-4 4a1 1 0 0 1-1.414 0l-4-4a1 1 0 1 1 1.414-1.414L7 8.586V2a1 1 0 0 1 1-1zM2 13a1 1 0 0 1 1-1h10a1 1 0 1 1 0 2H3a1 1 0 0 1-1-1z"/></svg></button>'
                    +'<div class="spl-vol-wrap" id="spl-vol">'
                    +'<button class="spl-btn spl-btn-sm spl-vol-btn" id="spl-vol-btn" aria-label="Volume"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M9.741.85a.75.75 0 0 1 .375.65v13a.75.75 0 0 1-1.125.65l-6.925-4a3.64 3.64 0 0 1-1.33-4.967 3.64 3.64 0 0 1 1.33-1.332l6.925-4a.75.75 0 0 1 .75 0zm-6.924 5.3a2.14 2.14 0 0 0 0 3.7l5.8 3.35V2.8zm8.683 4.29V5.56a2.75 2.75 0 0 1 0 4.88"/><path fill="currentColor" d="M11.5 13.614a5.752 5.752 0 0 0 0-11.228v1.55a4.252 4.252 0 0 1 0 8.127z"/></svg></button>'
                    +'<div class="spl-vol-bar" id="spl-vol-bar"><div class="spl-vol-track"></div><div class="spl-vol-fill" id="spl-vol-fill"></div><div class="spl-vol-handle" id="spl-vol-handle"></div></div>'
                    +'</div>'
                    +'</div>'
                    +'<button class="spl-btn spl-btn-sm spl-liked-btn" id="spl-liked" aria-label="Like"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M15.724 4.22A4.313 4.313 0 0 0 12.192.814a4.269 4.269 0 0 0-3.622 1.13.837.837 0 0 1-1.14 0 4.272 4.272 0 0 0-6.38 5.69l5.4 6.06a1.09 1.09 0 0 0 1.504.06l5.397-5.892a4.32 4.32 0 0 0 1.253-3.436z"/></svg></button>'
                    +'</div>'
                    +'<div class="spl-bottom">'
                    +'<span class="spl-time" id="spl-pos">0:00</span>'
                    +'<div class="spl-bar-wrap"><div class="spl-bar" id="spl-bar"><div class="spl-fill" id="spl-fill"></div><div class="spl-handle" id="spl-handle"></div></div></div>'
                    +'<span class="spl-time" id="spl-dur">0:00</span>'
                    +'</div>'
                    +'<div class="spl-edgebar" id="spl-edgebar"><div class="spl-fill" id="spl-fill-edge"></div></div>'
                    +'<div class="spl-transport">'
                    +'<button class="spl-btn spl-btn-sm" id="spl-shuffle" aria-label="Shuffle"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M13.151.922a.75.75 0 1 0-1.06 1.06L13.109 3H11.16a3.75 3.75 0 0 0-2.873 1.34l-6.173 7.356A2.25 2.25 0 0 1 .39 12.5H0V14h.391a3.75 3.75 0 0 0 2.873-1.34l6.173-7.356a2.25 2.25 0 0 1 1.724-.804h1.947l-1.017 1.018a.75.75 0 0 0 1.06 1.06L15.98 3.75zM.391 3.5H0V2h.391c1.109 0 2.16.49 2.873 1.34L4.89 5.277l-.979 1.167-1.796-2.14A2.25 2.25 0 0 0 .39 3.5zm7.758 6.22l.979-1.167 1.35 1.605a2.25 2.25 0 0 0 1.724.804h1.947l-1.017-1.018a.75.75 0 1 1 1.06-1.06l2.829 2.828-2.829 2.828a.75.75 0 1 1-1.06-1.06L13.109 13H11.16a3.75 3.75 0 0 1-2.873-1.34l-1.138-1.94z"/></svg></button>'
                    +'<button class="spl-btn" id="spl-prev" aria-label="Previous"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 9.149V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7z"/></svg></button>'
                    +'<button class="spl-btn spl-play" id="spl-play" aria-label="Play"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg></button>'
                    +'<button class="spl-btn" id="spl-next" aria-label="Next"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M12.7 1a.7.7 0 0 0-.7.7v5.15L2.05 1.107A.7.7 0 0 0 1 1.712v12.575a.7.7 0 0 0 1.05.607L12 9.149V14.3a.7.7 0 0 0 .7.7h1.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-repeat" aria-label="Repeat"><svg viewBox="0 0 16 16"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z"/></svg></button>'
                    +'</div>';

                document.body.appendChild(pl);
                if(window.__splHideEmpty) pl.classList.add('spl-empty');
                    document.body.appendChild(pl);
                    
                    window.splApplyEmpty=function(){
                        var t=document.getElementById('spl-track');
                        var empty=!t||!t.textContent||t.textContent==='No track';
                        if(window.__splHideEmpty&&empty) pl.classList.add('spl-empty');
                        else pl.classList.remove('spl-empty');
                    };

                if(!document.getElementById('spl-vol-css')){
                    var sst=document.createElement('style');sst.id='spl-vol-css';
                    sst.textContent='#spotilolPlayerControls .spl-vol-wrap{display:flex;align-items:center;gap:2px;margin-right:2px}#spotilolPlayerControls .spl-vol-btn{flex-shrink:0}#spotilolPlayerControls .spl-vol-bar{position:relative;width:70px;height:38px;display:flex;align-items:center;cursor:pointer;flex-shrink:0;margin:0 2px}#spotilolPlayerControls .spl-vol-track{position:absolute;left:0;right:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:rgba(255,255,255,.14)}#spotilolPlayerControls .spl-vol-fill{position:absolute;left:0;top:50%;transform:translateY(-50%);height:4px;border-radius:2px;background:var(--spl-accent,#1db954);width:0%}#spotilolPlayerControls .spl-vol-handle{position:absolute;top:50%;left:0%;width:12px;height:12px;transform:translate(-50%,-50%);border-radius:50%;background:#fff;opacity:0;transition:opacity .15s;box-shadow:0 1px 4px rgba(0,0,0,.5);pointer-events:none}#spotilolPlayerControls .spl-vol-bar:hover .spl-vol-handle,#spotilolPlayerControls .spl-vol-bar:active .spl-vol-handle{opacity:1}#spotilolPlayerControls.spl-empty{opacity:0!important;pointer-events:none!important;transform:translateY(24px)!important}';
                    var t=document.head||document.documentElement;if(t)t.appendChild(sst);
                }

                document.getElementById('spl-prev').onclick=function(){actSkipBack()};
                document.getElementById('spl-next').onclick=function(){actSkipForward()};
                document.getElementById('spl-play').onclick=function(){var pb=document.querySelector('button[data-testid=control-button-playpause]');actPlayPause(pb&&pb.getAttribute('aria-label')==='Play')};
                document.getElementById('spl-prev-mini').onclick=function(){actSkipBack()};
                document.getElementById('spl-next-mini').onclick=function(){actSkipForward()};
                document.getElementById('spl-play-mini').onclick=function(){var pb=document.querySelector('button[data-testid=control-button-playpause]');actPlayPause(pb&&pb.getAttribute('aria-label')==='Play')};
                document.getElementById('spl-shuffle').onclick=function(){var sb=splFindShuffle();if(sb&&sb.getAttribute('aria-disabled')!=='true')sb.click()};
                document.getElementById('spl-repeat').onclick=function(){actRepeat()};
                document.getElementById('spl-lyrics').onclick=function(){if(this.classList.contains('spl-disabled'))return;if(typeof closeNowPlay==='function') closeNowPlay();var lb=document.querySelector('button[data-testid=lyrics-button]');if(lb&&!lb.disabled)lb.click()};
                document.getElementById('spl-queue').onclick=function(){var qb=document.querySelector('button[data-testid=control-button-queue]');if(qb)qb.click()};
                document.getElementById('spl-vol-btn').onclick=function(){var vb=document.querySelector('button[data-testid=volume-bar-toggle-mute-button]');if(vb)vb.click()};
                (function(){
                    var vBar=document.getElementById('spl-vol-bar');
                    function splVolRange(){var r=document.querySelector('div[data-testid="volume-bar"] input[type="range"]');if(r)return r;return document.querySelector('input[type="range"][data-testid="volume-bar"]');}
                    function splSetVolPct(pct){
                        var rng=splVolRange();
                        if(!rng)return;
                        var max=parseFloat(rng.getAttribute('max'))||1;
                        var val=pct*max;
                        if(pct<=0) val=parseFloat(rng.getAttribute('min'))||0;
                        var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;
                        setter.call(rng,String(val));
                        rng.dispatchEvent(new Event('input',{bubbles:true}));
                        rng.dispatchEvent(new Event('change',{bubbles:true}));
                    }
                    function volTo(e){if(!vBar)return;var r=vBar.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));splSetVolPct(pct);}
                    var vDrag=false;
                    vBar.addEventListener('mousedown',function(e){vDrag=true;volTo(e);});
                    vBar.addEventListener('touchstart',function(e){vDrag=true;volTo(e.touches[0]);},{passive:true});
                    document.addEventListener('mousemove',function(e){if(vDrag)volTo(e);});
                    document.addEventListener('touchmove',function(e){if(vDrag)volTo(e.touches[0]);},{passive:true});
                    document.addEventListener('mouseup',function(){vDrag=false;});
                    document.addEventListener('touchend',function(){vDrag=false;});
                    document.addEventListener('touchcancel',function(){vDrag=false;});
                })();
                document.getElementById('spl-nptoggle').onclick=function(){clickNP()};
                document.getElementById('spl-timer').onclick=function(){AndBridge.openTimerDialog()};
                document.getElementById('spl-pip').onclick=function(){
                    var pv=document.querySelector('.VideoPlayer__container video');
                    if(pv){
                        var w=pv.videoWidth||0,h=pv.videoHeight||0;
                        try{pv.requestFullscreen();}catch(e){}
                        AndBridge.enterPipVideo(w,h);
                    } else {
                        AndBridge.enterPip();
                    }
                };
                document.getElementById('spl-liked').onclick=function(){actAddToFav()};
                document.getElementById('spl-download').onclick=function(){splDoDownload()};

                var splTrack=document.getElementById('spl-track');
                var splArtist=document.getElementById('spl-artist');
                splTrack.style.cursor='pointer';
                splArtist.style.cursor='pointer';
                splTrack.onclick=function(){
                    if(pl.classList.contains('spl-mini'))return;
                    if(typeof closeNowPlay==='function') closeNowPlay();
                    var rl=document.querySelector('a[data-testid=context-item-link]');
                    if(rl){rl.click();}
                };
                splArtist.onclick=function(){
                    if(pl.classList.contains('spl-mini'))return;
                    if(typeof closeNowPlay==='function') closeNowPlay();
                    var al=document.querySelector('a[data-testid=context-item-info-artist]');
                    if(!al) al=document.querySelector('a[data-testid=context-item-info-show]');
                    if(al){al.click();}
                };

                var barEl=document.getElementById('spl-bar');
                var edgeBarEl=document.getElementById('spl-edgebar');
                var dragging=false,dragEl=barEl;
                function seekTo(el,e){var r=el.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));var rg=document.querySelector('[data-testid="playback-progressbar"] input[type=range]');var mx=parseInt(rg?rg.getAttribute('max'):0)||1;actSeek(Math.round(pct*mx))}
                function bindSeek(el){el.addEventListener('mousedown',function(e){dragEl=el;dragging=true;seekTo(el,e)});el.addEventListener('touchstart',function(e){dragEl=el;dragging=true;seekTo(el,e.touches[0])},{passive:true});}
                bindSeek(barEl);bindSeek(edgeBarEl);
                document.addEventListener('mousemove',function(e){if(dragging)seekTo(dragEl,e)});
                document.addEventListener('touchmove',function(e){if(dragging)seekTo(dragEl,e.touches[0])},{passive:true});
                document.addEventListener('mouseup',function(){dragging=false});
                document.addEventListener('touchend',function(){dragging=false});

                var splMini=false;
                var splDrag=null,splSuppressClick=false,splLastDragEnd=0;
                function splSetMini(m){
                    splMini=!!m;
                    window.splMiniPref=splMini;
                    pl.classList.toggle('spl-mini',splMini);
                }
                function splDragStart(x,y){
                    splDrag={sx:x,sy:y,moving:false,dy:0,mini:splMini};
                    pl.style.transition='none';
                }
                function splDragMove(x,y){
                    if(!splDrag)return;
                    var dy=y-splDrag.sy,dx=x-splDrag.sx;
                    if(!splDrag.moving){
                        if(Math.abs(dy)<10||Math.abs(dy)<Math.abs(dx))return;
                        splDrag.moving=true;
                    }
                    splDrag.dy=splDrag.mini?Math.min(0,dy):Math.max(0,dy);
                    pl.style.transform='translateY('+splDrag.dy+'px)';
                    pl.style.opacity=String(Math.max(.7,1-Math.abs(splDrag.dy)/500));
                }
                function splDragEnd(){
                    if(!splDrag)return;
                    var d=splDrag;
                    splDrag=null;
                    pl.style.transition='';
                    pl.style.transform='';
                    pl.style.opacity='';
                    if(d.moving){
                        splSuppressClick=true;
                        splLastDragEnd=Date.now();
                        setTimeout(function(){splSuppressClick=false;},100);
                        if(d.mini){if(d.dy<-70)splSetMini(false);}
                        else{if(d.dy>70)splSetMini(true);}
                    }
                }
                pl.addEventListener('touchstart',function(e){if(e.target.closest('#spl-bar')||e.target.closest('#spl-edgebar')||e.target.closest('.spl-vol-bar'))return;var t=e.touches[0];splDragStart(t.clientX,t.clientY);},{passive:true});
                pl.addEventListener('touchmove',function(e){if(splDrag&&splDrag.moving)e.preventDefault();if(!splDrag)return;var t=e.touches[0];splDragMove(t.clientX,t.clientY);},{passive:false});
                pl.addEventListener('touchend',function(e){if(splDrag&&splDrag.moving)e.preventDefault();splDragEnd();});
                pl.addEventListener('touchcancel',function(){splDragEnd();});
                pl.addEventListener('mousedown',function(e){if(e.button!==0)return;if(e.target.closest('#spl-bar')||e.target.closest('#spl-edgebar')||e.target.closest('.spl-vol-bar')||e.target.closest('button'))return;splDragStart(e.clientX,e.clientY);});
                document.addEventListener('mousemove',function(e){splDragMove(e.clientX,e.clientY);});
                document.addEventListener('mouseup',function(){splDragEnd();});
                pl.addEventListener('click',function(e){if(splSuppressClick)return;if(Date.now()-splLastDragEnd<400)return;if(splMini&&!e.target.closest('button')&&!e.target.closest('#spl-bar')&&!e.target.closest('#spl-edgebar'))splSetMini(false);});                    window.splUpdate=function(){
                        var ci=document.getElementById('spl-cover-img');
                        var tk=document.getElementById('spl-track');
                        var ar=document.getElementById('spl-artist');
                        var fl=document.getElementById('spl-fill');
                        var fe=document.getElementById('spl-fill-edge');
                        var hd=document.getElementById('spl-handle');
                        var ps=document.getElementById('spl-pos');
                        var ds=document.getElementById('spl-dur');
                        var pp=document.getElementById('spl-play');
                        var ppm=document.getElementById('spl-play-mini');
                        var sh=document.getElementById('spl-shuffle');
                        var rp=document.getElementById('spl-repeat');
                        var vl=document.getElementById('spl-vol');
                        var lk=document.getElementById('spl-liked');
                        var ly=document.getElementById('spl-lyrics');
                        var tm=document.getElementById('spl-timer');

                        var npb=document.querySelector('[data-testid="now-playing-widget"]');
                        var imgEl=npb?npb.querySelector('img[data-testid="cover-art-image"]'):null;
                        if(ci&&imgEl&&imgEl.src&&ci.src!==imgEl.src) ci.src=imgEl.src;

                        var trackEl=document.querySelector('a[data-testid=context-item-link]');
                        if(tk&&trackEl&&trackEl.textContent&&tk.textContent!==trackEl.textContent) tk.textContent=trackEl.textContent;

                        var artistEl=document.querySelector('a[data-testid=context-item-info-artist]');
                        if(!artistEl) artistEl=document.querySelector('a[data-testid=context-item-info-show]');
                        if(ar&&artistEl&&tk.textContent!=='No track') ar.textContent=artistEl.textContent||'';

                        var rg=document.querySelector('[data-testid="playback-progressbar"] input[type=range]');
                        if(pp||ppm){
                            var pb=document.querySelector('button[data-testid=control-button-playpause]');
                            var isPlaying=pb&&pb.getAttribute('aria-label')!=='Play';
                            var ph=isPlaying
                                ?'<svg viewBox="0 0 16 16"><path fill="currentColor" d="M2.7 1a.7.7 0 0 0-.7.7v12.6a.7.7 0 0 0 .7.7h2.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7zm8 0a.7.7 0 0 0-.7.7v12.6a.7.7 0 0 0 .7.7h2.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg>'
                                :'<svg viewBox="0 0 16 16"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg>';
                            if(pp)pp.innerHTML=ph;
                            if(ppm)ppm.innerHTML=ph;
                        }
                        if(sh){
                            var sst=splShuffleState();
                            sh.classList.toggle('spl-active',sst==='shuffle'||sst==='smart');
                            sh.classList.toggle('spl-disabled',sst==='disabled');
                            var isSmart=sst==='smart';
                            var hasSparkle=!!sh.querySelector('.spl-sparkle');
                            if(isSmart&&!hasSparkle){
                                sh.innerHTML='<svg class="spl-sparkle" viewBox="0 0 16 16"><path fill="currentColor" d="M4.502 0a.637.637 0 0 1 .634.58 4.84 4.84 0 0 0 .81 2.184c.515.739 1.297 1.356 2.487 1.486a.637.637 0 0 1 0 1.267c-1.19.13-1.972.747-2.487 1.487a4.8 4.8 0 0 0-.81 2.185.637.637 0 0 1-1.268 0 4.8 4.8 0 0 0-.81-2.185C2.543 6.265 1.76 5.648.57 5.518a.637.637 0 0 1 0-1.268c1.19-.13 1.972-.747 2.487-1.486a4.84 4.84 0 0 0 .81-2.185A.637.637 0 0 1 4.502 0m4.765 11.878c.056.065.126.15.198.236l.33.397.013.015A3 3 0 0 0 12.1 13.59h1.009l-.444.443a.75.75 0 0 0 1.061 1.06l2.254-2.253-2.254-2.254a.75.75 0 0 0-1.06 1.06l.443.444H12.1a1.5 1.5 0 0 1-1.146-.533l-.004-.005-.333-.4-.288-.343-.031-.035-.02-.021-.037-.037-.974 1.16Z"/><path fill="currentColor" d="M12.69 4.196a.75.75 0 0 1 1.06 0l2.254 2.254-2.254 2.254a.75.75 0 0 1-1.06-1.06l.443-.444h-1.008a1.5 1.5 0 0 0-1.15.536l-4.63 5.517c-.344.411-.982 1.021-1.822 1.021v-1.5c.122 0 .371-.124.674-.485l4.63-5.517A3 3 0 0 1 12.125 5.7h1.008l-.443-.443a.75.75 0 0 1 0-1.061"/></svg>';
                            } else if(!isSmart&&hasSparkle){
                                sh.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M13.151.922a.75.75 0 1 0-1.06 1.06L13.109 3H11.16a3.75 3.75 0 0 0-2.873 1.34l-6.173 7.356A2.25 2.25 0 0 1 .39 12.5H0V14h.391a3.75 3.75 0 0 0 2.873-1.34l6.173-7.356a2.25 2.25 0 0 1 1.724-.804h1.947l-1.017 1.018a.75.75 0 0 0 1.06 1.06L15.98 3.75zM.391 3.5H0V2h.391c1.109 0 2.16.49 2.873 1.34L4.89 5.277l-.979 1.167-1.796-2.14A2.25 2.25 0 0 0 .39 3.5zm7.758 6.22l.979-1.167 1.35 1.605a2.25 2.25 0 0 0 1.724.804h1.947l-1.017-1.018a.75.75 0 1 1 1.06-1.06l2.829 2.828-2.829 2.828a.75.75 0 1 1-1.06-1.06L13.109 13H11.16a3.75 3.75 0 0 1-2.873-1.34l-1.138-1.94z"/></svg>';
                            }
                        }
                        if(rp){
                            var rr=splFindRepeat();
                            var rc=rr?rr.getAttribute('aria-checked'):null;
                            var rDisabled=!!(rr&&(rr.disabled||rr.getAttribute('aria-disabled')==='true'));
                            rp.classList.toggle('spl-active',rc==='true'||rc==='mixed');
                            rp.classList.toggle('spl-disabled',rDisabled);
                            rp.classList.toggle('spl-repeat-track',rc==='mixed');
                            if(rc==='mixed'&&!rp.getAttribute('data-rt')){
                                rp.setAttribute('data-rt','1');
                                rp.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h.75v1.5h-.75A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75zM12.25 2.5a2.25 2.25 0 0 1 2.25 2.25v5A2.25 2.25 0 0 1 12.25 12H9.81l1.018-1.018a.75.75 0 0 0-1.06-1.06L6.939 12.75l2.829 2.828a.75.75 0 1 0 1.06-1.06L9.811 13.5h2.439A3.75 3.75 0 0 0 16 9.75v-5A3.75 3.75 0 0 0 12.25 1h-.75v1.5z"/><path fill="currentColor" d="m8 1.85.77.694H6.095V1.488q1.046-.077 1.507-.385.474-.308.583-.913h1.32V8H8z"/><path fill="currentColor" d="M8.77 2.544 8 1.85v.693z"/></svg>';
                            } else if(rc!=='mixed'&&rp.getAttribute('data-rt')){
                                rp.removeAttribute('data-rt');
                                rp.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z"/></svg>';
                            }
                        }
                        if(lk){
                            var fb=document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
                            var liked=fb&&fb.getAttribute('aria-checked')==='true';
                            lk.classList.toggle('spl-active',liked===true);
                        }
                        if(vl){
                            var vbb=document.getElementById('spl-vol-btn');
                            var vf=document.getElementById('spl-vol-fill');
                            var vh=document.getElementById('spl-vol-handle');
                            var vrb=document.querySelector('button[data-testid=volume-bar-toggle-mute-button]');
                            var vrg=document.querySelector('div[data-testid="volume-bar"] input[type="range"]')||document.querySelector('input[type="range"][data-testid="volume-bar"]');
                            var vpct=0;
                            if(vrg){vpct=parseFloat(vrg.value||'0')/(parseFloat(vrg.getAttribute('max'))||1);}
                            var muted=(vrb&&vrb.getAttribute('aria-label')==='Unmute')||vpct<=0;
                            vl.classList.toggle('spl-active',muted===true);
                            var hasX=vbb&&!!vbb.querySelector('.spl-mute-x');
                            if(muted&&!hasX){
                                vbb.innerHTML='<svg viewBox="0 0 16 16"><path class="spl-mute-x" fill="currentColor" d="M13.86 5.47a.75.75 0 0 0-1.061 0l-1.47 1.47-1.47-1.47A.75.75 0 0 0 8.8 6.53L10.269 8l-1.47 1.47a.75.75 0 1 0 1.06 1.06l1.47-1.47 1.47 1.47a.75.75 0 0 0 1.06-1.06L12.39 8l1.47-1.47a.75.75 0 0 0 0-1.06"/><path fill="currentColor" d="M10.116 1.5A.75.75 0 0 0 8.991.85l-6.925 4a3.64 3.64 0 0 0-1.33 4.967 3.64 3.64 0 0 0 1.33 1.332l6.925 4a.75.75 0 0 0 1.125-.649v-1.906a4.7 4.7 0 0 1-1.5-.694v1.3L2.817 9.852a2.14 2.14 0 0 1-.781-2.92c.187-.324.456-.594.78-.782l5.8-3.35v1.3c.45-.313.956-.55 1.5-.694z"/></svg>';
                            } else if(!muted&&hasX){
                                vbb.innerHTML='<svg viewBox="0 0 16 16"><path fill="currentColor" d="M9.741.85a.75.75 0 0 1 .375.65v13a.75.75 0 0 1-1.125.65l-6.925-4a3.64 3.64 0 0 1-1.33-4.967 3.64 3.64 0 0 1 1.33-1.332l6.925-4a.75.75 0 0 1 .75 0zm-6.924 5.3a2.14 2.14 0 0 0 0 3.7l5.8 3.35V2.8zm8.683 4.29V5.56a2.75 2.75 0 0 1 0 4.88"/><path fill="currentColor" d="M11.5 13.614a5.752 5.752 0 0 0 0-11.228v1.55a4.252 4.252 0 0 1 0 8.127z"/></svg>';
                            }
                            if(vf) vf.style.width=(Math.max(0,Math.min(1,vpct))*100)+'%';
                            if(vh) vh.style.left=(Math.max(0,Math.min(1,vpct))*100)+'%';
                        }
                        var lb=document.querySelector('button[data-testid=lyrics-button]');
                        if(lb){
                            ly.style.display='';
                            ly.classList.toggle('spl-disabled',lb.disabled||lb.getAttribute('aria-disabled')==='true');
                        } else {
                            ly.style.display='none';
                        }
                        if(tm) tm.classList.toggle('spl-active',typeof sleepTimerActive!=='undefined'&&sleepTimerActive&&sleepTimerActive.value);

                        var pbEl=document.querySelector('[data-testid="playback-progressbar"] [data-testid="progress-bar"]');
                        if(pbEl){
                            var cs=getComputedStyle(pbEl);
                            var tr=cs.getPropertyValue('--progress-bar-transform');
                            if(tr){
                                var pct=parseFloat(tr)||0;
                                if(fl) fl.style.transform='scaleX('+(pct/100)+')';
                                if(fe) fe.style.transform='scaleX('+(pct/100)+')';
                                if(hd) hd.style.left=pct+'%';
                            }
                        }
                        var posEl=document.querySelector('[data-testid="playback-position"]');
                        var durEl=document.querySelector('[data-testid="playback-duration"]');
                        if(ps&&posEl) ps.textContent=posEl.textContent;
                        if(ds&&durEl) ds.textContent=durEl.textContent;
                        splApplyEmpty();
                    };
                    function formatTime(ms){
                        var t=Math.floor(ms/1000);
                        return Math.floor(t/60)+':'+(t%60<10?'0':'')+t%60;
                    }

                    var rafLastTime=0;
                    function rafUpdate(timestamp){
                        if(timestamp-rafLastTime>100){ splUpdate(); rafLastTime=timestamp; }
                        requestAnimationFrame(rafUpdate);
                    }
                    if(window.splMiniPref) splSetMini(true);
                    requestAnimationFrame(rafUpdate);
            };
            if(document.readyState==='complete') initSpotilolPlayer();
            else window.addEventListener('load',initSpotilolPlayer);
            setInterval(function(){
                if(window.__splBg) return;
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(npb&&npb.style.display!=='none') initSpotilolPlayer();
            },3000);
        
    """
}
