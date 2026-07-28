package com.tangem.data.walletmanager.utils

import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.blockchain.common.Amount as BlockchainAmount
import com.tangem.blockchain.common.AmountType as BlockchainAmountType

/** Maps the blockchain SDK [BlockchainAmount] to the serializable domain [SdkAmount]. */
internal fun BlockchainAmount.toDomain(): SdkAmount = SdkAmount(
    currencySymbol = currencySymbol,
    value = value,
    decimals = decimals,
    type = type.toDomain(),
)

private fun BlockchainAmountType.toDomain(): SdkAmountType = when (this) {
    BlockchainAmountType.Coin -> SdkAmountType.Coin
    BlockchainAmountType.Reserve -> SdkAmountType.Reserve
    is BlockchainAmountType.FeeResource -> SdkAmountType.FeeResource(name = name)
    is BlockchainAmountType.Token -> SdkAmountType.Token(contractAddress = token.contractAddress, id = token.id)
    is BlockchainAmountType.TokenYieldSupply -> SdkAmountType.Token(
        contractAddress = token.contractAddress,
        id = token.id,
    )
}