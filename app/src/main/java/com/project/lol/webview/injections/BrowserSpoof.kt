package com.project.lol.webview.injections

object BrowserSpoof {
    const val CONTENT = """
            (function(){
                try {
                    window.screen.__defineGetter__('width', function(){ return 1920; });
                    window.screen.__defineGetter__('height', function(){ return 1080; });
                    window.screen.__defineGetter__('availWidth', function(){ return 1920; });
                    window.screen.__defineGetter__('availHeight', function(){ return 1040; });
                    window.__defineGetter__('innerWidth', function(){ return 1920; });
                    window.__defineGetter__('innerHeight', function(){ return 978; });
                } catch(e){}
                function safeDefine(obj, name, getter){
                    try { Object.defineProperty(obj, name, { get: getter, configurable: true }); } catch(e){}
                }
                safeDefine(navigator, 'webdriver', function(){ return false; });
                safeDefine(navigator, 'vendor', function(){ return 'Google Inc.'; });
                safeDefine(navigator, 'productSub', function(){ return '20030107'; });
                safeDefine(navigator, 'platform', function(){ return 'Win32'; });
                safeDefine(navigator, 'oscpu', function(){ return 'Windows NT 10.0; Win64; x64'; });
                safeDefine(navigator, 'languages', function(){ return ['en-US','en']; });
                safeDefine(navigator, 'language', function(){ return 'en-US'; });
                safeDefine(navigator, 'plugins', function(){
                    var p = [
                        { name:'Chrome PDF Plugin', filename:'internal-pdf-viewer', description:'Portable Document Format' },
                        { name:'Chrome PDF Viewer', filename:'mhjfbmdgcfjbbpaeojofohoefgiehjai', description:'' },
                        { name:'Native Client', filename:'internal-nacl-plugin', description:'' }
                    ];
                    p.length = 3;
                    return p;
                });
                safeDefine(navigator, 'mimeTypes', function(){
                    var m = [
                        { type:'application/pdf', suffixes:'pdf', description:'Portable Document Format' },
                        { type:'application/x-google-chrome-pdf', suffixes:'pdf', description:'Portable Document Format' }
                    ];
                    m.length = 2;
                    return m;
                });
            })();
        
    """
}
