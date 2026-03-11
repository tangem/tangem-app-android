package com.tangem.features.onramp.swap.availablepairs

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.decompose.ComposableListContentComponent
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager
import com.tangem.features.onramp.swap.availablepairs.model.AddToPortfolioRoute
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import kotlinx.coroutines.flow.StateFlow

/** Token list component that present list of available tokens for swap */
@Stable
internal interface AvailableSwapPairsComponent : ComposableListContentComponent<TokenListUM> {

    val bottomSheetNavigation: SlotNavigation<AddToPortfolioRoute>
    val addToPortfolioManager: AddToPortfolioManager?
    val addToPortfolioCallback: AddToPortfolioComponent.Callback

    /** Component factory */
    interface Factory : ComponentFactory<Params, AvailableSwapPairsComponent>

    /**
     * Params
     *
     * @property userWalletId   id of multi-currency wallet
     * @property selectedStatus flow of selected status
     * @property onTokenClick   callback for token click
     */
    data class Params(
        val userWalletId: UserWalletId,
        val selectedStatus: StateFlow<CryptoCurrencyStatus?>,
        val onTokenClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
    )
}