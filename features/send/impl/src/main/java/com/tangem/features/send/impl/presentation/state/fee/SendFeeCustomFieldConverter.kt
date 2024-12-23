package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.custom.BitcoinCustomFeeConverter
import com.tangem.features.send.impl.presentation.state.fee.custom.EthereumCustomFeeConverter
import com.tangem.features.send.impl.presentation.state.fee.custom.KaspaCustomFeeConverter
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeCustomFieldConverter(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : Converter<Fee, ImmutableList<SendTextField.CustomFee>> {

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

    override fun convert(value: Fee): ImmutableList<SendTextField.CustomFee> {
        return when (value) {
            is Fee.Ethereum -> ethereumCustomFeeConverter.convert(value)
            is Fee.Bitcoin -> bitcoinCustomFeeConverter.convert(value)
            is Fee.Kaspa -> kaspaCustomFeeConverter.convert(value)
            else -> persistentListOf()
        }
    }

    fun onValueChange(feeSelectorState: FeeSelectorState.Content, index: Int, value: String) = feeSelectorState.copy(
        customValues = when (val fee = feeSelectorState.fees.normal) {
            is Fee.Ethereum -> ethereumCustomFeeConverter.onValueChange(
                feeValue = fee,
                customValues = feeSelectorState.customValues,
                index = index,
                value = value,
            )
            is Fee.Bitcoin -> bitcoinCustomFeeConverter.onValueChange(
                customValues = feeSelectorState.customValues,
                index = index,
                value = value,
                txSize = fee.txSize,
            )
            is Fee.Kaspa -> kaspaCustomFeeConverter.onValueChange(
                customValues = feeSelectorState.customValues,
                index = index,
                value = value,
            )
            else -> feeSelectorState.customValues
        },
    )

    fun tryAutoFixValue(feeSelectorState: FeeSelectorState.Content) = feeSelectorState.copy(
        customValues = when (feeSelectorState.fees) {
            is TransactionFee.Choosable -> feeSelectorState.fees.minimum
            is TransactionFee.Single -> feeSelectorState.fees.normal
        }.let {
            when (it) {
                is Fee.Kaspa -> kaspaCustomFeeConverter.tryAutoFixValue(
                    minimumFee = it,
                    customValues = feeSelectorState.customValues,
                )
                else -> feeSelectorState.customValues
            }
        },
    )
}