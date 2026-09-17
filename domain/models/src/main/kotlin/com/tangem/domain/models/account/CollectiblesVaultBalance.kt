package com.tangem.domain.models.account

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * What the gacha vault holds, as this device last read it — the balance of the vault's associated token account,
 * which is what spins are paid from. The SOL on the vault owner key pays rent and fees and is not part of the
 * account's balance.
 *
 * @property usdcToken    the token the vault holds, built from the mint its on-chain config names
 * @property usdcAmount   the token's amount, already scaled by the mint's decimals
 * @property usdcFiatRate rate of [usdcToken] into the app's currency, `null` until the quote answers
 */
@Serializable
data class CollectiblesVaultBalance(
    val usdcToken: CryptoCurrency.Token,
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