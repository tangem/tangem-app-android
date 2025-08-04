package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.model.converters.SendFeeCustomFieldConverter
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer

internal class SendFeeCustomAutoFixTransformer(
    private val clickIntents: SendFeeClickIntents,
    private val appCurrency: AppCurrency,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<FeeUM> {

    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState
        val feeSelectorUM = state.feeSelectorUM as? FeeSelectorUM.Content ?: return state

        val customFeeConverter = SendFeeCustomFieldConverter(
            clickIntents = clickIntents,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            normalFee = feeSelectorUM.fees.normal,
        )

        return when (feeSelectorUM.selectedType) {
            FeeType.Slow,
            FeeType.Market,
            FeeType.Fast,
            -> state
            FeeType.Custom -> {
                val updatedCustomValues = customFeeConverter.tryAutoFixValue(feeSelectorUM)
                state.copy(
                    feeSelectorUM = feeSelectorUM.copy(
                        customValues = updatedCustomValues,
                        selectedFee = customFeeConverter.convertBack(updatedCustomValues),
                    ),
                )
            }
        }
    }
}