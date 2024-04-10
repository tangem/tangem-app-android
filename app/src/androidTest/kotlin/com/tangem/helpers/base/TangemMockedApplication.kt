package com.tangem.helpers.base

import com.tangem.tap.TangemApplication

open class TangemMockedApplication : TangemApplication() {

    override fun onCreate() {
        // we intentionally don't call super.onCreate()
    }

}