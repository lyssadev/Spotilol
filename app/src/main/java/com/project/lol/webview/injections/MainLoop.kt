package com.project.lol.webview.injections

object MainLoop {
    const val CONTENT = """
            window.firstFuck = function(){
                if(pfint) clearInterval(pfint);
                pfint = setInterval(function(){
                    if(playing && document.visibilityState=='hidden' && !!document.querySelector('.VideoPlayer__container video')) {
                        AndBridge.wakeUp();
                    } else if(!AndBridge.isWoke() && document.visibilityState=='visible' && !document.querySelector('.VideoPlayer__container video')) {
                        AndBridge.wakeOff();
                    }

                    if(typeof npBtn=='undefined') {
                        var lyBtn = document.querySelector('button[data-testid=lyrics-button]:not(.fuckd)');
                        var queueBtn = document.querySelector('button[data-testid=control-button-queue]:not(.fuckd)');
                        var anchorBtn = lyBtn || queueBtn;
                        if(anchorBtn) {
                            if(anchorBtn === lyBtn) lyBtn.classList.add('fuckd');
                            npBtn = document.createElement('button');
                            npBtn.className = 'npbtn';
                            npBtn.onclick = clickNP;
                            npBtn.innerHTML = '<svg viewBox="0 0 16 17"><rect x="1" y="0.75" width="14" height="15.5" rx="2" fill="none" stroke="currentColor" stroke-width="1.5"/><path d="M 6 5 L 6 5.9160156 L 9.6933594 8.5 L 6 11.080078 L 6 12 L 11 8.5 L 6 5 z" stroke="currentColor" stroke-width="1.2"/></svg>';
                            window.timerBtn = document.createElement('button');
                            timerBtn.className = 'npbtn';
                            timerBtn.onclick = function(){ AndBridge.openTimerDialog(); };
                            timerBtn.innerHTML = '<svg viewBox="0 0 20 20" width="16" height="16"><path fill="currentColor" d="M16.32 7.1A8 8 0 1 1 9 4.06V2h2v2.06c1.46.18 2.8.76 3.9 1.62l1.46-1.46l1.42 1.42l-1.46 1.45zM10 18a6 6 0 1 0 0-12a6 6 0 0 0 0 12zM7 0h6v2H7V0zm5.12 8.46l1.42 1.42L10 13.4L8.59 12l3.53-3.54z"/></svg>';
                            anchorBtn.before(npBtn);
                            npBtn.before(timerBtn);
                            closeNowPlay();
                        }
                    }

                    var pb = document.querySelector('aside button[data-testid=control-button-playpause]:not(.fuckd)');
                    if(pb) {
                        AndBridge.playLoaded();
                        pb.classList.add('fuckd');
                        window.pBtn = pb;

                        pBtn.addEventListener('click', function(){
                            if(pBtn.getAttribute('aria-label')!=='Play') {
                                reqPause=true;
                                ulFlag=false;
                                AndBridge.wakeOff();
                            } else if(!ulFlag) {
                                reqPause=false;
                                AndBridge.wakeUp();
                                ulFlag=true;
                                setTimeout(function(){
                                    if(ulFlag && pBtn.getAttribute('aria-label')==='Play') {
                                        AndBridge.deferMessage('unlock');
                                        actSkipForward();
                                    } else if(ulFlag) { ulFlag=false; }
                                },10000);
                            }
                        });

                        if(!ffDone){
                            ffDone=true;
                            AndBridge.manageTShut(true);
                            AndBridge.manageTSleep(false);
                            addAndAuto();
                            setTimeout(addAutoFeatures, 1667);
                            setTimeout(addCSSJSHack, 3333);
                            setTimeout(function(){ if(window.autoPlayMode!=='disabled' && playing) actPlayPause(true); },10000);
                        }
                    }
                },5000);
            };
            firstFuck();
        
    """
}
