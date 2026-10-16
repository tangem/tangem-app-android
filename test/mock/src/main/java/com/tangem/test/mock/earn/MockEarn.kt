package com.tangem.test.mock.earn

import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType

/** Earn fixtures, every argument defaulted so a test states only what it is about. */
object MockEarn {

    fun yieldToken(apy: String = "0.092", networkId: String = "ethereum", tokenSymbol: String = "USDT") = EarnToken(
        apy = apy,
        networkId = networkId,
        rewardType = EarnRewardType.APY,
        type = EarnType.YIELD,
        tokenId = tokenSymbol.lowercase(),
        tokenSymbol = tokenSymbol,
        tokenName = tokenSymbol,
        tokenAddress = "0xTokenAbC",
        decimalCount = 6,
    )
}