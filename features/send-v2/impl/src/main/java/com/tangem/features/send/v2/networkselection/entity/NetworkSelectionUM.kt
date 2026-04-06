package com.tangem.features.send.v2.networkselection.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class NetworkSelectionUM(
    val searchBar: SearchBarUM,
    val walletGroups: ImmutableList<WalletGroupUM>,
    val isBalanceHidden: Boolean,
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
)