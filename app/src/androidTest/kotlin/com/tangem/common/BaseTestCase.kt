package com.tangem.common

import android.Manifest
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.rule.GrantPermissionRule
import com.kaspersky.components.alluresupport.interceptors.step.ScreenshotStepInterceptor
import com.kaspersky.components.alluresupport.withForcedAllureSupport
import com.kaspersky.components.composesupport.config.addComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.tangem.common.allure.FailedStepScreenshotInterceptor
import com.tangem.common.rules.ApiEnvironmentRule
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.configtoggle.feature.MutableFeatureTogglesManager
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoId
import com.tangem.tap.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
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
    lateinit var apiConfigsManager: ApiConfigsManager

    @Inject
    lateinit var appPreferencesStore: AppPreferencesStore

    @Inject
    lateinit var featureTogglesManager: FeatureTogglesManager

    @Inject
    lateinit var promoRepository: PromoRepository

    private val hiltRule = HiltAndroidRule(this)
    private val apiEnvironmentRule = ApiEnvironmentRule()
    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA,
    )

    /**
     * It is important to use `ComposeRule` without specifying an activity to ensure that the initialization order of
     * all test rules is fully controlled.
     */
    val composeTestRule = createEmptyComposeRule()

    @Rule
    @JvmField
    val ruleChain: TestRule = RuleChain
        .outerRule(hiltRule)
        .around(ApplicationInjectionExecutionRule())
        .around(permissionRule)
        .around(apiEnvironmentRule)
        .around(composeTestRule)

    /**
     * Initialization order is important:
     * – DI dependencies must be injected first,
     * – then the API environment should be set up,
     * – and only after that the activity should be launched.
     */
    protected fun setupHooks(
        additionalBeforeSection: () -> Unit = {},
        additionalAfterSection: () -> Unit = {},
    ) = before {
        hiltRule.inject()
        runBlocking {
            appPreferencesStore.editData { mutablePreferences ->
                mutablePreferences.set(
                    key = PreferencesKeys.NOTIFICATIONS_USER_ALLOW_SEND_ADDRESSES_KEY,
                    value = false
                )
            }
            promoRepository.setNeverToShowWalletPromo(PromoId.Sepa)
        }
        apiEnvironmentRule.setup(apiConfigsManager)
        ActivityScenario.launch(MainActivity::class.java)
        Intents.init()
        setFeatureToggles()
        additionalBeforeSection()
    }.after {
        additionalAfterSection()
        Intents.release()
    }

    /**
     * Prints the Compose semantics tree to logcat for debugging UI tests.
     *
     * @param useUnmergedTree When true, shows unmerged tree with all individual nodes.
     *                        Use for accessing inner elements of compound components.
     *                        Default: false (merged tree - accessibility view).
     * @param tag             Log tag for filtering in logcat. Default: "SEMANTIC_TREE".
     * @param maxDepth        Maximum nesting level to print. Use to avoid log overflow.
     *                        Default: Int.MAX_VALUE (unlimited depth).
     */
    fun printSemanticTree(
        useUnmergedTree: Boolean = false,
        tag: String = "SEMANTIC_TREE",
        maxDepth: Int = Int.MAX_VALUE)
    {
        composeTestRule.onRoot(useUnmergedTree = useUnmergedTree).printToLog(tag, maxDepth)
    }


    private fun setFeatureToggles() {
        runBlocking {
            with(featureTogglesManager as MutableFeatureTogglesManager) {
                changeToggle("WALLET_CONNECT_REDESIGN_ENABLED", true)
                changeToggle("WALLET_BALANCE_FETCHER_ENABLED", true)
                changeToggle("SEND_VIA_SWAP_ENABLED", true)
                changeToggle("SWAP_REDESIGN_ENABLED", true)
                changeToggle("SEND_REDESIGN_ENABLED", true)
                changeToggle("NEW_ONRAMP_MAIN_ENABLED", true)
            }
        }
    }
}