package com.project.lol.webview.injections

object ClassicLoginButton {
    const val CONTENT = """
            (function(){
                var gl = document.querySelector('section>div>div>div>div>a:first-child:not(.fuckd)');
                if(gl) {
                    var cl = document.createElement('a');
                    cl.className = 'fuckd';
                    cl.innerText = 'Email + Password Classic Login';
                    cl.style.cssText = 'display:block;padding:10px;margin:10px 0;color:white;font-weight:bold;text-decoration:none;border:1px solid #ddd;background:#339;border-radius:30px';
                    cl.href = '?allow_password=1';
                    gl.parentNode.insertBefore(cl, gl);
                }
            })();
        
    """
}
