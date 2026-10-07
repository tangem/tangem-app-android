package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.math.BigDecimal

/**
 * A payment-account token holding a positive balance, [isAvailable] when the off-ramp accepts it. Only available tokens
 * can be withdrawn from; the rest are listed but cannot be picked.
 */
internal data class BankWithdrawSource(
    val network: Network,
    val chainId: Long,
    val token: CryptoCurrency.Token,
    val fiatBalance: BigDecimal,
    val isAvailable: Boolean,
) {
    val id: String get() = "${network.rawId}:${token.contractAddress.lowercase()}"
}