package com.tangem.features.polymarket.impl.soccer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.polymarket.impl.soccer.ui.SoccerRulesSheet
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.soccer.model.PolymarketSoccerModel
import com.tangem.features.polymarket.impl.soccer.ui.PolymarketSoccerScreen

internal class PolymarketSoccerComponent(
    appComponentContext: AppComponentContext,
    params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {
    private val model: PolymarketSoccerModel = getOrCreateModel(params)

    private val rulesSlot = childSlot(
        source = model.rulesNavigation,
        serializer = null,
        key = "soccerRules",
        handleBackButton = false,
        childFactory = { rules, context ->
            SoccerRulesComponent(childByContext(context), rules, onDismiss = { model.rulesNavigation.dismiss() })
        },
    )

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val chart by model.chartState.collectAsStateWithLifecycle()
        val rules by rulesSlot.subscribeAsState()
        PolymarketSoccerScreen(
            state = state,
            chart = chart,
            onChartPeriodChange = model::onChartPeriodChange,
            onBack = model::onBack,
            onRetry = model::onRetry,
            onOutcomeClick = model::onOutcomeClick,
            onRules = model::onRules,
            onPositionChange = model::onPositionChange,
            onReadMore = model::onReadMore,
            onOpenUrl = model::onOpenUrl,
            modifier = modifier,
        )
        rules.child?.instance?.BottomSheet()
    }

    data class Params(val eventId: String, val userWalletId: UserWalletId)
}

private class SoccerRulesComponent(
    context: AppComponentContext,
    private val rules: PolymarketSoccerCardUM.Rules,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent, AppComponentContext by context {
    override fun dismiss() = onDismiss()

    @Composable
    override fun BottomSheet() = SoccerRulesSheet(rules, onDismiss = ::dismiss)
}