package com.tangem.helpers.base

import android.Manifest
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.tangem.tap.MainActivity
import com.tangem.tap.TangemApplication
import com.tangem.tap.domain.TangemSdkManager
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
abstract class BaseAutoTestCase : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @Inject
    lateinit var tangemSdkManager: TangemSdkManager

    @get:Rule
    open val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA
    )

    private val hiltRule = HiltAndroidRule(this)

    @Rule
    @JvmField
    val ruleChain = RuleChain
        .outerRule(hiltRule)
        .around(ApplicationInjectionExecutionRule())

    protected fun setupHooks(
        additionalBeforeSection: () -> Unit = {},
        additionalAfterSection: () -> Unit = {},
    ) = before {
        hiltRule.inject()
        Intents.init()
        additionalBeforeSection()
    }.after {
        additionalAfterSection()
        Intents.release()
    }

}
