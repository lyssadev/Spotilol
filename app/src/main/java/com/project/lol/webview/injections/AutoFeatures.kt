package com.project.lol.webview.injections

object AutoFeatures {
    const val CONTENT = """
            window.addAutoFeatures = function(){
                if('pBtn' in window && firstPlay && window.autoPlayMode!=='disabled' && pBtn.getAttribute('aria-label')==='Play') {
                    pBtn.click();
                    firstPlay=false;
                }
                if(afint) clearInterval(afint);
                afint = setInterval(function(){
                    if(window.closeNpPref) closeNowPlay();
                    var ft = document.querySelector('aside div.encore-bright-accent-set button');
                    if(ft) {
                        ft.click();
                        setTimeout(function(){
                            var cb = document.querySelector('aside ul[role=list] li[role=listitem] div[role=button]');
                            if(cb) cb.click();
                        },500);
                    }
                    if(window.autoPlayMode==='permanent' && 'pBtn' in window && !reqPause && !ulFlag && pBtn.getAttribute('aria-label')==='Play') {
                        pBtn.click();
                    }
                },5000);
            };
        
    """
}
