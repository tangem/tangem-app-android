package com.tangem.common

import android.app.Application
import android.content.Context
import android.os.Bundle
import com.kaspersky.kaspresso.runner.KaspressoRunner
import com.tangem.common.di.TangemMockedApplication_Application
import com.tangem.datasource.remote.interceptors.WireMockRedirectInterceptor

class HiltTestRunner : KaspressoRunner() {

    override fun onCreate(arguments: Bundle) {
        // MockAware* repositories serve WireMock data under instrumentation, canned data in a hand-launched build.
        System.setProperty("uitest.tangempay.tx_history_from_api", "1")
        System.setProperty("uitest.tangempay.bank_offramp_orders_from_api", "1")
        // The Auth Service device registration starts in Application.onCreate, before BaseTestCase's hooks run.
        WireMockRedirectInterceptor.overriddenBaseUrl = arguments.getString("wiremockBaseUrl")
        super.onCreate(arguments)
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TangemMockedApplication_Application::class.java.name, context)
    }
}