package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.custom.BitcoinCustomFeeConverter
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter
import com.tangem.features.send.impl.presentation.state.fee.custom.KaspaCustomFeeConverter
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class FeeConverter(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : Converter<FeeSelectorState.Content, Fee> {

    private val ethereumCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        EthereumCustomFeeConverter(
            clickIntents = clickIntents,
            stateRouterProvider = stateRouterProvider,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
        )
    }

    private val bitcoinCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        BitcoinCustomFeeConverter(
            clickIntents = clickIntents,
            stateRouterProvider = stateRouterProvider,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
        )
    }

    private val kaspaCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        KaspaCustomFeeConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
        )
    }

    override fun convert(value: FeeSelectorState.Content): Fee {
        return when (val fees = value.fees) {
            is TransactionFee.Choosable -> {
                when (value.selectedFee) {
                    FeeType.Slow -> fees.minimum
                    FeeType.Market -> fees.normal
                    FeeType.Fast -> fees.priority
                    FeeType.Custom -> convertCustom(value, fees)
                }
            }
            is TransactionFee.Single ->
                when (value.selectedFee) {
                    FeeType.Market -> fees.normal
                    FeeType.Custom -> convertCustom(value, fees)
                    else -> fees.normal
                }
        }
    }

    private fun convertCustom(feeSelectorState: FeeSelectorState.Content, fees: TransactionFee): Fee {
        val customValues = feeSelectorState.customValues
        val normalFee = fees.normal
        return if (customValues.isEmpty()) {
            normalFee
        } else {
            when (normalFee) {
                is Fee.Ethereum -> ethereumCustomFeeConverter.convertBack(normalFee = normalFee, value = customValues)
                is Fee.Bitcoin -> bitcoinCustomFeeConverter.convertBack(normalFee = normalFee, value = customValues)
                is Fee.Kaspa -> kaspaCustomFeeConverter.convertBack(normalFee = normalFee, value = customValues)
                else -> {
                    val customFee = customValues.firstOrNull()
                    Fee.Common(
                        normalFee.amount.copy(
                            value = customFee?.value?.parseToBigDecimal(customFee.decimals),
                        ),
                    )
                }
            }
        }
    }
}