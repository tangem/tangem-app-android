package com.tangem.common

import android.app.Application
import android.content.Context
import android.os.Bundle
import com.kaspersky.kaspresso.runner.KaspressoRunner
import com.tangem.common.di.TangemMockedApplication_Application

class HiltTestRunner : KaspressoRunner() {

    override fun onCreate(arguments: Bundle) {
        // Mirrors MockAwareTangemPayTxHistoryRepository.UITEST_HISTORY_FROM_API_KEY — WireMock serves the
        // history under instrumentation, while a hand-launched mocked build keeps the canned demo list.
        System.setProperty("uitest.tangempay.tx_history_from_api", "1")
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