package com.tangem.features.tokenreceive.entity

import com.tangem.domain.models.network.Network

internal enum class ReceiveExtrasHint {
    None,
    NoMemo,
    NoTag,
}

internal fun receiveExtrasHint(
    shouldShowMemoDisclaimer: Boolean,
    transactionExtrasType: Network.TransactionExtrasType,
): ReceiveExtrasHint {
    if (!shouldShowMemoDisclaimer) return ReceiveExtrasHint.None

    return when (transactionExtrasType) {
        Network.TransactionExtrasType.NONE -> ReceiveExtrasHint.None
        Network.TransactionExtrasType.MEMO -> ReceiveExtrasHint.NoMemo
        Network.TransactionExtrasType.DESTINATION_TAG -> ReceiveExtrasHint.NoTag
    }
}