package com.tangem.common

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.dataStoreFile
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import javax.inject.Inject

abstract class BaseTestCase : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
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

    companion object {
        private const val INIT_DELAY = 1000L
    }

}