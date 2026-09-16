package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.math.BigDecimal

/** A payment-account token the wizard can withdraw from: accepted by the off-ramp and holding a positive balance. */
internal data class BankWithdrawSource(
    val network: Network,
    val chainId: Long,
    val token: CryptoCurrency.Token,
    val fiatBalance: BigDecimal,
) {
    val id: String get() = "${network.rawId}:${token.contractAddress.lowercase()}"
}