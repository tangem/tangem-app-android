package com.tangem.domain.common

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CardIdRange
import com.tangem.common.contains

object SaltPayWorkaround {
    fun tokenFrom(blockchain: Blockchain): Token {
        return when (blockchain) {
            Blockchain.SaltPay -> Token(
                name = "WXDAI",
                symbol = "wxDAI",
                contractAddress = "0x4346186e7461cB4DF06bCFCB4cD591423022e417",
                decimals = 18,
                id = "wrapped-xdai",
            )
            Blockchain.SaltPayTestnet -> Token(
                name = "WXDAI Test",
                symbol = "MyERC20",
                contractAddress = "0x69cca8D8295de046C7c14019D9029Ccc77987A48",
                decimals = 0,
                id = "wrapped-xdai",
            )
            else -> throw IllegalArgumentException()
        }
    }

    fun isVisaBatchId(batchId: String): Boolean = visaBatches.contains(batchId)

    fun isWalletCardId(cardId: String): Boolean {
        return if (walletCardIds.contains(cardId)) true else walletCardIdRanges.contains(cardId)
    }

    val visaBatches = listOf(
        "AE02",
        "AE03",
    ) + attachTestVisaBatches()

    private fun attachTestVisaBatches(): List<String> {
        return listOf(
            "FF03",
        )
    }

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

    private fun attachTestWalletCardIds(): List<String> {
        return listOf(
            "FF04000000000232",
        )
    }

    val walletCardIdRanges = listOf(
        CardIdRange("AC05000000000003", "AC05000000023997")!!,
    ) + attachTestWalletCardIdRanges()

    private fun attachTestWalletCardIdRanges(): List<CardIdRange> {
        return listOf(
            CardIdRange("FF04000000000000", "FF04999999999999")!!,
        )
    }
}