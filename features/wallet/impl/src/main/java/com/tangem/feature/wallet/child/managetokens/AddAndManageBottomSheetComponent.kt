package com.tangem.feature.wallet.child.managetokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.managetokens.model.AddAndManageModel
import com.tangem.feature.wallet.child.managetokens.ui.AddAndManageBottomSheetContent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import kotlinx.serialization.builtins.serializer

internal class AddAndManageBottomSheetComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val model: AddAndManageModel = getOrCreateModel(params)

    private val portfolioSelectorSlot = childSlot(
        source = model.portfolioSelectorNavigation,
        serializer = Unit.serializer(),
        handleBackButton = false,
        childFactory = { _, context -> portfolioSelectorChild(context) },
    )

    private fun portfolioSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
            ),
        )

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val portfolioSelectorSlot by portfolioSelectorSlot.subscribeAsState()
        val state by model.state.collectAsStateWithLifecycle()

        AddAndManageBottomSheetContent(
            onAddTokensClick = model::onAddTokensClick,
            shouldShowOrganizeButton = state.shouldShowOrganize,
            onOrganizeTokensClick = model::onOrganizeTokensClick,
            onDismiss = ::dismiss,
        )

        portfolioSelectorSlot.child?.instance?.BottomSheet()
    }

    data class Params(
        val userWalletId: UserWalletId,
        val onDismiss: () -> Unit,
        val onOrganizeTokensClick: () -> Unit,
        val onManageTokensClick: (AccountId) -> Unit,
    )
}