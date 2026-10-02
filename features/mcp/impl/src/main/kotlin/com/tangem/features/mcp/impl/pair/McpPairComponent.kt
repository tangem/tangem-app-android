package com.tangem.features.mcp.impl.pair

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.inner.InnerRouter
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.mcp.impl.pair.model.McpPairModel
import com.tangem.features.mcp.impl.pair.model.McpPairRoutes
import com.tangem.features.mcp.impl.pair.model.McpPairTexts
import com.tangem.features.mcp.impl.pair.ui.McpPairBottomSheet
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Container of the MCP pairing flow. Unlike the WalletConnect pairing flow, every destination lives
 * inside a single bottom sheet: the sheet shell stays put while [contentStack] swaps its content.
 */
internal class McpPairComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    private val model: McpPairModel = getOrCreateModel(params)

    private val innerRouter = InnerRouter<McpPairRoutes>(
        stackNavigation = model.stackNavigation,
        popCallback = { onBack() },
    )

    private val agentInfoComponent = McpAgentInfoComponent(
        appComponentContext = child(key = "mcpAgentInfo", router = innerRouter),
        model = model,
    )

    private val portfolioSelectorComponent: PortfolioSelectorComponent = portfolioSelectorComponentFactory.create(
        context = child(key = "mcpPortfolioSelector", router = innerRouter),
        params = PortfolioSelectorComponent.Params(
            portfolioFetcher = model.portfolioFetcher,
            controller = model.selectorController,
            settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
        ),
    )

    private val contentStack = childStack(
        key = "mcpPairStack",
        source = model.stackNavigation,
        serializer = McpPairRoutes.serializer(),
        initialConfiguration = McpPairRoutes.AgentInfo,
        handleBackButton = true,
        childFactory = { config, _ -> contentChild(config) },
    )

    init {
        // Picks up notifications enabled in the OS settings the warning sent the user to.
        lifecycle.doOnResume { model.onResume() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val stack by contentStack.subscribeAsState()
        val state by model.uiState.collectAsStateWithLifecycle()
        val selectorTitle by portfolioSelectorComponent.title.collectAsStateWithLifecycle()

        RequestPushPermissionEffect()

        BackHandler(onBack = ::onBack)
        McpPairBottomSheet(
            stack = stack,
            state = state,
            title = when (stack.active.configuration) {
                McpPairRoutes.AgentInfo -> McpPairTexts.connectionTitle
                McpPairRoutes.PortfolioSelector -> selectorTitle
            },
            onBack = ::onBack,
            onDismiss = model::onDecline,
        )
    }

    @Composable
    private fun RequestPushPermissionEffect() {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { model.onPushPermissionResult() },
        )

        LaunchedEffect(Unit) {
            model.requestPushPermission.collect {
                val permission = getPushPermissionOrNull()
                // Below Android 13 there is no runtime permission, so the OS settings are the only way out.
                if (permission == null) model.onPushPermissionResult() else permissionLauncher.launch(permission)
            }
        }
    }

    private fun onBack() {
        if (contentStack.backStack.isEmpty()) model.onDecline() else model.stackNavigation.pop()
    }

    private fun contentChild(config: McpPairRoutes): ComposableContentComponent = when (config) {
        McpPairRoutes.AgentInfo -> agentInfoComponent
        McpPairRoutes.PortfolioSelector -> portfolioSelectorComponent
    }

    internal data class Params(val request: McpRequest.Pair)

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: Params): McpPairComponent
    }
}