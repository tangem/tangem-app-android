package com.tangem.common

import android.app.Application
import android.content.Context
import com.kaspersky.kaspresso.runner.KaspressoRunner
import com.tangem.common.di.TangemMockedApplication_Application

class HiltTestRunner : KaspressoRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TangemMockedApplication_Application::class.java.name, context)
    }
}