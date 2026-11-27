package com.tangem.data.earn.converter

import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.utils.converter.Converter

internal object EarnTokenConverter : Converter<EarnResponse, EarnToken?> {

    /** Null for a type the app does not know yet: the backend adds types ahead of the clients. */
    override fun convert(value: EarnResponse): EarnToken? {
        val type = convertEarnType(value.type) ?: return null
        return EarnToken(
            apy = value.apy,
            networkId = value.networkId,
            rewardType = convertEarnRewardType(value.rewardType),
            type = type,
            tokenId = value.token.id,
            tokenSymbol = value.token.symbol,
            tokenName = value.token.name,
            tokenAddress = value.token.address,
            decimalCount = value.token.decimalCount,
        )
    }

    private fun convertEarnRewardType(type: String): EarnRewardType {
        return enumValues<EarnRewardType>().find { it.name.equals(type, ignoreCase = true) }
            ?: EarnRewardType.APY
    }

    private fun convertEarnType(type: String): EarnType? {
        return enumValues<EarnType>().find { it.name.equals(type, ignoreCase = true) }
    }
}