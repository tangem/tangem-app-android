package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceTypeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.utils.transformer.Transformer

/**
 * Toggles the balance type between [TokenBalanceTypeUM.Type.ALL] and [TokenBalanceTypeUM.Type.AVAILABLE].
 *
 * No-op if the current balance state is not [TokenDetailsBalanceBlockUM.Content] or its
 * [TokenDetailsBalanceBlockUM.Content.tokenBalanceTypeUM] is not [TokenBalanceTypeUM.Multiple].
 */
internal class ToggleBalanceTypeTransformer : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM {
        val content = prevState.balanceBlockUM as? TokenDetailsBalanceBlockUM.Content ?: return prevState
        val multiple = content.tokenBalanceTypeUM as? TokenBalanceTypeUM.Multiple ?: return prevState

        val nextType = when (multiple.type) {
            TokenBalanceTypeUM.Type.ALL -> TokenBalanceTypeUM.Type.AVAILABLE
            TokenBalanceTypeUM.Type.AVAILABLE -> TokenBalanceTypeUM.Type.ALL
        }

        return prevState.copy(
            balanceBlockUM = content.copy(
                tokenBalanceTypeUM = multiple.copy(type = nextType),
            ),
        )
    }
}