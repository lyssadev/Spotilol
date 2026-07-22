package com.project.lol.webview.injections

object LoginDetection {
    const val CONTENT = """
            (function() {
                var l = document.querySelector('button[data-testid=web-player-link]');
                if(l) {
                    AndBridge.loginDetected();
                    l.click();
                }
            })();
        
    """
}
