package com.tangem.features.send.impl.navigation

import com.tangem.features.send.api.navigation.SendRouter

interface InnerSendRouter : SendRouter {

    /** Open website by [url] */
    fun openUrl(url: String)
}