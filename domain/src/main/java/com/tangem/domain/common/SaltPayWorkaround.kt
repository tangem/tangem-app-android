package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CardIdRange
import com.tangem.common.contains

object SaltPayWorkaround {
    @Suppress("MagicNumber")
    val visaBatches = listOf("AE02", "AE03") + attachTestVisaBatches()

    val walletCardIds = listOf(
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
    ) + attachTestWalletCardIds()

    val walletCardIdRanges = listOf(
        CardIdRange("AC05000000000003", "AC05000000023997")!!,
    ) + attachTestWalletCardIdRanges()

    fun tokenFrom(blockchain: Blockchain): Token {
        return when (blockchain) {
            Blockchain.SaltPay -> Token(
                name = "WXDAI",
                symbol = "wxDAI",
                contractAddress = "0x4200000000000000000000000000000000000006",
                decimals = 18,
                id = "wrapped-xdai",
            )
            else -> error("It is not SaltPay")
        }
    }

    fun isVisaBatchId(batchId: String): Boolean = visaBatches.contains(batchId)

    fun isWalletCardId(cardId: String): Boolean {
        return if (walletCardIds.contains(cardId)) true else walletCardIdRanges.contains(cardId)
    }

    private fun attachTestVisaBatches(): List<String> {
        return listOf("FF03")
    }

    private fun attachTestWalletCardIds(): List<String> {
        return listOf("FF04000000000232")
    }

    private fun attachTestWalletCardIdRanges(): List<CardIdRange> {
        return listOf(CardIdRange("FF04000000000000", "FF04999999999999")!!)
    }
}
