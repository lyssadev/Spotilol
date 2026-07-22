package com.project.lol.webview

import android.graphics.Bitmap
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.project.lol.webview.helpers.*
import com.project.lol.webview.injections.*
import java.io.ByteArrayInputStream

class SpotifyWebViewClient(
    private val onLoginRequired: () -> Unit
) : WebViewClient() {

    private var currentWebView: WebView? = null
    private var prefsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

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
        view?.evaluateJavascript(BrowserSpoof.CONTENT, null)
        view?.evaluateJavascript(FetchOverride.CONTENT, null)
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

        if (isAnalyticsDomain(url)) {
            val headers = mapOf("Access-Control-Allow-Origin" to "*")
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", headers,
                ByteArrayInputStream(ByteArray(0)))
        }

        val adMatch = matchAdCdn(url)
        if (adMatch != null) {
            view.post { view.evaluateJavascript("AndBridge.deferMessage('adblock')", null) }
            val silent = view.context.assets?.open("silent.mp3") ?: return null
            return WebResourceResponse("audio/mpeg", null, silent)
        }

        return null
    }

    private fun injectPlayerControl(view: WebView) {
        val prefs = view.context.getSharedPreferences("spotilol_prefs", 0)
        val autoPlayMode = prefs.getString("APlayMode", "disabled") ?: "disabled"
        val closeNowPlay = prefs.getBoolean("CloseNowPlay", true)
        val amoledEnabled = prefs.getBoolean("AmoledTheme", false)
        val customCss = prefs.getString("CustomCss", "") ?: ""
        val playerMode = prefs.getString("PlayerMode", "spotilol") ?: "spotilol"

        val js = buildString {
            append("window.autoPlayMode='$autoPlayMode';\n")
            append("window.closeNpPref=$closeNowPlay;\n")
            append(PlayerCore.CONTENT)
            append(MediaUpdater.CONTENT)
            append(LibraryFetcher.CONTENT)
            append(LibraryParser.CONTENT)
            append(PlaybackControls.CONTENT)
            append(MainLoop.CONTENT)
            append(AutoFeatures.CONTENT)
            append(AndroidTracker.CONTENT)
            append(CssHack.CONTENT)
            if (playerMode == "spotilol") {
                append(SpotilolPlayer.CONTENT)
            }
        }
        val cleanJs = JsUtils.stripConsoleLogs(js) + "\n" +
                buildAmoledJs(amoledEnabled) + "\n" +
                buildCustomCssJs(customCss)
        if (playerMode == "original") {
            view.evaluateJavascript(cleanJs + "\n(function(){var s=document.createElement('style');s.textContent='aside[data-testid=\"now-playing-bar\"]{display:flex!important}';document.head.appendChild(s);})();", null)
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
    }
}
