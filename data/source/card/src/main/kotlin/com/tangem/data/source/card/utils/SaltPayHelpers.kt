package com.tangem.data.source.card.utils

import com.tangem.common.CardIdRange

private val visaBatches = arrayOf(
    "AE02",
    "AE03",
    // Test batches
    "FF03",
)
private val walletCardIds = arrayOf(
    "AC01000000033503",
    "AC01000000033594",
    "AC01000000033586",
    "AC01000000034477",
    "AC01000000032760",
    "AC01000000033867",
    "AC01000000032653",
    "AC01000000032752",
    "AC01000000034485",
    "AC01000000033644",
    "AC01000000037454",
    "AC01000000037462",
    "AC03000000076070",
    "AC03000000076088",
    "AC03000000076096",
    "AC03000000076104",
    "AC03000000076112",
    "AC03000000076120",
    "AC03000000076138",
    "AC03000000076146",
    "AC03000000076153",
    "AC03000000076161",
    "AC03000000076179",
    "AC03000000076187",
    "AC03000000076195",
    "AC03000000076203",
    "AC03000000076211",
    "AC03000000076229",
    // Test card IDs
    "FF04000000000232",
)
private val walletCardIdRanges = arrayOf(
    CardIdRange("AC05000000000003", "AC05000000023997")!!,
    // Test card ID ranges
    CardIdRange("FF04000000000000", "FF04999999999999")!!,
)

internal fun isSaltPayWalletCardId(cardId: String): Boolean {
    return cardId in walletCardIds || walletCardIdRanges.any { it.contains(cardId) }
}

internal fun isSaltPayVisaBatchId(batchId: String): Boolean {
    return batchId in visaBatches
}
