package com.tangem.commands.personalization.entities

import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils

object Issuer {

    val name = "TANGEM SDK"
    val id = name + "\u0000"
    val dataPrivateKey = "11121314151617184771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()

    val dataPublicKey: ByteArray by lazy { CryptoUtils.generatePublicKey(dataPrivateKey) }

    val transactionPrivateKey =
            "11121314151617184771ED81F2BACF57479E4735EB1405081918171615141312".hexToBytes()

    val transactionPublicKey: ByteArray by lazy { CryptoUtils.generatePublicKey(transactionPrivateKey) }
}