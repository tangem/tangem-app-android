package com.tangem.data.account.converter

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.TokensGroupType
import com.tangem.utils.converter.TwoWayConverter

/**
 * Converts a [UserTokensResponse.GroupType] to a [TokensGroupType] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object TokensGroupTypeConverter : TwoWayConverter<UserTokensResponse.GroupType, TokensGroupType> {

    override fun convert(value: UserTokensResponse.GroupType): TokensGroupType {
        return when (value) {
            UserTokensResponse.GroupType.NETWORK -> TokensGroupType.NETWORK
            UserTokensResponse.GroupType.NONE,
            UserTokensResponse.GroupType.TOKEN,
            -> TokensGroupType.NONE
        }
    }

    override fun convertBack(value: TokensGroupType): UserTokensResponse.GroupType {
        return when (value) {
            TokensGroupType.NONE -> UserTokensResponse.GroupType.NONE
            TokensGroupType.NETWORK -> UserTokensResponse.GroupType.NETWORK
        }
    }
}