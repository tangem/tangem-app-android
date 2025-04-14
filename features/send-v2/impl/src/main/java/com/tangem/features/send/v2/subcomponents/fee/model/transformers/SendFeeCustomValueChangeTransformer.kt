package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.model.converters.SendFeeCustomFieldConverter
import com.tangem.utils.transformer.Transformer

internal class SendFeeCustomValueChangeTransformer(
    private val index: Int,
    private val value: String,
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

        val updatedCustomValues = customFeeConverter.onValueChange(feeSelectorUM, index, value)

        return state.copy(
            feeSelectorUM = feeSelectorUM.copy(
                customValues = updatedCustomValues,
                selectedFee = customFeeConverter.convertBack(updatedCustomValues),
            ),
        )
    }
}