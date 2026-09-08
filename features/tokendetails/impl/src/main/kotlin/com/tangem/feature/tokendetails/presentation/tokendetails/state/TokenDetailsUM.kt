package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
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
    val quickTopUpBlock: QuickTopUpBlockUM? = null,
)

@Immutable
internal data class TokenDetailsTopAppBarUM(
    val titleState: TitleState,
    val subtitle: TextReference,
    val onBackClick: () -> Unit,
    val menuItems: ImmutableList<MenuItemUM>,
) {
    /**
     * One row of the top bar's "•••" context menu: a leading glyph and a label. [isDestructive] renders the row in the
     * error color and puts a divider above it (Hide token).
     */
    @Immutable
    data class MenuItemUM(
        val title: TextReference,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val isDestructive: Boolean = false,
    )

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