package com.tangem.common

import android.Manifest
import androidx.test.rule.GrantPermissionRule
import com.atiurin.ultron.core.compose.config.UltronComposeConfig
import com.atiurin.ultron.core.compose.createUltronComposeRule
import com.atiurin.ultron.core.compose.listeners.ComposDebugListener
import com.atiurin.ultron.core.config.UltronCommonConfig
import com.atiurin.ultron.core.config.UltronConfig
import com.atiurin.ultron.core.test.UltronTest
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Rule
import javax.inject.Inject

abstract class BaseTestCase : UltronTest() {
    @Inject
    lateinit var tangemSdkManager: TangemSdkManager

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    @get:Rule(order = 0)
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA
    )
    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule (order = 2)
    val injectionRule = ApplicationInjectionExecutionRule()

    @get:Rule(order = 3)
    val composeRule = createUltronComposeRule<MainActivity>()

    override val beforeTest: () -> Unit = {
        hiltRule.inject()
        runBlocking {
            delay(INIT_DELAY)
        }
    }

    override val afterTest: () -> Unit = {
        runBlocking {
            appPreferencesStore.editData { prefs -> prefs.clear() }
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun config() {
            UltronConfig.applyRecommended()
            UltronComposeConfig.applyRecommended()
            UltronCommonConfig.addListener(ComposDebugListener())
        }

        private const val INIT_DELAY = 2000L
    }
}