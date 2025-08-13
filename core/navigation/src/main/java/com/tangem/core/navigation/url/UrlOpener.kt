package com.tangem.core.navigation.url

interface UrlOpener {

    fun openUrl(url: String)

    fun openUrlExternalBrowser(url: String)
}