package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error

import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.InProgressStateConverter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenListSortingErrorConverter(
    private val currentState: Provider<OrganizeTokensState>,
    private val inProgressStateConverter: InProgressStateConverter,
) : Converter<TokenListSortingError, OrganizeTokensState> {

    override fun convert(value: TokenListSortingError): OrganizeTokensState {
        return inProgressStateConverter.convertBack(currentState())
    }
}