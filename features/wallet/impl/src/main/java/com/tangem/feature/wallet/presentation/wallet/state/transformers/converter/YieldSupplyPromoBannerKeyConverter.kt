package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TokenConverterParams
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class YieldSupplyPromoBannerKeyConverter(
    private val yieldModuleApyMap: Map<String, BigDecimal>,
    private val shouldShowMainPromo: Boolean,
) : Converter<TokenConverterParams, String?> {

    override fun convert(value: TokenConverterParams): String? {
        if (!shouldShowMainPromo) return null

        val currencies = when (value) {
            is TokenConverterParams.Wallet -> value.tokenList.flattenCurrencies()
            is TokenConverterParams.Account -> value.accountList.flattenCurrencies()
        }.filter { status ->
            status.value is CryptoCurrencyStatus.Loaded ||
                status.value is CryptoCurrencyStatus.Custom
        }

        val tokens = currencies.filter { it.currency is CryptoCurrency.Token }

        if (tokens.any { it.value.yieldSupplyStatus?.isActive == true }) return null
        if (yieldModuleApyMap.isEmpty()) return null

        val max = tokens.asSequence()
            .mapNotNull { status ->
                val token = status.currency as? CryptoCurrency.Token ?: return@mapNotNull null
                val tokenKey = token.yieldSupplyKey()
                val shouldIgnoreCase = BlockchainUtils.isCaseInsensitiveContractAddress(token.network.rawId)

                val matchedKey = yieldModuleApyMap.keys.firstOrNull { mapKey ->
                    mapKey.equals(tokenKey, shouldIgnoreCase)
                } ?: return@mapNotNull null

                status to matchedKey
            }
            .maxByOrNull { (status, _) -> status.value.amount ?: BigDecimal.ZERO }

        return max?.second
    }
}