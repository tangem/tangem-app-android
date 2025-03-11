package com.tangem.tap.features.home.errors

import com.tangem.common.core.TangemError

interface TangemSdkErrorHandler {

    fun onErrorReceived(error: TangemError)
}