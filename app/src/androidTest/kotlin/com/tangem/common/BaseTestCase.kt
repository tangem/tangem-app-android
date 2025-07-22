package com.tangem.common

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.intent.Intents
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.components.alluresupport.interceptors.step.ScreenshotStepInterceptor
import com.kaspersky.components.alluresupport.withForcedAllureSupport
import com.kaspersky.components.composesupport.config.addComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.tangem.common.allure.FailedStepScreenshotInterceptor
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import javax.inject.Inject

abstract class BaseTestCase : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withForcedAllureSupport(
        shouldRecordVideo = false
    ).apply {
        stepWatcherInterceptors = stepWatcherInterceptors.filter {
            it !is ScreenshotStepInterceptor
        }.toMutableList()
        stepWatcherInterceptors.addAll(
            listOf(
                FailedStepScreenshotInterceptor(screenshots)
            )
        )
    }.addComposeSupport()
) {

    @Inject
    lateinit var tangemSdkManager: TangemSdkManager

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    private val hiltRule = HiltAndroidRule(this)
    private val permissionRule =  GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA,
    )
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Rule
    @JvmField
    val ruleChain: TestRule = RuleChain
        .outerRule(hiltRule)
        .around(ApplicationInjectionExecutionRule())
        .around(permissionRule)
        .around(composeTestRule)

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