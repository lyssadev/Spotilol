package com.project.lol.webview.injections

object PlaybackControls {
    const val CONTENT = """
            window.playFromUri = function(uri) {
                var type = uri.match(/^spotify:([^:]+)/);
                type = type ? type[1] : 'your_library';
                if(type=='user') type='your_library';
                oriFetch('https://gew4-spclient.spotify.com/connect-state/v1/player/command/from/'+window.spotDevId+'/to/'+window.spotDevId, {
                    method:'POST',
                    headers:{'Authorization':window.spotAuthToken,'Client-Token':window.spotCliToken,'Content-Type':'application/json'},
                    body:JSON.stringify({
                        command:{
                            context:{uri:uri,url:'context://'+uri,metadata:{}},
                            play_origin:{feature_identifier:type,feature_version:featVer,referrer_identifier:'your_library'},
                            options:{license:'tft',skip_to:{},player_options_override:{}},
                            endpoint:'play'
                        }
                    })
                });
            };
            window.actPlayPause = function(play) {
                var pb = window.pBtn;
                if (!pb) return;
                if (play === null || typeof play === 'undefined') {
                    pb.click();
                } else if (play === true) {
                    if (!window.playing) pb.click();
                } else if (play === false) {
                    if (window.playing) pb.click();
                }
            };
            window.actSkipBack = function() {
                var bb = document.querySelector('button[data-testid=control-button-skip-back]');
                if(bb) { AndBridge.wakeUp(); bb.click(); }
            };
            window.actSkipForward = function() {
                var fb = document.querySelector('button[data-testid=control-button-skip-forward]');
                if(fb) { AndBridge.wakeUp(); fb.click(); }
            };
            window.actRepeat = function() {
                var rb = document.querySelector('button[data-testid=control-button-repeat]');
                if(rb) {
                    if(repmode=='false') repmode='true';
                    else if(repmode=='true') repmode='mixed';
                    else repmode='false';
                    updMedia();
                    rb.click();
                }
            };
            window.actAddToFav = function() {
                var fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
                if(fb) {
                    if(fb.getAttribute('aria-checked')==='false') {
                        fb.click();
                        isfav=true;
                        updMedia();
                    } else {
                        AndBridge.wakeUp();
                        fb.click();
                        var rfint = setInterval(function(){
                            var fr = document.querySelector('#context-menu button[role=menuitemcheckbox][aria-checked=true]');
                            if(fr) {
                                clearInterval(rfint);
                                fr.click();
                                setTimeout(function(){
                                    var sb = document.querySelector('#context-menu button[type=submit]');
                                    if(sb) { sb.click(); isfav=false; updMedia(); }
                                    AndBridge.wakeOff();
                                },500);
                            }
                        },1000);
                    }
                }
            };
            window.actSeek = function(pos) {
                var rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]');
                if(rg) { rg.value=pos+1; rg.dispatchEvent(new Event('change',{bubbles:true})); }
            };
        
    """
}
