package com.tangem.common.ui.markets.tokenselector

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class TokenSelectorContentUM(
    val sections: ImmutableList<TokenSelectorSectionUM>,
) : TangemBottomSheetConfigContent

@Immutable
data class AccountHeaderData(
    val accountName: TextReference,
    val cryptoPortfolioIcon: CryptoPortfolioIcon,
)

@Immutable
sealed interface TokenSelectorSectionUM {

    data class WalletHeader(val walletName: String) : TokenSelectorSectionUM

    data class TokenGroup(
        val accountHeader: AccountHeaderData?,
        val items: ImmutableList<UserAssetItemUM.Single>,
    ) : TokenSelectorSectionUM
}