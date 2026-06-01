package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.common.ui.earn.EarnBlockUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TokenDetailsUM(
    val topAppBarUM: TokenDetailsTopAppBarUM,
    val balanceBlockUM: TokenDetailsBalanceBlockUM,
    val notifications: ImmutableList<TangemMessageUM>,
    val earnBlockState: EarnBlockUM?,
    val marketPriceBlockState: MarketPriceBlockState,
    val pullToRefreshConfig: PullToRefreshConfig,
    val isBalanceHidden: Boolean,
    val isMarketPriceAvailable: Boolean,
    val addFundsUM: AddFundsUM,
    val transferUM: TransferUM,
    val zeroBalanceActionsUM: ZeroBalanceActionsUM,
)

@Immutable
internal data class TokenDetailsTopAppBarUM(
    val titleState: TitleState,
    val subtitle: TextReference,
    val onBackClick: () -> Unit,
    val menuItems: ImmutableList<TangemDropdownMenuItem>,
) {
    @Immutable
    sealed interface TitleState {
        val tokenName: String

        data class Simple(
            override val tokenName: String,
        ) : TitleState

        data class WithWallet(
            override val tokenName: String,
            val walletName: String,
            val deviceIconUM: DeviceIconUM,
        ) : TitleState

        data class WithAccount(
            override val tokenName: String,
            val accountName: TextReference,
            val accountIconUM: AccountIconUM.CryptoPortfolio,
        ) : TitleState
    }
}