package com.tangem.common

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.tangem.common.di.TangemMockedApplication_Application

class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, TangemMockedApplication_Application::class.java.name, context)
    }
}