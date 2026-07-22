package com.project.lol.webview.injections

object SpotilolPlayer {
    const val CONTENT = """
            window.initSpotilolPlayer=function(){
                if(document.getElementById('spotilolPlayerControls')) return;
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(!npb) return;
                npb.style.display='none';

                var pl=document.createElement('div');
                pl.id='spotilolPlayerControls';
                pl.innerHTML=''
                    +'<div class="spl-top">'
                    +'<div class="spl-cover"><img id="spl-cover-img" src="" alt=""></div>'
                    +'<div class="spl-info"><div class="spl-track" id="spl-track">No track</div>'
                    +'<div class="spl-artist" id="spl-artist">\u2014</div></div>'
                    +'</div>'
                    +'<div class="spl-row2">'
                    +'<div class="spl-actions-left">'
                    +'<button class="spl-btn spl-btn-sm" id="spl-timer" aria-label="Timer"><svg viewBox="0 0 20 20" width="14" height="14"><path fill="currentColor" d="M16.32 7.1A8 8 0 1 1 9 4.06V2h2v2.06c1.46.18 2.8.76 3.9 1.62l1.46-1.46l1.42 1.42l-1.46 1.45zM10 18a6 6 0 1 0 0-12a6 6 0 0 0 0 12zM7 0h6v2H7V0zm5.12 8.46l1.42 1.42L10 13.4L8.59 12l3.53-3.54z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-nptoggle" aria-label="Now Playing"><svg viewBox="0 0 16 17" width="14" height="14"><rect x="1" y="0.75" width="14" height="15.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M 6 5 L 6 5.9160156 L 9.6933594 8.5 L 6 11.080078 L 6 12 L 11 8.5 L 6 5 z" stroke="currentColor" stroke-width="1.2"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-lyrics" aria-label="Lyrics"><svg viewBox="0 0 16 16" width="14" height="14"><path fill="currentColor" d="M13.426 2.574a2.831 2.831 0 0 0-4.797 1.55l3.247 3.247a2.831 2.831 0 0 0 1.55-4.797M10.5 8.118l-2.619-2.62L4.74 9.075 2.065 12.12a1.287 1.287 0 0 0 1.816 1.816l3.06-2.688 3.56-3.129zM7.12 4.094a4.331 4.331 0 1 1 4.786 4.786l-3.974 3.493-3.06 2.689a2.787 2.787 0 0 1-3.933-3.933l2.676-3.045z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-queue" aria-label="Queue"><svg viewBox="0 0 16 16" width="14" height="14"><path fill="currentColor" d="M15 15H1v-1.5h14zm0-4.5H1V9h14zm-14-7A2.5 2.5 0 0 1 3.5 1h9a2.5 2.5 0 0 1 0 5h-9A2.5 2.5 0 0 1 1 3.5m2.5-1a1 1 0 0 0 0 2h9a1 1 0 1 0 0-2z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-vol" aria-label="Volume"><svg viewBox="0 0 16 16" width="14" height="14"><path fill="currentColor" d="M9.741.85a.75.75 0 0 1 .375.65v13a.75.75 0 0 1-1.125.65l-6.925-4a3.64 3.64 0 0 1-1.33-4.967 3.64 3.64 0 0 1 1.33-1.332l6.925-4a.75.75 0 0 1 .75 0zm-6.924 5.3a2.14 2.14 0 0 0 0 3.7l5.8 3.35V2.8zm8.683 4.29V5.56a2.75 2.75 0 0 1 0 4.88"/><path fill="currentColor" d="M11.5 13.614a5.752 5.752 0 0 0 0-11.228v1.55a4.252 4.252 0 0 1 0 8.127z"/></svg></button>'
                    +'</div>'
                    +'<button class="spl-btn spl-btn-sm spl-liked-btn" id="spl-liked" aria-label="Like"><svg viewBox="0 0 16 16" width="14" height="14"><path fill="currentColor" d="M15.724 4.22A4.313 4.313 0 0 0 12.192.814a4.269 4.269 0 0 0-3.622 1.13.837.837 0 0 1-1.14 0 4.272 4.272 0 0 0-6.38 5.69l5.4 6.06a1.09 1.09 0 0 0 1.504.06l5.397-5.892a4.32 4.32 0 0 0 1.253-3.436z"/></svg></button>'
                    +'</div>'
                    +'<div class="spl-bottom">'
                    +'<span class="spl-time" id="spl-pos">0:00</span>'
                    +'<div class="spl-bar-wrap"><div class="spl-bar" id="spl-bar"><div class="spl-fill" id="spl-fill"></div><div class="spl-handle" id="spl-handle"></div></div></div>'
                    +'<span class="spl-time" id="spl-dur">0:00</span>'
                    +'</div>'
                    +'<div class="spl-transport">'
                    +'<button class="spl-btn spl-btn-sm" id="spl-shuffle" aria-label="Shuffle"><svg viewBox="0 0 16 16" width="14" height="14"><path fill="currentColor" d="M13.151.922a.75.75 0 1 0-1.06 1.06L13.109 3H11.16a3.75 3.75 0 0 0-2.873 1.34l-6.173 7.356A2.25 2.25 0 0 1 .39 12.5H0V14h.391a3.75 3.75 0 0 0 2.873-1.34l6.173-7.356a2.25 2.25 0 0 1 1.724-.804h1.947l-1.017 1.018a.75.75 0 0 0 1.06 1.06L15.98 3.75zM.391 3.5H0V2h.391c1.109 0 2.16.49 2.873 1.34L4.89 5.277l-.979 1.167-1.796-2.14A2.25 2.25 0 0 0 .39 3.5zm7.758 6.22l.979-1.167 1.35 1.605a2.25 2.25 0 0 0 1.724.804h1.947l-1.017-1.018a.75.75 0 1 1 1.06-1.06l2.829 2.828-2.829 2.828a.75.75 0 1 1-1.06-1.06L13.109 13H11.16a3.75 3.75 0 0 1-2.873-1.34l-1.138-1.94z"/></svg></button>'
                    +'<button class="spl-btn" id="spl-prev" aria-label="Previous"><svg viewBox="0 0 16 16" width="18" height="18"><path fill="currentColor" d="M3.3 1a.7.7 0 0 1 .7.7v5.15l9.95-5.744a.7.7 0 0 1 1.05.606v12.575a.7.7 0 0 1-1.05.607L4 9.149V14.3a.7.7 0 0 1-.7.7H1.7a.7.7 0 0 1-.7-.7V1.7a.7.7 0 0 1 .7-.7z"/></svg></button>'
                    +'<button class="spl-btn spl-play" id="spl-play" aria-label="Play"><svg viewBox="0 0 16 16" width="22" height="22"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg></button>'
                    +'<button class="spl-btn" id="spl-next" aria-label="Next"><svg viewBox="0 0 16 16" width="18" height="18"><path fill="currentColor" d="M12.7 1a.7.7 0 0 0-.7.7v5.15L2.05 1.107A.7.7 0 0 0 1 1.712v12.575a.7.7 0 0 0 1.05.607L12 9.149V14.3a.7.7 0 0 0 .7.7h1.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg></button>'
                    +'<button class="spl-btn spl-btn-sm" id="spl-repeat" aria-label="Repeat"><svg viewBox="0 0 16 16" width="14" height="14"><path fill="currentColor" d="M0 4.75A3.75 3.75 0 0 1 3.75 1h8.5A3.75 3.75 0 0 1 16 4.75v5a3.75 3.75 0 0 1-3.75 3.75H9.81l1.018 1.018a.75.75 0 1 1-1.06 1.06L6.939 12.75l2.829-2.828a.75.75 0 1 1 1.06 1.06L9.811 12h2.439a2.25 2.25 0 0 0 2.25-2.25v-5a2.25 2.25 0 0 0-2.25-2.25h-8.5A2.25 2.25 0 0 0 1.5 4.75v5A2.25 2.25 0 0 0 3.75 12H5v1.5H3.75A3.75 3.75 0 0 1 0 9.75z"/></svg></button>'
                    +'</div>';

                document.body.appendChild(pl);

                document.getElementById('spl-prev').onclick=function(){actSkipBack()};
                document.getElementById('spl-next').onclick=function(){actSkipForward()};
                document.getElementById('spl-play').onclick=function(){var pb=document.querySelector('button[data-testid=control-button-playpause]');actPlayPause(pb&&pb.getAttribute('aria-label')==='Play')};
                document.getElementById('spl-shuffle').onclick=function(){var sb=document.querySelector('button[data-testid=control-button-shuffle]');if(sb)sb.click()};
                document.getElementById('spl-repeat').onclick=function(){actRepeat()};
                document.getElementById('spl-lyrics').onclick=function(){if(this.classList.contains('spl-disabled'))return;if(typeof closeNowPlay==='function') closeNowPlay();var lb=document.querySelector('button[data-testid=lyrics-button]');if(lb&&!lb.disabled)lb.click()};
                document.getElementById('spl-queue').onclick=function(){var qb=document.querySelector('button[data-testid=control-button-queue]');if(qb)qb.click()};
                document.getElementById('spl-vol').onclick=function(){var vb=document.querySelector('button[data-testid=volume-bar-toggle-mute-button]');if(vb)vb.click()};
                document.getElementById('spl-nptoggle').onclick=function(){clickNP()};
                document.getElementById('spl-timer').onclick=function(){AndBridge.openTimerDialog()};
                document.getElementById('spl-liked').onclick=function(){actAddToFav()};

                var splTrack=document.getElementById('spl-track');
                var splArtist=document.getElementById('spl-artist');
                splTrack.style.cursor='pointer';
                splArtist.style.cursor='pointer';
                splTrack.onclick=function(){
                    if(typeof closeNowPlay==='function') closeNowPlay();
                    var rl=document.querySelector('a[data-testid=context-item-link]');
                    if(rl){rl.click();}
                };
                splArtist.onclick=function(){
                    if(typeof closeNowPlay==='function') closeNowPlay();
                    var al=document.querySelector('a[data-testid=context-item-info-artist]');
                    if(!al) al=document.querySelector('a[data-testid=context-item-info-show]');
                    if(al){al.click();}
                };

                var barEl=document.getElementById('spl-bar');
                var dragging=false;
                function seekTo(e){var r=barEl.getBoundingClientRect();var pct=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width));var rg=document.querySelector('[data-testid="playback-progressbar"] input[type=range]');var mx=parseInt(rg?rg.getAttribute('max'):0)||1;actSeek(Math.round(pct*mx))}
                barEl.addEventListener('mousedown',function(e){dragging=true;seekTo(e)});
                barEl.addEventListener('touchstart',function(e){dragging=true;seekTo(e.touches[0])},{passive:true});
                document.addEventListener('mousemove',function(e){if(dragging)seekTo(e)});
                document.addEventListener('touchmove',function(e){if(dragging)seekTo(e.touches[0])},{passive:true});
                document.addEventListener('mouseup',function(){dragging=false});
                document.addEventListener('touchend',function(){dragging=false});                    window.splUpdate=function(){
                        var ci=document.getElementById('spl-cover-img');
                        var tk=document.getElementById('spl-track');
                        var ar=document.getElementById('spl-artist');
                        var fl=document.getElementById('spl-fill');
                        var hd=document.getElementById('spl-handle');
                        var ps=document.getElementById('spl-pos');
                        var ds=document.getElementById('spl-dur');
                        var pp=document.getElementById('spl-play');
                        var sh=document.getElementById('spl-shuffle');
                        var rp=document.getElementById('spl-repeat');
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
                        if(pp){
                            var pb=document.querySelector('button[data-testid=control-button-playpause]');
                            var isPlaying=pb&&pb.getAttribute('aria-label')!=='Play';
                            pp.innerHTML=isPlaying
                                ?'<svg viewBox="0 0 16 16" width="22" height="22"><path fill="currentColor" d="M2.7 1a.7.7 0 0 0-.7.7v12.6a.7.7 0 0 0 .7.7h2.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7zm8 0a.7.7 0 0 0-.7.7v12.6a.7.7 0 0 0 .7.7h2.6a.7.7 0 0 0 .7-.7V1.7a.7.7 0 0 0-.7-.7z"/></svg>'
                                :'<svg viewBox="0 0 16 16" width="22" height="22"><path fill="currentColor" d="M3 1.713a.7.7 0 0 1 1.05-.607l10.89 6.288a.7.7 0 0 1 0 1.212L4.05 14.894A.7.7 0 0 1 3 14.288z"/></svg>';
                        }
                        if(sh){
                            var sb=document.querySelector('button[data-testid=control-button-shuffle]');
                            sh.classList.toggle('spl-active',sb&&sb.getAttribute('aria-checked')==='true');
                        }
                        if(rp){
                            var rr=document.querySelector('button[data-testid=control-button-repeat]');
                            rp.classList.toggle('spl-active',rr&&rr.getAttribute('aria-checked')==='true');
                        }
                        if(lk){
                            var fb=document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
                            var liked=fb&&fb.getAttribute('aria-checked')==='true';
                            lk.classList.toggle('spl-active',liked===true);
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
                                if(hd) hd.style.left=pct+'%';
                            }
                        }
                        var posEl=document.querySelector('[data-testid="playback-position"]');
                        var durEl=document.querySelector('[data-testid="playback-duration"]');
                        if(ps&&posEl) ps.textContent=posEl.textContent;
                        if(ds&&durEl) ds.textContent=durEl.textContent;
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
                    requestAnimationFrame(rafUpdate);
            };
            if(document.readyState==='complete') initSpotilolPlayer();
            else window.addEventListener('load',initSpotilolPlayer);
            setInterval(function(){
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(npb&&npb.style.display!=='none') initSpotilolPlayer();
            },3000);
        
    """
}
