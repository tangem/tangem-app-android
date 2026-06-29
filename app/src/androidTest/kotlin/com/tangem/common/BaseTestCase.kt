package com.tangem.common

import android.Manifest
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import com.kaspersky.components.alluresupport.interceptors.step.ScreenshotStepInterceptor
import com.kaspersky.components.alluresupport.withForcedAllureSupport
import com.kaspersky.components.composesupport.config.addComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.tangem.common.allure.FailedStepScreenshotInterceptor
import com.tangem.common.constants.TestConstants.ALLURE_LABEL_NAME
import com.tangem.common.constants.TestConstants.ALLURE_LABEL_VALUE
import com.tangem.common.rules.ApiEnvironmentRule
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.datasource.utils.WireMockRedirectInterceptor
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION
import com.tangem.tap.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import io.qameta.allure.kotlin.Allure
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
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
    lateinit var walletManagersStore: WalletManagersStore

    @Inject
    lateinit var getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase

    @Inject
    lateinit var singleAccountListSupplier: SingleAccountListSupplier

    private val hiltRule = HiltAndroidRule(this)
    private val apiEnvironmentRule = ApiEnvironmentRule()
    private val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.POST_NOTIFICATIONS,
    )

    /**
     * It is important to use `ComposeRule` without specifying an activity to ensure that the initialization order of
     * all test rules is fully controlled.
     */
    val composeTestRule = createEmptyComposeRule()

    private val semanticTreePrinterRule = object : TestWatcher() {
        override fun failed(e: Throwable?, description: Description?) {
            runCatching {
                runBlocking {
                    withTimeoutOrNull(SEMANTIC_TREE_PRINT_TIMEOUT_MS) { printAllRoots() }
                }
            }
        }
    }

    @Rule
    @JvmField
    val ruleChain: TestRule = RuleChain
        .outerRule(hiltRule)
        .around(applicationInjectionRule())
        .around(permissionRule)
        .around(apiEnvironmentRule)
        .around(composeTestRule)
        .around(semanticTreePrinterRule)

    /**
     * Initialization order is important:
     * – DI dependencies must be injected first,
     * – then the API environment should be set up,
     * – and only after that the activity should be launched.
     */
    protected fun setupHooks(
        additionalBeforeAppLaunchSection: () -> Unit = {},
        additionalBeforeSection: () -> Unit = {},
        additionalAfterSection: () -> Unit = {},
    ) = before {
        Allure.label(ALLURE_LABEL_NAME, ALLURE_LABEL_VALUE)
        // Setup WireMock redirect for CI with local WireMock instances
        val wiremockUrl = InstrumentationRegistry.getArguments().getString(WIREMOCK_BASE_URL_ARG)
        WireMockRedirectInterceptor.overriddenBaseUrl = wiremockUrl
        additionalBeforeAppLaunchSection()
        hiltRule.inject()
        runBlocking {
            appPreferencesStore.editData { mutablePreferences ->
                mutablePreferences.set(
                    key = PreferencesKeys.NOTIFICATIONS_USER_ALLOW_SEND_ADDRESSES_KEY,
                    value = false
                )
            }
            appPreferencesStore.editData { mutablePreferences ->
                mutablePreferences.set(
                    key = PreferencesKeys.getShouldShowNotificationKey("EnablePushesReminderNotification"),
                    value = false
                )
            }
            appPreferencesStore.editData { mutablePreferences ->
                mutablePreferences.set(
                    key = PreferencesKeys.getShouldShowInitialPermissionScreen(PUSH_PERMISSION),
                    value = false
                )
            }
        }
        apiEnvironmentRule.setup(apiConfigsManager)
        ActivityScenario.launch(MainActivity::class.java)
        Intents.init()
        additionalBeforeSection()
    }.after {
        additionalAfterSection()
        Intents.release()
    }

    /**
     * Prints the Compose semantics tree to logcat for debugging UI tests.
     *
     * @param rootIndex       Use rootIndex > 0, if you need to print semantics tree for bottom sheet.
     *                        Default: 0.
     * @param useUnmergedTree When true, shows unmerged tree with all individual nodes.
     *                        Use for accessing inner elements of compound components.
     *                        Default: false (merged tree - accessibility view).
     * @param tag             Log tag for filtering in logcat. Default: "SEMANTIC_TREE".
     * @param maxDepth        Maximum nesting level to print. Use to avoid log overflow.
     *                        Default: Int.MAX_VALUE (unlimited depth).
     */
    fun printSemanticTree(
        rootIndex: Int = 0,
        useUnmergedTree: Boolean = false,
        tag: String = "SEMANTIC_TREE",
        maxDepth: Int = Int.MAX_VALUE
    ) {
        composeTestRule.onAllNodes(isRoot(), useUnmergedTree = useUnmergedTree)[rootIndex]
            .printToLog(tag, maxDepth)
    }

    fun printAllRoots(
        tag: String = "ComposeTree",
    ) {
        val roots = composeTestRule.onAllNodes(isRoot())
        val count = roots.fetchSemanticsNodes().size
        repeat(count) { index ->
            roots[index].printToLog("$tag[$index]")
        }
    }

    fun waitForIdle() = composeTestRule.waitForIdle()

    private fun applicationInjectionRule(): ApplicationInjectionExecutionRule {
        return ApplicationInjectionExecutionRule(
            toggleStates = mapOf(
                "SWAP_REDESIGN_ENABLED" to false,
                "ACCOUNTS_FEATURE_ENABLED" to true,
                "MAIN_SCREEN_QR_SCANNING_ENABLED" to true,
                "ASSETS_DISCOVERY_ENABLED" to true,
                "VISA_ONBOARDING_ENABLED" to true,
                // Version-gated toggles released in versions <= 6.0 — forced on so tests run against the actual
                // build even when the app version resolves to 1.0.0-SNAPSHOT on CI (then 1.0.0 < x.xx would
                // disable them). On the releases/6.0 branch every toggle with version <= 6.0 ships enabled.
                // 5.37
                "HEDERA_ERC20_ENABLED" to true,
                // 5.39
                "STAKING_ETH_ENABLED" to true,
                "DYNAMIC_ADDRESSES_ENABLED" to true,
                "SOLANA_TX_HISTORY_ENABLED" to true,
                "SOLANA_SCALED_UI_AMOUNT_ENABLED" to true,
                "SWAP_AB_ENABLED" to true,
                "AND_15310_ADD_FUNDS_STAGE1" to true,
                "AND_15009_SWAP_PROVIDER_FILTER_ENABLED" to true,
                "AND_15101_TANGEM_PAY_HOT_WALLET_ONBOARDING" to true,
                "AND_15402_ADI_MAIN_SCREEN_DEFAULT_ENABLED" to true,
                "AND_15103_SWAP_RATE_EXPERIENCE_ENABLED" to true,
                "AND_15122_SWAP_PREDEFINED_BUTTONS_ENABLED" to true,
                "TWI_1512_HIDE_STORIES_FOR_REFERRAL_ENABLED" to true,
                // 5.40
                "TWI_1377_MANAGE_FUNDS" to true,
                // 6.0
                "APP_REDESIGN_ENABLED" to true,
                "TWI_1326_YIELD_MODE_SWAP_ENABLED" to true,
                "AND_15207_SWAP_SWITCH_TO_TRANSFER_ENABLED" to true,
                "AND_15120_SWAP_INTEGRATED_APPROVE" to true,
                "AND_15596_ONBOARDING_PUSH_NOTIFICATION_DOUBLE_ASK_AB_ENABLED" to true,
                "AND_15258_QUICK_TOP_UP_ENABLED" to true,
                "AND_15368_VISA_PAY_REDESIGN" to true,
                "AND_15364_VISA_PAY_CARD_CLOSE" to true,
                "AND_15489_EXPRESS_SHARE_BUTTON_ENABLED" to true,
                "AND_15235_VISA_MULTIPLE_CARDS" to true,
                "AND_15715_SWAP_BEST_DEX_RATE_ENABLED" to true,
            )
        )
    }

    private companion object {
        const val WIREMOCK_BASE_URL_ARG = "wiremockBaseUrl"
        const val SEMANTIC_TREE_PRINT_TIMEOUT_MS = 5_000L
    }
}