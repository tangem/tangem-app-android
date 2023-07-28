package com.tangem.feature.wallet.presentation.organizetokens.utils.converter.error

import com.tangem.common.Provider
import com.tangem.domain.tokens.error.TokenListSortingError
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensState
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.InProgressStateConverter
import com.tangem.utils.converter.Converter

internal class TokenListSortingErrorConverter(
    private val currentState: Provider<OrganizeTokensState>,
    private val inProgressStateConverter: InProgressStateConverter,
) : Converter<TokenListSortingError, OrganizeTokensState> {

    // TODO: https://tangem.atlassian.net/browse/AND-4021
    override fun convert(value: TokenListSortingError): OrganizeTokensState {
        return inProgressStateConverter.convertBack(currentState())
    }
}
