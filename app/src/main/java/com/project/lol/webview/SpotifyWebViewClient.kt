package com.project.lol.webview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class SpotifyWebViewClient(
    private val onLoginRequired: () -> Unit
) : WebViewClient() {

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view == null || url == null) return

        if (url.startsWith("https://www.facebook.com/privacy/consent/gdp/")) {
            onPageFinishedClean(view, FB_GDPR_BYPASS)
            return
        }

        if (url.endsWith("/login")) {
            onPageFinishedClean(view, CLASSIC_LOGIN_BUTTON)
        }

        val loggedIn = view.context.getSharedPreferences("spotilol_prefs", 0)
            .getBoolean("LoggedIn", false)

        if (!loggedIn) {
            onPageFinishedClean(view, LOGIN_DETECTION)
            return
        }

        injectPlayerControl(view)

        view.evaluateJavascript(LOGOUT_CHECK_JS) { result ->
            if (result == "\"out\"") {
                view.context.getSharedPreferences("spotilol_prefs", 0)
                    .edit().putBoolean("LoggedIn", false).apply()
                view.loadUrl("https://accounts.spotify.com/login")
            }
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        view?.evaluateJavascript(BROWSER_SPOOF, null)
        view?.evaluateJavascript(FETCH_OVERRIDE, null)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.w(TAG, "Renderer process gone: crashed=${detail?.didCrash()}")
        view?.let {
            it.stopLoading()
            it.destroy()
        }
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()
        val method = request.method

        if (isAnalyticsDomain(url)) {
            val headers = mapOf("Access-Control-Allow-Origin" to "*")
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", headers,
                ByteArrayInputStream(ByteArray(0)))
        }

        if (isKnownAudioCdn(url)) {
            try {
                val realConn = URL(url).openConnection() as HttpURLConnection
                try {
                    realConn.requestMethod = method
                    realConn.connectTimeout = 5000
                    realConn.readTimeout = 5000

                    request.requestHeaders.forEach { (key, value) ->
                        realConn.setRequestProperty(key, value)
                    }

                    realConn.connect()
                    val contentType = realConn.contentType

                    if (contentType != null
                        && contentType.equals("audio/mpeg", ignoreCase = true)
                        && !isAudioWhitelisted(url)
                    ) {
                        view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
                        val silent = view.context.assets?.open("silent.mp3") ?: return null
                        return WebResourceResponse("audio/mpeg", null, silent)
                    }
                } finally {
                    realConn.disconnect()
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun injectPlayerControl(view: WebView) {
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        val autoPlayMode = prefs.getString("APlayMode", "disabled") ?: "disabled"
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)
        val customCss = prefs.getString("CustomCss", "") ?: ""

        val js = buildString {
            append("window.autoPlayMode='$autoPlayMode';\n")
            append("window.closeNpPref=$closeNowPlay;\n")
            append(BASE_PLAYER_VARS)
            append(MEDIA_UPDATER)
            append(LIBRARY_FETCHER)
            append(LIBRARY_PARSER)
            append(PLAYBACK_CONTROLS)
            append(MAIN_LOOP)
            append(AUTO_FEATURES)
            append(ANDROID_AUTO_TRACKER)
            append(CSS_JS_HACK)
            append(CUSTOM_PLAYER)
        }
        val cleanJs = stripConsoleLogs(js) + "\n" +
                buildAmoledJs(amoledEnabled) + "\n" +
                buildCustomCssJs(customCss)
        view.evaluateJavascript(cleanJs, null)
    }

    private fun onPageFinishedClean(view: WebView, js: String) {
        view.evaluateJavascript(stripConsoleLogs(js), null)
    }

    private fun stripConsoleLogs(code: String): String {
        return CONSOLE_LOG_PATTERN.matcher(code).replaceAll("")
    }

    companion object {
        private const val TAG = "SpotifyWebViewClient"
        private val CONSOLE_LOG_PATTERN = Pattern.compile("console\\.log\\([^)]*\\);?")

        private const val FB_GDPR_BYPASS = """
            (function(){
                var btn = document.querySelector('#facebook div[role=button]');
                if(btn) btn.click();
            })();
        """

        private const val CLASSIC_LOGIN_BUTTON = """
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

        const val LOGOUT_CHECK_JS = """
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

        private const val LOGIN_DETECTION = """
            (function() {
                var l = document.querySelector('button[data-testid=web-player-link]');
                if(l) {
                    AndBridge.loginDetected();
                    l.click();
                }
            })();
        """

        private const val FETCH_OVERRIDE = """
            (function(){
                if(window.oriFetch) return;
                var orig = window.fetch.bind(window);
                window.oriFetch = orig;
                window.fetch = function(input, init) {
                    if(init && init.headers) {
                        var h = init.headers;
                        var auth, cliTok;
                        if(typeof h.get === 'function') {
                            auth = h.get('Authorization') || h.get('authorization');
                            cliTok = h.get('Client-Token') || h.get('client-token');
                        } else {
                            auth = h.Authorization || h.authorization;
                            cliTok = h['Client-Token'] || h['client-token'];
                        }
                        if(auth && typeof auth === 'string') {
                            window.spotAuthToken = auth.indexOf('Bearer ')===-1 ? 'Bearer '+auth.replace(/^Bearer\s+/i,'') : auth;
                        }
                        if(cliTok) {
                            window.spotCliToken = cliTok;
                        }
                    }
                    var url = typeof input==='string' ? input : (input ? input.url : '');
                    if(url && url.indexOf) {
                        var m = url.match(/\/from\/([A-Za-z0-9_-]+)\/to\//);
                        if(m && m[1]) window.spotDevId = m[1];
                        var m2 = url.match(/connect-state\/v1\/player\/(?:command|transfer)\/from\/([A-Za-z0-9_-]+)\/to\/([A-Za-z0-9_-]+)/);
                        if(m2 && m2[2]) window.spotDevId = m2[2];
                        var m3 = url.match(/\/track-playback\/v1\/devices/);
                        if(m3 && init && init.body) {
                            try {
                                var pb = typeof init.body==='string' ? JSON.parse(init.body) : init.body;
                                if(pb && pb.device && pb.device.device_id && pb.device.device_id!==window.spotDevId) {
                                    window.spotDevId = pb.device.device_id;
                                }
                            } catch(e){}
                        }
                        var m4 = url.match(/connect-state\/v1\/player\/command/);
                        if(m4 && init && init.headers) {
                            var body = init.body;
                            if(body && typeof body==='string') {
                                try {
                                    var j = JSON.parse(body);
                                    if(j && j.command && j.command.context && j.command.context.uri) {
                                        var m5 = j.command.context.uri.match(/spotify:track:([A-Za-z0-9]+)/);
                                        if(m5) {
                                            window.lastPlayedTrack = m5[1];
                                        }
                                    }
                                } catch(e){}
                            }
                        }
                    }
                    return orig.call(window, input, init);
                };
            })();
        """

        private const val BROWSER_SPOOF = """
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
                safeDefine(navigator, 'hardwareConcurrency', function(){ return 8; });
                safeDefine(navigator, 'deviceMemory', function(){ return 8; });
                safeDefine(navigator, 'maxTouchPoints', function(){ return 0; });
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
                try {
                    var getParam = WebGLRenderingContext.prototype.getParameter;
                    WebGLRenderingContext.prototype.getParameter = function(p){
                        if(p === 37445) return 'Google Inc. (NVIDIA)';
                        if(p === 37446) return 'ANGLE (NVIDIA, NVIDIA GeForce GTX 1050 Ti Direct3D11 vs_5_0 ps_5_0, D3D11)';
                        return getParam.call(this, p);
                    };
                } catch(e){}
                try {
                    if(navigator.mediaDevices){
                        var origEnumerate = navigator.mediaDevices.enumerateDevices;
                        navigator.mediaDevices.enumerateDevices = function(){
                            return origEnumerate.call(this).then(function(devices){
                                return devices.map(function(d){
                                    var c = Object.assign({}, d);
                                    if(d.kind === 'audioinput') c.label = 'Default - Microphone (Realtek High Definition Audio)';
                                    if(d.kind === 'audiooutput') c.label = 'Default - Speakers (Realtek High Definition Audio)';
                                    return c;
                                });
                            });
                        };
                    }
                } catch(e){}
                try {
                    if(navigator.connection){
                        Object.defineProperty(navigator.connection, 'rtt', { get: function(){ return 50; } });
                    }
                } catch(e){}
            })();
        """

        private const val BASE_PLAYER_VARS = """
            var reqPause=false,firstPlay=true,ulFlag=false,ffDone=false,npOpen=false;
            var featVer='web-player_'+new Date().toISOString().split('T')[0]+'_'+Date.now()+'_'+Math.floor(Math.random()*0xFFFFFFF).toString(16).padStart(7,'0');
            var lastState=null,lastPos=null,playing=false;
            var pfint=null,afint=null,cssint=null,aaint=null;
        """

        private const val MEDIA_UPDATER = """
            window.updMedia = function(){
                var currState=track+'|'+artist+'|'+playing+'|'+repmode+'|'+isfav;
                if(currState!==lastState) {
                    lastState=currState;
                    var values={artist:artist,track:track,playing:playing,repeat:repmode,fav:isfav,duration:duration,position:position,cover:cover};
                    AndBridge.recMediaStatus(JSON.stringify(values));
                } else {
                    AndBridge.recMediaPosition(position);
                    lastPos=position;
                }
            };
        """

        private const val LIBRARY_FETCHER = """
            window.fetchAllLibrary = async function(){
                var limit=50, offset=0, allItems=[], hasMore=true;
                while(hasMore){
                    var resp = await oriFetch('https://api-partner.spotify.com/pathfinder/v2/query',{
                        method:'POST',
                        headers:{
                            'Authorization':window.spotAuthToken,
                            'Client-Token':window.spotCliToken,
                            'Content-Type':'application/json;charset=UTF-8'
                        },
                        body:JSON.stringify({
                            variables:{
                                filters:[],order:null,textFilter:'',
                                features:['LIKED_SONGS','YOUR_EPISODES_V2','PRERELEASES','EVENTS'],
                                limit:limit,offset:offset,flatten:false,expandedFolders:[],
                                folderUri:null,includeFoldersWhenFlattening:true
                            },
                            operationName:'libraryV3',
                            extensions:{persistedQuery:{version:1,sha256Hash:'0082bf82412db50128add72dbdb73e2961d59100b9cbf41fb25c568bd8bc358b'}}
                        })
                    });
                    var data = await resp.json();
                    var items = (data && data.data && data.me && data.me.libraryV3 && data.me.libraryV3.items) || [];
                    allItems = allItems.concat(items);
                    if(items.length < limit) hasMore=false; else offset+=limit;
                }
                return allItems;
            };
        """

        private const val LIBRARY_PARSER = """
            window.parseLibrary = function(items) {
                var res={playlists:[],albums:[],artists:[],podcasts:[]};
                items.forEach(function(entry){
                    var data = entry.item && entry.item.data;
                    if(!data || !data.__typename) return;
                    switch(data.__typename) {
                        case 'PseudoPlaylist':
                        case 'Playlist':
                            res.playlists.push({id:data.uri,name:data.name,image:data.images&&data.images.items&&data.images.items[0]&&data.images.items[0].sources&&data.images.items[0].sources[0]?data.images.items[0].sources[0].url:null});
                            break;
                        case 'Album':
                            res.albums.push({id:data.uri,name:data.name,image:data.coverArt&&data.coverArt.sources?data.coverArt.sources[0].url:null,artists:data.artists&&data.artists.items?data.artists.items.map(function(a){return a.profile&&a.profile.name}).filter(Boolean):[]});
                            break;
                        case 'Artist':
                            res.artists.push({id:data.uri,name:data.profile&&data.profile.name,image:data.visuals&&data.visuals.avatarImage&&data.visuals.avatarImage.sources?data.visuals.avatarImage.sources[0].url:null});
                            break;
                        case 'Podcast':
                            res.podcasts.push({id:data.uri,name:data.name,image:data.coverArt&&data.coverArt.sources?data.coverArt.sources[0].url:null,artists:data.publisher&&data.publisher.name?[data.publisher.name]:[]});
                            break;
                    }
                });
                return res;
            };
        """

        private const val PLAYBACK_CONTROLS = """
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
                if('pBtn' in window) {
                    if(pBtn.getAttribute('aria-label')==='Play') { if(play) pBtn.click(); }
                    else { if(!play) pBtn.click(); }
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

        private const val MAIN_LOOP = """
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
                            addAutoFeatures();
                            addCSSJSHack();
                            addAndAuto();
                            setTimeout(function(){ if(window.autoPlayMode!=='disabled' && playing) actPlayPause(true); },10000);
                        }
                    }
                },5000);
            };
            firstFuck();
        """

        private const val AUTO_FEATURES = """
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

        private const val ANDROID_AUTO_TRACKER = """
            window.addAndAuto = function(){
                if(aaint) clearInterval(aaint);
                aaint = setInterval(function(){
                    var ta = document.querySelector('a[data-testid=context-item-link]');
                    if(ta) track=ta.text; else track=null;
                    var aa = document.querySelector('a[data-testid=context-item-info-artist]');
                    if(!aa) aa = document.querySelector('a[data-testid=context-item-info-show]');
                    if(aa) artist=aa.text; else artist='';
                    var rr = document.querySelector('button[data-testid=control-button-repeat]');
                    if(rr) repmode=rr.getAttribute('aria-checked'); else repmode='false';
                    var fb = document.querySelector('div[data-testid=now-playing-widget]>div:last-child>button');
                    if(fb && fb.getAttribute('aria-checked')==='true') isfav=true; else isfav=false;
                    var pb = document.querySelector('button[data-testid=control-button-playpause]');
                    if(pb) playing=pb.getAttribute('aria-label')!=='Play';
                    var rg = document.querySelector('div[data-testid=playback-progressbar] input[type=range]');
                    if(rg) { duration=parseInt(rg.getAttribute('max')); position=parseInt(rg.getAttribute('value')); }
                    else { duration=null; position=null; }
                    var im = document.querySelector('img[data-testid=cover-art-image]');
                    if(im) {
                        var s=im.src;
                        if(s.indexOf('i.scdn.co')!==-1) {
                            s=s.replace(/ab67616d0000[0-9a-f]{4}/,'ab67616d000082c1');
                            s=s.replace(/ab6761670000[0-9a-f]{4}/,'ab676167000082e8');
                        }
                        cover=s;
                    } else cover=null;
                    updMedia();
                },1000);
            };
        """

        private const val CSS_JS_HACK = """
            window.closeNowPlay=function(){
                var rc=document.querySelector('#Desktop_PanelContainer_Id');
                if(rc&&rc.parentNode.parentNode.ariaHidden=='false'){clickNP();}
            };
            window.clickNP=function(){
                var rBtn=document.querySelector('#Desktop_PanelContainer_Id').parentNode.parentNode.nextElementSibling.querySelector('button');
                if(rBtn){
                    var npHid=document.querySelector('#Desktop_LeftSidebar_Id').parentNode.parentNode.ariaHidden;
                    if(npHid&&npHid=='true') npBtn.classList.add('active'); else npBtn.classList.remove('active');
                    rBtn.click();
                }
            };
            window.switchLs=function(){
                var ls=document.querySelector('#Desktop_LeftSidebar_Id');
                if(ls){
                    var exp=ls.querySelector('nav>div>div:first-child').classList.length;
                    if(exp==2){
                        ls.style.position='fixed';ls.style.width='100%';ls.style.height='92%';ls.style.left=0;ls.style.zIndex=20;
                        var lh=ls.querySelector('header>div>div:first-child h1');
                        if(lh) lh.innerHTML='\u2716 &nbsp; Close Library';
                    } else {
                        ls.style.zIndex=1;ls.style.position='fixed';ls.style.top='0';ls.style.left='60px';ls.style.width='48px';ls.style.height='48px';
                    }
                }
            };
            window.addCSSJSHack=function(){
                if(cssint) clearInterval(cssint);
                cssint=setInterval(function(){
                    var lb=document.querySelector('#Desktop_LeftSidebar_Id header>div>div:first-child button:not(.fuckd)');
                    if(lb){
                        window.lBtn=lb;lb.classList.add('fuckd','lbtn');lb.style.padding=0;lb.style.height='20px';
                        lb.addEventListener('click',function(){setTimeout(function(){switchLs();},0);});
                        switchLs();AndBridge.cssInjected();
                    }
                    var lbit=document.querySelector('#Desktop_LeftSidebar_Id div[role=grid]:not(.fuckd)');
                    if(lbit){lbit.classList.add('fuckd');lbit.addEventListener('click',function(){setTimeout(function(){lBtn.click();closeNowPlay();},0);});}
                    var hb=document.querySelector('#global-nav-bar button[data-testid=home-button]:not(.fuckd)');
                    if(hb){hb.classList.add('fuckd');hb.addEventListener('click',function(){closeNowPlay();});}
                    var sr=document.querySelector('input[data-testid=search-input]:not(.fuckd)');
                    if(sr){
                        sr.classList.add('fuckd');
                        sr.addEventListener('keydown',function(e){if(e.key==='Enter'){closeNowPlay();}});
                    }
                    var sdd=document.getElementById('search-dropdown');
                    if(sdd && !sdd.classList.contains('fuckd')){
                        sdd.classList.add('fuckd');
                        sdd.addEventListener('click',function(e){
                            var t=e.target.closest('a[href*="/track/"]');
                            if(t) closeNowPlay();
                        },true);
                    }
                    var ub=document.querySelector('button[data-testid=user-widget-link]:not(.fuckd)');
                    if(ub){ub.classList.add('fuckd');ub.addEventListener('click',function(){closeNowPlay();});}
                },5000);
            };
            var st=document.createElement('style');
            st.textContent='body{min-width:100%!important;min-height:100%!important} .os-scrollbar{--os-size:6px!important} .contentSpacing{padding:0} div[data-testid=root]{--panel-gap:0!important} #main-view+div,#main-view+div>div{overflow:hidden!important;width:auto} #main-view+div>div>div>div:nth-child(2)>div{width:100vw!important} div[data-encore-id=banner],#global-nav-bar>div:first-of-type,#global-nav-bar a[href="/download"],button[data-testid=fullscreen-mode-button],button[data-testid=friend-activity-button],div.main-view-container__mh-footer-container,li:has(>a[href*="spotify.com/premium"]),li:has(>a[href*="support.spotify.com"]),li:has(>a[href*="spotify.com/download"]){display:none!important} aside[data-testid="now-playing-bar"]{display:none!important} #spotilolPlayerControls{position:fixed;bottom:12px;left:12px;right:12px;z-index:9999;display:flex;flex-direction:column;padding:12px 14px 14px;background:rgba(24,24,24,.92);backdrop-filter:blur(28px);-webkit-backdrop-filter:blur(28px);border:1px solid rgba(255,255,255,.06);border-radius:16px;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;color:#fff;box-shadow:0 8px 32px rgba(0,0,0,.6)} #spotilolPlayerControls .spl-top{display:flex;align-items:center;gap:10px;margin-bottom:8px} #spotilolPlayerControls .spl-cover{flex-shrink:0} #spotilolPlayerControls .spl-cover img{width:48px;height:48px;border-radius:10px;object-fit:cover;background:#282828} #spotilolPlayerControls .spl-info{flex:1;min-width:0;overflow:hidden} #spotilolPlayerControls .spl-track{font-size:14px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.3;transition:color .15s;cursor:pointer} #spotilolPlayerControls .spl-track:hover{color:#1db954} #spotilolPlayerControls .spl-artist{font-size:12px;color:rgba(255,255,255,.55);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.3;transition:color .15s;cursor:pointer} #spotilolPlayerControls .spl-artist:hover{color:#1db954} #spotilolPlayerControls .spl-row2{display:flex;align-items:center;justify-content:space-between;margin-bottom:6px} #spotilolPlayerControls .spl-actions-left{display:flex;align-items:center;gap:2px} #spotilolPlayerControls .spl-liked-btn{color:rgba(255,255,255,.7)!important;position:relative} #spotilolPlayerControls .spl-liked-btn.spl-active{color:#1db954!important} #spotilolPlayerControls .spl-btn{background:none;border:none;color:rgba(255,255,255,.7);cursor:pointer;padding:8px;border-radius:50%;display:flex;align-items:center;justify-content:center;transition:color .15s,background .15s} #spotilolPlayerControls .spl-btn:hover{color:#fff;background:rgba(255,255,255,.1)} #spotilolPlayerControls .spl-btn-sm{padding:6px} #spotilolPlayerControls .spl-active{color:#1db954} #spotilolPlayerControls .spl-bottom{display:flex;align-items:center;gap:8px;width:100%;margin-bottom:6px} #spotilolPlayerControls .spl-time{font-size:10px;color:rgba(255,255,255,.45);min-width:30px;text-align:center;font-variant-numeric:tabular-nums} #spotilolPlayerControls .spl-bar-wrap{flex:1;position:relative;height:14px;display:flex;align-items:center} #spotilolPlayerControls .spl-bar{width:100%;height:4px;background:rgba(255,255,255,.12);border-radius:2px;cursor:pointer;position:relative} #spotilolPlayerControls .spl-fill{height:100%;background:#1db954;border-radius:2px;transition:width .4s linear;width:0%;position:relative} #spotilolPlayerControls .spl-handle{position:absolute;top:50%;width:12px;height:12px;background:#fff;border-radius:50%;transform:translate(-50%,-50%);left:0%;opacity:0;transition:opacity .15s;pointer-events:none;box-shadow:0 1px 4px rgba(0,0,0,.5)} #spotilolPlayerControls .spl-bar-wrap:hover .spl-handle{opacity:1} #spotilolPlayerControls .spl-transport{display:flex;align-items:center;justify-content:center;gap:16px;padding:2px 0} #spotilolPlayerControls .spl-play{background:rgba(255,255,255,.1)!important;color:#fff!important;padding:10px!important} #spotilolPlayerControls .spl-play:hover{background:rgba(255,255,255,.2)!important} @media(max-width:420px){#spotilolPlayerControls{bottom:8px;left:8px;right:8px;padding:8px 10px 10px;border-radius:14px} #spotilolPlayerControls .spl-cover img{width:42px;height:42px;border-radius:8px} #spotilolPlayerControls .spl-track{font-size:13px} #spotilolPlayerControls .spl-artist{font-size:11px} #spotilolPlayerControls .spl-actions-left{gap:0} #spotilolPlayerControls .spl-btn-sm{padding:5px} #spotilolPlayerControls .spl-transport{gap:12px} #spotilolPlayerControls .spl-play{padding:8px!important}} section[data-testid=artist-page]>div>div:first-child:not([data-encore-id]){height:25vh} div[data-testid=tracklist-row]{padding:0 10px 0 0;grid-gap:0} div[data-testid=tracklist-row] button:not([data-testid=add-to-playlist-button]){transform:scale(1.3)!important;opacity:0.6!important} div[data-testid=tracklist-row] button:{-webkit-margin-end:0!important} div[data-testid=tracklist-row] button:hover{color:#2d6!important} div[data-testid=tracklist-row]>div:first-child>div:first-child{height:24px;min-height:24px;min-width:24px;margin:0 8px!important} [aria-colcount="3"] div[data-testid=tracklist-row]{grid-template-columns:[index] var(--tracklist-index-column-width,40px) [first] minmax(120px,var(--col1,4fr)) [last] minmax(82px,var(--col2,1fr))!important} [aria-colcount="4"] div[data-testid=tracklist-row]{grid-template-columns:[index] var(--tracklist-index-column-width,40px) [first] minmax(120px,var(--col1,4fr)) [var1] minmax(120px,var(--col2,2fr)) [last] minmax(82px,var(--col3,1fr))!important} [aria-colcount="5"] div[data-testid=tracklist-row]{grid-template-columns:[index] var(--tracklist-index-column-width,40px) [first] minmax(120px,var(--col1,6fr)) [var1] minmax(120px,var(--col2,4fr)) [var2] minmax(120px,var(--col3,3fr)) [last] minmax(82px,var(--col4,1fr))!important} section[data-testid=track-page]>div.contentSpacing>div:nth-child(2) [aria-colcount="2"] div[data-testid=tracklist-row]{grid-template-columns:[first] minmax(120px,var(--col0,4fr)) [last] minmax(82px,var(--col1,1fr))!important} section[data-testid=track-page]>div.contentSpacing>div:nth-child(2) [aria-colcount="3"] div[data-testid=tracklist-row]{grid-template-columns:[first] minmax(120px,var(--col0,4fr)) [var1] minmax(120px,var(--col1,2fr)) [last] minmax(82px,var(--col2,1fr))!important} .npbtn{cursor:pointer;color:#b3b3b3;background:transparent;border:none;width:32px;height:32px;padding:8px} .npbtn.active{color:#FFFFFF} *{--content-spacing:10px} section[data-testid=home-page] .contentSpacing{padding:0 10px!important;overflow:hidden} [data-shelf-collapsable="true"]{display:none!important} div[data-testid=grid-container]{margin-inline:0!important;column-gap:0!important;overflow:hidden!important} div[data-testid=action-bar-row],div[data-testid=topbar-content]{padding:5px 10px} div[data-testid=track-list]>div:first-child,div[data-testid=playlist-tracklist]>div:first-child{margin:0!important;padding:0!important} main>section:not([data-testid=artist-page])>div:first-child{height:auto!important;min-height:auto!important;padding:10px} section[data-testid=track-page]>div>div.contentSpacing>div:last-child{overflow:hidden} section[data-testid=artist-page]>div>div:first-child>div.contentSpacing{padding:10px} section[data-testid=artist-page] div[data-testid=grid-container] h2,section[data-testid=artist-page] section[data-testid=component-shelf]{padding:0 10px} main>section h1.encore-text-headline-large{font-size:22px!important} section[data-testid=artist-page] span.encore-text-headline-large{font-size:26px!important} section[data-testid=track-page] h1{font-size:20px!important} aside[data-testid=now-playing-bar]{min-width:100%!important;box-shadow:none!important;background:#000000!important} aside[data-testid=now-playing-bar]>div:first-child{margin-top:2px;flex-direction:column!important;height:auto!important} aside[data-testid=now-playing-bar]>div>div{width:100%!important} aside[data-testid=now-playing-bar]>div>div:last-child>div{min-height:32px;margin:5px 10px} aside[data-testid=now-playing-bar]>div>div:last-child button{transform:scale(1.15);margin:0 5px} div[data-testid=general-controls]{margin:15px 0 25px} div[data-testid=general-controls] button{transform:scale(1.4)!important;margin:0 8px!important} div[data-testid=player-controls]{margin:5px 0} div[data-testid=now-playing-widget]{justify-content:center;overflow:hidden} form[role=search]{z-index:10;margin-left:48px;max-width:88%} div[data-testid=now-playing-widget]>div:last-child>button{transform:scale(1.3)} div[data-testid=now-playing-widget]>div:first-child{display:none!important} div[data-testid=now-playing-widget]>div:nth-child(2){display:flex!important;overflow:hidden!important} div[data-testid=now-playing-widget]>div:nth-child(2) span{font-size:13px!important;height:20px!important;margin:0!important} div[data-testid=now-playing-widget]>div:nth-child(2)>div{min-width:auto;max-width:66%} [data-tippy-root]{overflow:hidden!important} [data-tippy-root],[data-tippy-root] *{transition:none!important;transform:none!important} div[data-testid=hover-or-focus-tooltip],#Desktop_LeftSidebar_Id header>div>div:last-child{display:none!important} #Desktop_LeftSidebar_Id>nav>div{min-height:48px;border-radius:25px} .YourLibraryX{overflow:hidden;background:var(--background-elevated-base)!important} .YourLibraryX header{padding:14px} #spotilolPlayerControls .spl-disabled{opacity:.3!important;pointer-events:none}';
            try{var target=document.head||document.documentElement;if(target)target.appendChild(st);else{document.addEventListener('DOMContentLoaded',function(){var t=document.head||document.documentElement;if(t)t.appendChild(st);});}}catch(e){}
        """

        private const val CUSTOM_PLAYER = """
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
                document.addEventListener('touchend',function(){dragging=false});

                window.splUpdate=function(){
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
                            if(fl) fl.style.width=pct+'%';
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

                setInterval(splUpdate,500);
            };
            if(document.readyState==='complete') initSpotilolPlayer();
            else window.addEventListener('load',initSpotilolPlayer);
            setInterval(function(){
                var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                if(npb&&npb.style.display!=='none') initSpotilolPlayer();
            },3000);
        """
        fun buildAmoledJs(enabled: Boolean): String {
            return if (enabled) {
                """
                    (function(){
                        var aled = document.getElementById('spotilol-amoled-theme') || document.createElement('style');
                        aled.id = 'spotilol-amoled-theme';
                        aled.textContent = '.encore-dark-theme{--background-base:#000;--background-highlight:#000;--background-elevated-base:#000;--background-elevated-highlight:#000;--background-elevated-press:#000;--background-tinted-base:#000} aside[data-testid=now-playing-bar]{background:#000!important;box-shadow:none;border-top:1px solid #666}';
                        if (!aled.parentNode) (document.head || document.documentElement).appendChild(aled);
                    })();
                """.trimIndent()
            } else {
                """
                    (function(){
                        var aled = document.getElementById('spotilol-amoled-theme');
                        if (aled) aled.remove();
                    })();
                """.trimIndent()
            }
        }

        private fun isAnalyticsDomain(url: String): Boolean {
            return url.contains("doubleclick.net") ||
                    url.contains("googlesyndication.com") ||
                    url.contains("fastly-insights.com") ||
                    url.contains("sentry.io")
        }

        private fun isAudioWhitelisted(url: String): Boolean {
            return url.contains("podz-content") ||
                    url.contains("gew4-spclient")
        }

        private fun isKnownAudioCdn(url: String): Boolean {
            return url.contains("akamaized.net/audio/") ||
                    url.contains("scdn.co/audio/") ||
                    url.contains("scdn.co/mp3-ad/") ||
                    url.contains("spotifycdn.com/audio/") ||
                    url.contains("amillionads.com") ||
                    url.contains("2mdn.net") ||
                    url.contains("adxcel.com") ||
                    url.contains("adstudio-assets.scdn.co")
        }

        fun buildCustomCssJs(css: String): String {
            val jsonCss = JSONObject.quote(css)
            return """
                (function(){
                    var cst = document.getElementById('spotilol-custom-css');
                    if ($jsonCss === "") {
                        if (cst) cst.remove();
                        return;
                    }
                    if (!cst) {
                        cst = document.createElement('style');
                        cst.id = 'spotilol-custom-css';
                    }
                    cst.textContent = $jsonCss;
                    var target = document.head || document.documentElement;
                    if (target && !cst.parentNode) {
                        target.appendChild(cst);
                    }
                })();
            """.trimIndent()
        }
    }
}
