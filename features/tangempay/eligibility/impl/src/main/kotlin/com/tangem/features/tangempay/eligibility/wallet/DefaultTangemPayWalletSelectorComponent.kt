package com.tangem.features.tangempay.eligibility.wallet

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class DefaultTangemPayWalletSelectorComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: TangemPayWalletSelectorComponent.Params,
    portfolioFetcherFactory: PortfolioFetcher.Factory,
    private val controller: PortfolioSelectorController,
    portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
) : TangemPayWalletSelectorComponent, AppComponentContext by context {

    private val selectorComponent = portfolioSelectorComponentFactory.create(
        context = child(key = "tangemPayPortfolioSelector"),
        params = PortfolioSelectorComponent.Params(
            portfolioFetcher = portfolioFetcherFactory.create(
                mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
                scope = componentScope,
            ),
            controller = controller,
            bsCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
                override val onDismiss: () -> Unit = ::dismiss
                override val onBack: () -> Unit = ::dismiss
            },
            settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true),
        ),
    )

    init {
        val eligibleWalletsIds = params.walletsIds.toSet()
        controller.isEnabled.value = { wallet, _ -> wallet.walletId in eligibleWalletsIds }

        controller.selectedAccount
            .filterNotNull()
            .onEach { accountId -> params.listener.onWalletSelected(accountId.userWalletId) }
            .launchIn(componentScope)
    }

    override fun dismiss() {
        params.listener.onWalletSelectorDismiss()
    }

    @Composable
    override fun BottomSheet() {
        selectorComponent.BottomSheet()
    }

    @AssistedFactory
    interface Factory : TangemPayWalletSelectorComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayWalletSelectorComponent.Params,
        ): DefaultTangemPayWalletSelectorComponent
    }
}