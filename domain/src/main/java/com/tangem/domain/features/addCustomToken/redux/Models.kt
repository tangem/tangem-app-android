package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.DomainWrapped

/**
[REDACTED_AUTHOR]
 */
// describes state the screen, except the form fields
data class ScreenState(
    val contractAddressField: ViewStates.TokenField,
    val network: ViewStates.TokenField,
    val name: ViewStates.TokenField,
    val symbol: ViewStates.TokenField,
    val decimals: ViewStates.TokenField,
    val derivationPath: ViewStates.TokenField,
    val addButton: ViewStates.AddButton
)

sealed class ViewStates {
    data class TokenField(
        val isLoading: Boolean = false,
        val isEnabled: Boolean = true,
        val isVisible: Boolean = true,
    ) : ViewStates()

    data class AddButton(
        val isEnabled: Boolean = true
    ) : ViewStates()
}

data class AddedCurrencies(
    val addedTokens: List<DomainWrapped.TokenWithBlockchain>,
    val addedBlockchains: List<Blockchain>
)