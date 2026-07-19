package com.project.lol.webview

import android.os.Handler
import android.os.Looper
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

class SpotifyWebChromeClient : WebChromeClient() {

    private var childWebView: WebView? = null

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        childWebView?.let {
            try { it.destroy() } catch (_: Exception) {}
        }
        val newWebView = WebView(view?.context ?: return false).apply {
            webViewClient = object : WebViewClient() {
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    view?.loadUrl(url ?: return false)
                    return true
                }
            }
        }
        childWebView = newWebView
        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg?.sendToTarget()
        return true
    }

    @Suppress("DEPRECATION")
    override fun onPermissionRequest(permissionRequest: PermissionRequest?) {
        permissionRequest ?: return
        Handler(Looper.getMainLooper()).post {
            val resources = permissionRequest.resources
            if (resources.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)) {
                permissionRequest.grant(resources)
            } else {
                permissionRequest.deny()
            }
        }
    }

    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceId: String?) {
        android.util.Log.d("SpotifyJS", "$message [$sourceId:$lineNumber]")
    }
}
