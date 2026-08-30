package com.project.lol.webview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.project.lol.webview.helpers.*
import com.project.lol.webview.injections.*
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class SpotifyWebViewClient(
    private val onLoginRequired: () -> Unit,
    private val onNavStateChanged: ((Boolean) -> Unit)? = null,
    private val onRenderProcessGone: (() -> Unit)? = null,
    private val onWebViewError: ((errorCode: Int, description: String) -> Unit)? = null
) : WebViewClient() {

    private var currentWebView: WebView? = null
    private var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        onNavStateChanged?.invoke(view?.canGoBack() == true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view == null || url == null) return

        currentWebView = view
        registerPrefsListener(view)

        if (url.startsWith("https://www.facebook.com/privacy/consent/gdp/")) {
            onPageFinishedClean(view, FbGdprBypass.CONTENT)
            return
        }

        if (url.endsWith("/login")) {
            onPageFinishedClean(view, ClassicLoginButton.CONTENT)
        }

        val loggedIn = view.context.getSharedPreferences("spotilol_prefs", 0)
            .getBoolean("LoggedIn", false)

        if (!loggedIn) {
            onPageFinishedClean(view, LoginDetection.CONTENT)
            return
        }

        view.postDelayed({
            injectPlayerControl(view)
        }, 500)

        view.evaluateJavascript(LogoutCheck.CONTENT) { result ->
            if (result == "\"out\"") {
                view.context.getSharedPreferences("spotilol_prefs", 0)
                    .edit().putBoolean("LoggedIn", false).apply()
                view.loadUrl("https://accounts.spotify.com/login")
            }
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val useProxy = view?.context?.getSharedPreferences("spotilol_prefs", 0)
            ?.getString("ConnectionMode", "normal") == "proxy"
        val powerSave = view?.context?.getSharedPreferences("spotilol_prefs", 0)
            ?.getBoolean("PowerSave", false) ?: false
        val blockSW = view?.context?.getSharedPreferences("spotilol_prefs", 0)
            ?.getBoolean("BlockServiceWorker", true) ?: true

        view?.evaluateJavascript("window.__spotilolUseProxy=$useProxy;", null)
        if (isGoogleAuthUrl(url)) {
            view?.evaluateJavascript(GoogleSpoof.CONTENT, null)
        } else {
            view?.evaluateJavascript(BrowserSpoof.CONTENT, null)
        }
        view?.evaluateJavascript(FetchOverride.CONTENT, null)
        AdIdStore.clear()
        view?.evaluateJavascript(AdStateHook.CONTENT, null)
        if (blockSW) view?.evaluateJavascript(WorkerNeutralize.CONTENT, null)
        view?.evaluateJavascript(GaBlocker.CONTENT, null)
        view?.evaluateJavascript("window.__splPowerSavePref=$powerSave;", null)
        view?.evaluateJavascript(PowerSave.CONTENT, null)
        view?.evaluateJavascript(SettingsFix.CONTENT, null)
        view?.evaluateJavascript(VideoPark.CONTENT, null)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        Log.w(TAG, "Renderer process gone: crashed=${detail?.didCrash()}")
        view?.let {
            it.stopLoading()
            it.destroy()
        }
        onRenderProcessGone?.invoke()
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame != true) return
        val code = try { error?.errorCode ?: -1 } catch (_: Exception) { -1 }
        val desc = try { error?.description?.toString() ?: "" } catch (_: Exception) { "" }
        onWebViewError?.invoke(code, desc)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame != true) return
        val status = try { errorResponse?.statusCode ?: 0 } catch (_: Exception) { 0 }
        if (status >= 400) {
            onWebViewError?.invoke(status, "HTTP $status")
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val url = request.url.toString()

        if (isAnalyticsDomain(url)) {
            val headers = mapOf("Access-Control-Allow-Origin" to "*")
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", headers,
                ByteArrayInputStream(ByteArray(0)))
        }

        if (AdIdStore.matches(url)) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        val useProxy = view.context.getSharedPreferences("spotilol_prefs", 0)
            .getString("ConnectionMode", "normal") == "proxy"

        if (!useProxy) {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = request.method
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val isGoogle = isGoogleAuthUrl(url)
                    for ((k, v) in request.requestHeaders) {
                        val lk = k.lowercase(Locale.ROOT)
                        if (lk != "x-requested-with" && lk != "sec-gpc" && !lk.startsWith("sec-ch-ua") &&
                            !(isGoogle && lk == "user-agent")
                        ) {
                            conn.setRequestProperty(k, v)
                        }
                    }
                    if (isGoogle) {
                        conn.setRequestProperty("User-Agent", DESKTOP_UA)
                        val cookie = CookieManager.getInstance().getCookie(url)
                        if (!cookie.isNullOrEmpty()) conn.setRequestProperty("Cookie", cookie)
                    }
                    conn.setRequestProperty("sec-gpc", "1")
                    conn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"")
                    conn.setRequestProperty("sec-ch-ua-mobile", "?0")
                    conn.setRequestProperty("sec-ch-ua", "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Google Chrome\";v=\"150\"")
                    conn.connect()
                    if (isGoogle) {
                        conn.headerFields.forEach { (key, values) ->
                            if (key != null && key.equals("Set-Cookie", ignoreCase = true)) {
                                values.forEach { CookieManager.getInstance().setCookie(url, it) }
                            }
                        }
                        CookieManager.getInstance().flush()
                    }
                    val contentType = conn.contentType
                    if (contentType == "audio/mpeg" &&
                        !url.contains("podz-content") && !url.contains("gew4-spclient") &&
                        isAdAudioUrl(url)
                    ) {
                        view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
                        val silent = view.context.assets?.open("silent.mp3") ?: return null
                        return WebResourceResponse("audio/mpeg", null, silent)
                    }
                } finally {
                    conn.disconnect()
                }
            } catch (_: Exception) {
                return null
            }
            return null
        }

        val adMatch = matchAdCdn(url)
        if (adMatch != null) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        return null
    }

    private fun isGoogleAuthUrl(url: String?): Boolean {
        if (url == null) return false
        val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()
            ?.lowercase() ?: return false
        return host == "google.com" ||
            host.endsWith(".google.com") ||
            host.contains(".google.") ||
            host.endsWith(".youtube.com") ||
            host == "youtube.com"
    }

    private fun injectPlayerControl(view: WebView) {
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        val autoPlayMode = prefs.getString("APlayMode", "disabled") ?: "disabled"
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)
        val customCss = prefs.getString("CustomCss", "") ?: ""
        val playerMode = prefs.getString("PlayerMode", "spotilol") ?: "spotilol"
        val useProxy = prefs.getString("ConnectionMode", "normal") == "proxy"
        val takeControl = prefs.getBoolean("TakeControl", true)

        val js = buildString {
            append("window.autoPlayMode='$autoPlayMode';\n")
            append("window.closeNpPref=$closeNowPlay;\n")
            append("window.__spotilolUseProxy=$useProxy;\n")
            append("window.__splTakeControl=$takeControl;\n")
            if (prefs.getBoolean("DebugOverlay", false)) {
                append(DevLogPrelude.js())
                append("\n")
            }
            append(PlayerCore.CONTENT)
            append(TrackObserver.CONTENT)
            append(ClassicBridge.CONTENT)
            append(MediaUpdater.CONTENT)
            append(LibraryFetcher.CONTENT)
            append(LibraryParser.CONTENT)
            append(PlaybackControls.CONTENT)
            append(MainLoop.CONTENT)
            append(AutoFeatures.CONTENT)
            append(AndroidTracker.CONTENT)
            append(SearchOverlay.CONTENT)
            append(DownloadButton.CONTENT)
            append(DownloadProgress.CONTENT)
            append("""
                (function(){
                    var recAcc=function(){
                        try{
                            var uw=document.querySelector('[data-testid="user-widget-link"]');
                            if(uw){
                                var txt=(uw.textContent||'').split('\n')[0].trim();
                                if(txt) AndBridge.recAccountName(txt);
                            }
                        }catch(e){}
                    };
                    setTimeout(recAcc,5000);
                    setInterval(recAcc,60000);
                })();
            """.trimIndent())
            append(CssHack.CONTENT)
            append(ModalFix.CONTENT)
            append(ErrorDialogRestyle.CONTENT)
            append(ToastFix.CONTENT)
            append(LyricsSyncFix.CONTENT)
            append(QueueAutoClose.CONTENT)
            if (playerMode == "spotilol") {
                append(SpotilolPlayer.CONTENT)
            }
        }
        val cleanJs = JsUtils.stripConsoleLogs(js) + "\n" +
                buildAmoledJs(amoledEnabled) + "\n" +
                AccentTheme.buildAccentJs(view.context) + "\n" +
                buildCustomCssJs(customCss)
        if (playerMode == "original") {
            view.evaluateJavascript(cleanJs + "\n(function(){var s=document.createElement('style');s.id='spl-np-show';s.textContent='aside[data-testid=\"now-playing-bar\"]{display:flex!important}';document.head.appendChild(s);})();", null)
        } else {
            view.evaluateJavascript(cleanJs, null)
        }
    }

    private fun registerPrefsListener(view: WebView) {
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        prefsListener?.let { prefs.unregisterOnSharedPreferenceChangeListener(it) }
        prefsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "PlayerMode") {
                val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
                val mode = prefs.getString("PlayerMode", "spotilol") ?: "spotilol"
                switchPlayerMode(wv, mode)
            } else if (key == "PowerSave") {
                val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
                val on = prefs.getBoolean("PowerSave", false)
                wv.evaluateJavascript("if(window.__splApplyPowerSave) window.__splApplyPowerSave($on);", null)
            } else if (key == "AmoledTheme" || key == "CustomCss") {
                val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
                val amoled = prefs.getBoolean("AmoledTheme", false)
                val css = prefs.getString("CustomCss", "") ?: ""
                wv.evaluateJavascript(buildAmoledJs(amoled), null)
                wv.evaluateJavascript(buildCustomCssJs(css), null)
            }
            if (key == "PaletteSeed" || key == "MaterialYou") {
                val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
                wv.evaluateJavascript(AccentTheme.buildAccentJs(wv.context), null)
            }
            if (key == "TakeControl"){
                val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
                val on = prefs.getBoolean("TakeControl", true)
                wv.evaluateJavascript("window.__splTakeControl=$on;", null)
            }
            if (key == "BlockServiceWorker") {
                val wv = currentWebView ?: return@OnSharedPreferenceChangeListener
                val enabled = prefs.getBoolean("BlockServiceWorker", true)
                if (enabled) {
                    wv.evaluateJavascript(WorkerNeutralize.CONTENT, null)
                    wv.evaluateJavascript("""
                        try {
                            if(navigator.serviceWorker){
                                navigator.serviceWorker.getRegistrations().then(function(regs){
                                    regs.forEach(function(r){ r.unregister(); });
                                });
                            }
                        } catch(e){}
                    """.trimIndent(), null)
                } else {
                    wv.reload()
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun switchPlayerMode(view: WebView, mode: String) {
        if (mode == "original") {
            val js = """
                (function(){
                    var pl=document.getElementById('spotilolPlayerControls');
                    if(pl) pl.style.display='none';
                    var s=document.createElement('style');
                    s.id='spl-np-show';
                    s.textContent='aside[data-testid="now-playing-bar"]{display:flex!important}';
                    document.head.appendChild(s);
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        } else {
            view.evaluateJavascript("if(typeof initSpotilolPlayer!=='function'){" + SpotilolPlayer.CONTENT + "}", null)
            val js = """
                (function(){
                    var s=document.getElementById('spl-np-show');
                    if(s) s.remove();
                    var npb=document.querySelector('aside[data-testid="now-playing-bar"]');
                    if(npb) npb.style.display='none';
                    var pl=document.getElementById('spotilolPlayerControls');
                    if(pl){pl.style.display='flex';}
                    else if(typeof initSpotilolPlayer==='function'){initSpotilolPlayer();}
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        }
    }

    private fun onPageFinishedClean(view: WebView, js: String) {
        view.evaluateJavascript(JsUtils.stripConsoleLogs(js), null)
    }

    companion object {
        private const val TAG = "SpotifyWebViewClient"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"
    }
}
