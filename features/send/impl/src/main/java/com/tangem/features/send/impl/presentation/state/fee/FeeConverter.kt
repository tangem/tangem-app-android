package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class FeeConverter(
    private val clickIntents: SendClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<FeeSelectorState.Content, Fee> {

    private val ethereumCustomFeeConverter by lazy {
        EthereumCustomFeeConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    override fun convert(value: FeeSelectorState.Content): Fee {
        return when (val fees = value.fees) {
            is TransactionFee.Choosable -> {
                when (value.selectedFee) {
                    FeeType.SLOW -> fees.minimum
                    FeeType.MARKET -> fees.normal
                    FeeType.FAST -> fees.priority
                    FeeType.CUSTOM -> convertCustom(value, fees)
                }
            }
            is TransactionFee.Single -> fees.normal
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
