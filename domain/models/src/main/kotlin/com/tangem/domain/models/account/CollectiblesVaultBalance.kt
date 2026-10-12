package com.tangem.domain.models.account

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * The gacha vault as this device last read it. The amount is that of its associated token account, which is what
 * spins are paid from; the SOL on the vault owner key pays rent and fees and is not part of it.
 *
 * @property usdcToken    the token the vault holds, built from the mint its on-chain config names
 * @property usdcAmount   the token's amount, already scaled by the mint's decimals
 * @property ataAddress   the vault's associated token account — where a top-up is sent. The vault itself is a PDA
 *                        of the gacha program, and a transfer addressed to it is refused
 * @property usdcFiatRate rate of [usdcToken] into the app's currency, `null` until the quote answers
 */
@Serializable
data class CollectiblesVaultBalance(
    val usdcToken: CryptoCurrency.Token,
    val usdcAmount: SerializedBigDecimal,
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
        value = buildValue(source = source),
    )

    private fun buildValue(source: StatusSource): CryptoCurrencyStatus.Value {
        val networkAddress = NetworkAddress.Single(
            defaultAddress = NetworkAddress.Address(
                type = NetworkAddress.Address.Type.Primary,
                value = ataAddress,
            ),
        )
        val sources = CryptoCurrencyStatus.Sources(
            networkSource = source,
            quoteSource = source,
            stakingBalanceSource = source,
        )

        return if (usdcFiatRate != null) {
            CryptoCurrencyStatus.Loaded(
                amount = usdcAmount,
                fiatAmount = usdcAmount.multiply(usdcFiatRate),
                fiatRate = usdcFiatRate,
                priceChange = SerializedBigDecimal.ZERO,
                networkAddress = networkAddress,
                sources = sources,
                pendingTransactions = emptySet(),
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
            )
        } else {
            CryptoCurrencyStatus.NoQuote(
                amount = usdcAmount,
                networkAddress = networkAddress,
                yieldSupplyStatus = null,
                sources = sources,
                pendingTransactions = emptySet(),
                hasCurrentNetworkTransactions = false,
            )
        }
    }
}