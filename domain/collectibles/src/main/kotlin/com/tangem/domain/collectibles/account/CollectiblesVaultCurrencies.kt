package com.tangem.domain.collectibles.account

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * The currency the gacha vault is priced with, as the quote service identifies it. The chain gives the mint; the
 * name, the symbol and the decimals it does not, so those are described here rather than looked up.
 */
object CollectiblesVaultCurrencies {

    const val TOKEN_NAME = "USD Coin"
    const val TOKEN_SYMBOL = "USDC"
    const val DECIMALS = 6

    /** Network-agnostic on purpose: a test card's vault lives on the testnet chain, which has no quote of its own. */
    val USDC_RAW_ID = CryptoCurrency.RawID(value = "usd-coin")
}