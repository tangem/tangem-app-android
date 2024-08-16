package com.tangem.lib.crypto.models

import java.math.BigDecimal
import java.math.BigInteger

sealed interface ProxyFee {

    val gasLimit: BigInteger
    val fee: ProxyAmount

    data class Common(
        override val gasLimit: BigInteger,
        override val fee: ProxyAmount,
    ) : ProxyFee

    data class CardanoToken(
        override val gasLimit: BigInteger,
        override val fee: ProxyAmount,
        val minAdaValue: BigDecimal,
    ) : ProxyFee

    data class Filecoin(
        override val gasLimit: BigInteger,
        override val fee: ProxyAmount,
        val gasPremium: Long,
    ) : ProxyFee
}
