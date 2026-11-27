package com.tangem.data.earn

import com.tangem.datasource.api.tangemTech.models.EarnResponse
import com.tangem.datasource.api.tangemTech.models.EarnTokenResponse

internal fun createEarnResponse(
    apy: String = "0.031",
    networkId: String = "ethereum",
    rewardType: String = "apy",
    type: String = "yield",
    tokenId: String = "ethereum",
    tokenSymbol: String = "ETH",
    tokenName: String = "Ethereum",
    tokenAddress: String? = null,
    decimalCount: Int? = 18,
): EarnResponse = EarnResponse(
    apy = apy,
    networkId = networkId,
    rewardType = rewardType,
    type = type,
    token = EarnTokenResponse(
        id = tokenId,
        symbol = tokenSymbol,
        name = tokenName,
        address = tokenAddress,
        decimalCount = decimalCount,
    ),
)