package com.tangem.commands.personalization.entities

import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils

object Acquirer {
    val privateKey = "21222324252627284771ED81F2BACF57479E4735EB1405083927372D40DA9E92".hexToBytes()
    val publicKey: ByteArray by lazy { CryptoUtils.generatePublicKey(privateKey) }
}