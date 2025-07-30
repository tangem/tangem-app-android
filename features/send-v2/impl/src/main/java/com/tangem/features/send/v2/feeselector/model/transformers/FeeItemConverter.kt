package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class FeeItemConverter(
    private val feeStateConfiguration: FeeSelectorParams.FeeStateConfiguration,
    private val normalFee: Fee,
    private val feeSelectorIntents: FeeSelectorIntents,
    private val appCurrency: AppCurrency,
    cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Converter<FeeItemConverter.Input, ImmutableList<FeeItem>> {

    private val customFeeFieldConverter = FeeSelectorCustomFieldConverter(
        feeSelectorIntents = feeSelectorIntents,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = cryptoCurrencyStatus,
        normalFee = normalFee,
    )

    override fun convert(value: Input): ImmutableList<FeeItem> {
        val fees = mutableListOf<FeeItem>()

        when (feeStateConfiguration) {
            FeeSelectorParams.FeeStateConfiguration.None -> fees.addFeeItemsFull(value = value)
            is FeeSelectorParams.FeeStateConfiguration.Suggestion -> {
                fees.addFeeItemSuggested(feeStateConfiguration)
                fees.addFeeItemsFull(value = value)
            }
            FeeSelectorParams.FeeStateConfiguration.ExcludeLow -> {
                fees.addFeeItemsLimited(value = value)
            }
        }

        return fees.toImmutableList()
    }

    private fun MutableList<FeeItem>.addFeeItemsFull(value: Input) {
        when (value.transactionFee) {
            is TransactionFee.Choosable -> {
                add(FeeItem.Slow(fee = value.transactionFee.minimum))
                add(FeeItem.Market(fee = value.transactionFee.normal))
                add(FeeItem.Fast(fee = value.transactionFee.priority))
            }
            is TransactionFee.Single -> {
                add(FeeItem.Market(fee = value.transactionFee.normal))
            }
        }
        val customFee = value.customFee ?: constructCustomFee()
        customFee?.let(::add)
    }

    private fun MutableList<FeeItem>.addFeeItemsLimited(value: Input) {
        when (value.transactionFee) {
            is TransactionFee.Choosable -> {
                add(FeeItem.Market(fee = value.transactionFee.normal))
                add(FeeItem.Fast(fee = value.transactionFee.priority))
            }
            is TransactionFee.Single -> {
                add(FeeItem.Market(fee = value.transactionFee.normal))
            }
        }
    }

    private fun MutableList<FeeItem>.addFeeItemSuggested(
        feeStateConfiguration: FeeSelectorParams.FeeStateConfiguration.Suggestion,
    ) {
        add(
            FeeItem.Suggested(
                title = feeStateConfiguration.title,
                fee = feeStateConfiguration.fee,
            ),
        )
    }

    private fun constructCustomFee(): FeeItem.Custom? {
        val customFeeFields = customFeeFieldConverter.convert(normalFee)

        if (customFeeFields.isEmpty()) return null

        return FeeItem.Custom(
            fee = customFeeFieldConverter.convertBack(customFeeFields),
            customValues = customFeeFields,
        )
    }

    data class Input(val transactionFee: TransactionFee, val customFee: FeeItem.Custom?)
}