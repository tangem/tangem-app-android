package com.tangem.tap.data

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.tap.common.apptheme.MutableAppThemeModeHolder
import com.tangem.tap.network.exchangeServices.SellService

/**
 * Default implementation of [OfframpRepository]
 *
 * @property sellService sell service for getting offramp URL
 */
internal class DefaultOfframpRepository(
    private val sellService: SellService,
) : OfframpRepository {

    override fun getOfframpUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyCode: String,
        walletAddress: String,
    ): String? {
        return sellService.getUrl(
            cryptoCurrency = cryptoCurrency,
            fiatCurrencyName = fiatCurrencyCode,
            walletAddress = walletAddress,
            isDarkTheme = MutableAppThemeModeHolder.isDarkThemeActive,
        )
    }
}