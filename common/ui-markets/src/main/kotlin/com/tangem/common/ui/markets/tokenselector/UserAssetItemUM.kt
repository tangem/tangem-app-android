package com.tangem.common.ui.markets.tokenselector

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface BalanceDisplayState {

    data class Loaded(
        val cryptoBalance: TextReference,
        val fiatBalance: TextReference,
    ) : BalanceDisplayState

    data class Flickering(
        val cryptoBalance: TextReference,
        val fiatBalance: TextReference,
    ) : BalanceDisplayState

    data class Stale(
        val cryptoBalance: TextReference,
        val fiatBalance: TextReference,
    ) : BalanceDisplayState

    data object Loading : BalanceDisplayState
    data object Unreachable : BalanceDisplayState
}

@Immutable
sealed interface UserAssetItemUM {
    val id: String
    val icon: TangemIconUM
    val tokenName: String
    val tokenSymbol: String
    val onClick: () -> Unit

    data class Single(
        override val id: String,
        override val icon: TangemIconUM,
        override val tokenName: String,
        override val tokenSymbol: String,
        val fiatRate: String?,
        val priceChangeState: PriceChangeState,
        val balanceState: BalanceDisplayState,
        val isBalanceHidden: Boolean,
        val networkName: String,
        override val onClick: () -> Unit,
    ) : UserAssetItemUM

    data class Grouped(
        override val id: String,
        override val icon: TangemIconUM,
        override val tokenName: String,
        override val tokenSymbol: String,
        val tokensCount: Int,
        val balanceState: BalanceDisplayState,
        val isBalanceHidden: Boolean,
        override val onClick: () -> Unit,
    ) : UserAssetItemUM
}