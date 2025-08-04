package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class FeeSelectorCustomValueChangedTransformer(
    private val index: Int,
    private val value: String,
    private val intents: FeeSelectorIntents,
    private val appCurrency: AppCurrency,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        val state = prevState as? FeeSelectorUM.Content ?: return prevState
        val customFee = state.feeItems.filterIsInstance<FeeItem.Custom>().firstOrNull() ?: return prevState
        val customFeeConverter = FeeSelectorCustomFieldConverter(
            feeSelectorIntents = intents,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            normalFee = state.selectedFeeItem.fee,
        )
        val updatedCustomValues = customFeeConverter.onValueChange(state, customFee.customValues, index, value)
        val newCustomFee = customFee.copy(
            fee = customFeeConverter.convertBack(updatedCustomValues),
            customValues = updatedCustomValues,
        )
        return state.copy(
            feeItems = state.feeItems.map { if (it is FeeItem.Custom) newCustomFee else it }.toImmutableList(),
            selectedFeeItem = newCustomFee,
        )
    }
}