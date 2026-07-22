package com.project.lol.webview.helpers

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
