package com.tangem.domain.collectibles.account

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * The currency the gacha vault is priced with, as the quote service identifies it. The id is network-agnostic on
 * purpose: the vault lives on devnet while the program does.
 */
object CollectiblesVaultCurrencies {

    val USDC_RAW_ID = CryptoCurrency.RawID(value = "usd-coin")
}