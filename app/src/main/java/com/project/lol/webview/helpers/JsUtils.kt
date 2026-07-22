package com.project.lol.webview.helpers

object JsUtils {
    fun stripConsoleLogs(code: String): String {
        return CONSOLE_LOG_PATTERN.replace(code, "")
    }

    private val CONSOLE_LOG_PATTERN = Regex("console\\.log\\([^)]*\\);?")
}
