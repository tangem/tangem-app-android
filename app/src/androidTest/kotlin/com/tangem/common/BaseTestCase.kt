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
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
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
        // Record the launch-time feature-toggle overrides so the report shows the run configuration.
        FeatureToggleArgs.rawArg()?.takeIf { it.isNotBlank() }?.let { overrides ->
            Allure.parameter("feature_toggles", overrides)
        }
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

    /**
     * Waits until [block] stops throwing (or [timeoutMillis] elapses). Use in scenario (BaseTestCase extension)
     * code where flakySafely is unavailable; in test bodies prefer flakySafely.
     */
    fun awaitSuccess(timeoutMillis: Long = WAIT_UNTIL_TIMEOUT, block: () -> Unit) {
        composeTestRule.waitUntil(timeoutMillis = timeoutMillis) { runCatching(block).isSuccess }
    }

    private fun applicationInjectionRule(): ApplicationInjectionExecutionRule {
        // Toggle states can be set from code here — base defaults for the UI-test build. Launch-time
        // overrides (the `feature_toggles` GitHub Actions input / Allure TestOps launch parameter, delivered
        // via the `featureToggles` instrumentation arg) merge on top and win. Keys must be current
        // FeatureToggles rawNames; stale keys match nothing and are logged as ignored.
        val baseToggles = mapOf(
            "VISA_ONBOARDING_ENABLED" to true, // "undefined" on develop → forced on for tests
        )
        // Overrides supplied at launch time (GitHub Actions `feature_toggles` input / Allure TestOps launch
        // parameter, delivered via the `featureToggles` instrumentation arg) win over the base map. Empty
        // when nothing was passed, so the default behaviour is unchanged.
        val launchOverrides = FeatureToggleArgs.fromInstrumentation()

        return ApplicationInjectionExecutionRule(toggleStates = baseToggles + launchOverrides)
    }

    private companion object {
        const val WIREMOCK_BASE_URL_ARG = "wiremockBaseUrl"
        const val SEMANTIC_TREE_PRINT_TIMEOUT_MS = 5_000L
    }
}