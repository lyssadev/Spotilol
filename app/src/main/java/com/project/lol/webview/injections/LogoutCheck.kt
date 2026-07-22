package com.project.lol.webview.injections

object LogoutCheck {
    const val CONTENT = """
            (function(){
                var s = document.getElementById('appServerConfig');
                if(!s) return 'skip';
                try {
                    var d = JSON.parse(atob(s.textContent.trim()));
                    return d.isAnonymous ? 'out' : 'in';
                } catch(e) {
                    return 'skip';
                }
            })();
        
    """
}
