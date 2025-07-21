package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.bitcoin.BitcoinCustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.ethereum.EthereumCustomFeeConverter
import com.tangem.features.send.v2.subcomponents.fee.model.converters.custom.kaspa.KaspaCustomFeeConverter
import com.tangem.utils.converter.TwoWayConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class FeeSelectorCustomFieldConverter(
    private val feeSelectorIntents: FeeSelectorIntents,
    private val appCurrency: AppCurrency,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val normalFee: Fee,
) : TwoWayConverter<Fee, ImmutableList<CustomFeeFieldUM>> {

    private val ethereumCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        EthereumCustomFeeConverter(
            onCustomFeeValueChange = feeSelectorIntents::onCustomFeeValueChange,
            onNextClick = null,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        )
    }

    private val bitcoinCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        BitcoinCustomFeeConverter(
            onCustomFeeValueChange = feeSelectorIntents::onCustomFeeValueChange,
            onNextClick = null,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        )
    }

    private val kaspaCustomFeeConverter by lazy(LazyThreadSafetyMode.NONE) {
        KaspaCustomFeeConverter(
            onCustomFeeValueChange = feeSelectorIntents::onCustomFeeValueChange,
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

    fun onValueChange(
        feeSelectorState: FeeSelectorUM.Content,
        customValues: ImmutableList<CustomFeeFieldUM>,
        index: Int,
        value: String,
    ) = when (val fee = feeSelectorState.fees.normal) {
        is Fee.Ethereum -> ethereumCustomFeeConverter.onValueChange(
            feeValue = fee,
            customValues = customValues,
            index = index,
            value = value,
        )
        is Fee.Bitcoin -> bitcoinCustomFeeConverter.onValueChange(
            customValues = customValues,
            index = index,
            value = value,
            txSize = fee.txSize,
        )
        is Fee.Kaspa -> kaspaCustomFeeConverter.onValueChange(
            customValues = customValues,
            index = index,
            value = value,
        )
        else -> customValues
    }

    fun tryAutoFixValue(feeSelectorState: FeeSelectorUM.Content, customValues: ImmutableList<CustomFeeFieldUM>) =
        when (val fees = feeSelectorState.fees) {
            is TransactionFee.Choosable -> fees.minimum
            is TransactionFee.Single -> fees.normal
        }.let {
            when (it) {
                is Fee.Kaspa -> kaspaCustomFeeConverter.tryAutoFixValue(
                    minimumFee = it,
                    customValues = customValues,
                )
                else -> customValues
            }
        }
}