package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.math.BigDecimal

/**
 * A payment-account token the wizard can withdraw from. [isAvailable] is false when the off-ramp does not accept the
 * token on that network or the balance is empty; such sources are listed but cannot be selected.
 */
internal data class BankWithdrawSource(
    val network: Network,
    val chainId: Long,
    val token: CryptoCurrency.Token,
    val fiatBalance: BigDecimal?,
    val isAvailable: Boolean,
) {
    val id: String get() = "${network.rawId}:${token.contractAddress.lowercase()}"
}