package com.tangem.feature.wallet.child.organizetokens.model.converter.error

import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensState
import com.tangem.feature.wallet.child.organizetokens.model.converter.InProgressStateConverter
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