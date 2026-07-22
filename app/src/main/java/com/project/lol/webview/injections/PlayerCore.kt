package com.project.lol.webview.injections

object PlayerCore {
    const val CONTENT = """
            var reqPause=false,firstPlay=true,ulFlag=false,ffDone=false,npOpen=false;
            var featVer='web-player_'+new Date().toISOString().split('T')[0]+'_'+Date.now()+'_'+Math.floor(Math.random()*0xFFFFFFF).toString(16).padStart(7,'0');
            var lastState=null,lastPos=null,playing=false;
            var pfint=null,afint=null,cssint=null,aaint=null;
        
    """
}
