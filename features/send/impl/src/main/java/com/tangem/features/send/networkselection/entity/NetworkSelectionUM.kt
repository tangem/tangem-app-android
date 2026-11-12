package com.tangem.features.send.networkselection.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.annotations.RemoveWithToggle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class NetworkSelectionUM(
    val searchBar: SearchBarUM,
    val walletGroups: ImmutableList<WalletGroupUM>,
    val isBalanceHidden: Boolean,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Inline true")
    val isRedesignedTokenRowsEnabled: Boolean = false,
)

@Immutable
internal data class WalletGroupUM(
    val userWalletId: UserWalletId,
    val walletName: String,
    val isExpanded: Boolean,
    val onExpandToggle: () -> Unit,
    val accounts: ImmutableList<AccountGroupUM>,
)

@Immutable
internal data class AccountGroupUM(
    val accountName: TextReference,
    val iconState: CurrencyIconState.CryptoPortfolio?,
    val tokens: ImmutableList<TokenItemState>,
    val hiddenTokensCount: Int,
    val tokenRows: ImmutableList<TangemTokenRow.State> = persistentListOf(),
)