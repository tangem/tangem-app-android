package com.tangem.features.feed.ui.search.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TokenSelectorContentUM(
    val sections: ImmutableList<TokenSelectorSectionUM>,
) : TangemBottomSheetConfigContent

@Immutable
internal data class AccountHeaderData(
    val accountName: TextReference,
    val cryptoPortfolioIcon: CryptoPortfolioIcon,
)

@Immutable
internal sealed interface TokenSelectorSectionUM {

    data class WalletHeader(val walletName: String) : TokenSelectorSectionUM

    data class TokenGroup(
        val accountHeader: AccountHeaderData?,
        val items: ImmutableList<UserAssetItemUM.Single>,
    ) : TokenSelectorSectionUM
}