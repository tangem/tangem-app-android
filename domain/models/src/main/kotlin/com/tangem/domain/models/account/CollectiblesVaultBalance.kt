package com.tangem.domain.models.account

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * What the gacha vault holds, as this device last read it — the USDC on the vault's associated token account,
 * which is what spins are paid from. The SOL on the vault owner key pays rent and fees and is not part of the
 * account's balance.
 *
 * @property usdcAmount   USDC on the vault's associated token account
 * @property usdcFiatRate rate of USDC into the app's currency, `null` until the quote answers
 */
@Serializable
data class CollectiblesVaultBalance(
    val usdcAmount: SerializedBigDecimal,
    val usdcFiatRate: SerializedBigDecimal? = null,
) {

    /**
     * The vault priced in the app's currency, `null` while it holds something no quote can price yet. An empty
     * vault needs no rate, so it never blanks the wallet total.
     */
    val fiatAmount: SerializedBigDecimal?
        get() = when {
            usdcAmount.signum() == 0 -> SerializedBigDecimal.ZERO
            usdcFiatRate == null -> null
            else -> usdcAmount.multiply(usdcFiatRate)
        }
}