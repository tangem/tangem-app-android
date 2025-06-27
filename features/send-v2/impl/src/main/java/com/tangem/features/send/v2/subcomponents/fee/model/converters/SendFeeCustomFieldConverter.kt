package com.tangem.features.send.v2.subcomponents.fee.model.converters

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.bitcoin.BitcoinCustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum.EthereumCustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.kaspa.KaspaCustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.ui.state.CustomFeeFieldUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.utils.converter.TwoWayConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeCustomFieldConverter(
    private val clickIntents: SendFeeClickIntents,
    private val appCurrency: AppCurrency,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val normalFee: Fee,
) : TwoWayConverter<Fee, ImmutableList<CustomFeeFieldUM>> {

    private val ethereumCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        EthereumCustomFeeConverter(
            onCustomFeeValueChange = clickIntents::onCustomFeeValueChange,
            onNextClick = clickIntents::onNextClick,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        )
    }

    private val bitcoinCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        BitcoinCustomFeeConverter(
            onCustomFeeValueChange = clickIntents::onCustomFeeValueChange,
            onNextClick = clickIntents::onNextClick,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        )
    }

    private val kaspaCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        KaspaCustomFeeConverter(
            onCustomFeeValueChange = clickIntents::onCustomFeeValueChange,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        )
    }

    override fun convert(value: Fee): ImmutableList<CustomFeeFieldUM> {
        return when (value) {
            is Fee.Ethereum -> ethereumCustomFeeConverter.convert(value)
            is Fee.Bitcoin -> bitcoinCustomFeeConverter.convert(value)
            is Fee.Kaspa -> kaspaCustomFeeConverter.convert(value)
            else -> persistentListOf()
        }
    }

    override fun convertBack(value: ImmutableList<CustomFeeFieldUM>): Fee {
        return if (value.isEmpty()) {
            normalFee
        } else {
            when (normalFee) {
                is Fee.Ethereum -> ethereumCustomFeeConverter.convertBack(normalFee = normalFee, value = value)
                is Fee.Bitcoin -> bitcoinCustomFeeConverter.convertBack(normalFee = normalFee, value = value)
                is Fee.Kaspa -> kaspaCustomFeeConverter.convertBack(normalFee = normalFee, value = value)
                else -> {
                    val customFee = value.firstOrNull()
                    Fee.Common(
                        normalFee.amount.copy(
                            value = customFee?.value?.parseToBigDecimal(customFee.decimals),
                        ),
                    )
                }
            }
        }
    }

    fun onValueChange(feeSelectorState: FeeSelectorUM.Content, index: Int, value: String) =
        when (val fee = feeSelectorState.fees.normal) {
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
        }

    fun tryAutoFixValue(feeSelectorState: FeeSelectorUM.Content) = when (feeSelectorState.fees) {
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
    }
}