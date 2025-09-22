package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.TokensSortType
import com.tangem.utils.converter.TwoWayConverter

/**
 * Converts a [UserTokensResponse.SortType] to a [TokensSortType] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object TokensSortTypeConverter : TwoWayConverter<UserTokensResponse.SortType, TokensSortType> {

    override fun convert(value: UserTokensResponse.SortType): TokensSortType {
        return when (value) {
            UserTokensResponse.SortType.BALANCE -> TokensSortType.BALANCE
            UserTokensResponse.SortType.MANUAL,
            UserTokensResponse.SortType.MARKETCAP,
            -> TokensSortType.NONE
        }
    }

    override fun convertBack(value: TokensSortType): UserTokensResponse.SortType {
        return when (value) {
            TokensSortType.NONE -> UserTokensResponse.SortType.MANUAL
            TokensSortType.BALANCE -> UserTokensResponse.SortType.BALANCE
        }
    }
}