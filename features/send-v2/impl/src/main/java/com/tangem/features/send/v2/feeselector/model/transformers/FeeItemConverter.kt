package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class FeeItemConverter(
    private val suggestedFeeState: FeeSelectorParams.SuggestedFeeState,
    private val normalFee: Fee,
    private val feeSelectorIntents: FeeSelectorIntents,
    private val appCurrency: AppCurrency,
    cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Converter<TransactionFee, ImmutableList<FeeItem>> {

    private val customFeeFieldConverter = FeeSelectorCustomFieldConverter(
        feeSelectorIntents = feeSelectorIntents,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = cryptoCurrencyStatus,
        normalFee = normalFee,
    )

    override fun convert(value: TransactionFee): ImmutableList<FeeItem> {
        val fees = mutableListOf<FeeItem>()

        when (suggestedFeeState) {
            FeeSelectorParams.SuggestedFeeState.None -> Unit
            is FeeSelectorParams.SuggestedFeeState.Suggestion -> fees.add(
                FeeItem.Suggested(
                    title = suggestedFeeState.title,
                    fee = suggestedFeeState.fee,
                ),
            )
        }
        when (value) {
            is TransactionFee.Choosable -> {
                fees.add(FeeItem.Slow(fee = value.minimum))
                fees.add(FeeItem.Market(fee = value.normal))
                fees.add(FeeItem.Fast(fee = value.priority))
            }
            is TransactionFee.Single -> {
                fees.add(FeeItem.Market(fee = value.normal))
            }
        }
        val customFeeFields = customFeeFieldConverter.convert(normalFee)
        if (customFeeFields.isNotEmpty()) {
            fees.add(
                FeeItem.Custom(
                    fee = customFeeFieldConverter.convertBack(customFeeFields),
                    customValues = customFeeFields,
                ),
            )
        }

        return fees.toImmutableList()
    }
}