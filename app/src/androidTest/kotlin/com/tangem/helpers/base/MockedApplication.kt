package com.tangem.helpers.base

import com.tangem.tap.TapApplication

open class MockedApplication : TapApplication() {

    override fun onCreate() {
        // we intentionally don't call super.onCreate()
    }

}