package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * The gacha vault as this device last read it. The amount is that of its associated token account, which is what
 * spins are paid from; the SOL on the vault owner key pays rent and fees and is not part of it.
 *
 * @property usdcToken    the token the vault holds, built from the mint its on-chain config names
 * @property usdcAmount   the token's amount, already scaled by the mint's decimals
 * @property vaultAddress the vault PDA of the gacha program, shown to external senders: a wallet deriving the
 *                        token account from it lands in [ataAddress]
 * @property ataAddress   the vault's associated token account — where an in-app top-up is sent, since the SDK
 *                        refuses a program-owned PDA as a recipient
 * @property usdcFiatRate rate of [usdcToken] into the app's currency, `null` until the quote answers
 */
@Serializable
data class CollectiblesVaultBalance(
    val usdcToken: CryptoCurrency.Token,
    val usdcAmount: SerializedBigDecimal,
    val vaultAddress: String,
    val ataAddress: String,
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

    fun cryptoCurrencyStatus(source: StatusSource): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = usdcToken,
        value = depositCryptoCurrencyStatusValue(
            amount = usdcAmount,
            fiatRate = usdcFiatRate,
            depositAddress = vaultAddress,
            source = source,
        ),
    )
}