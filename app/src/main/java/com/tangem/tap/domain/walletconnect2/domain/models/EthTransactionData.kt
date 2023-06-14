package com.tangem.tap.domain.walletconnect2.domain.models

import com.tangem.tap.domain.walletconnect2.domain.WcEthereumTransaction
import com.tangem.tap.features.details.redux.walletconnect.WcEthTransactionType

data class EthTransactionData(
    val transaction: WcEthereumTransaction,
    val networkId: String,
    val rawDerivationPath: String?,
    val id: Long,
    val topic: String,
    val type: WcEthTransactionType,
    val metaName: String,
    val metaUrl: String,
)