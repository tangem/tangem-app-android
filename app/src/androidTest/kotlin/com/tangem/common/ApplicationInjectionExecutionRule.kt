package com.tangem.common

import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.tangem.tap.ApplicationEntryPoint
import com.tangem.tap.TangemApplication
import dagger.hilt.android.testing.OnComponentReadyRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ApplicationInjectionExecutionRule : TestRule {

    private val targetApplication: TangemApplication
        get() = ApplicationProvider.getApplicationContext()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                OnComponentReadyRunner.addListener(
                    targetApplication, ApplicationEntryPoint::class.java
                ) { entryPoint: ApplicationEntryPoint ->
                    runOnUiThread {
                        targetApplication.init()
                    }
                }
                base.evaluate()
            }
        }
    }
}