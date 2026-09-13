package com.tangem.features.tokendetails.deeplink

import kotlinx.coroutines.flow.SharedFlow

interface ExpressDeepLinkListener {
    val actionFlow: SharedFlow<String>
}