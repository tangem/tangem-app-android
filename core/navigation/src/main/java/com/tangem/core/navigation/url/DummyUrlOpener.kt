package com.tangem.core.navigation.url

class DummyUrlOpener : UrlOpener {

    override fun openUrl(url: String) {
        /* no-op */
    }

    override fun openUrlExternalBrowser(url: String) {
        /* no-op */
    }
}