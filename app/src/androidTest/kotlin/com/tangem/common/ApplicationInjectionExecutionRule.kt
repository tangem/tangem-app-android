package com.tangem.common

import androidx.test.core.app.ApplicationProvider
import com.tangem.tap.ApplicationEntryPoint
import com.tangem.tap.TangemApplication
import dagger.hilt.android.testing.OnComponentReadyRunner
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ApplicationInjectionExecutionRule : TestRule {

    private val tangemApplication: TangemApplication
        get() = ApplicationProvider.getApplicationContext()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                OnComponentReadyRunner.addListener(
                    tangemApplication, ApplicationEntryPoint::class.java
                ) { _: ApplicationEntryPoint ->
                    tangemApplication.init()
                }
                base.evaluate()
            }
        }
    }
}