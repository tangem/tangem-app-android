package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.model.converters.FeeConverter
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer

internal class SendFeeSelectTransformer(
    private val feeType: FeeType,
    private val clickIntents: SendFeeClickIntents,
    private val appCurrency: AppCurrency,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<FeeUM> {

    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState
        val feeSelectorUM = state.feeSelectorUM as? FeeSelectorUM.Content ?: return state

        val feeConverter = FeeConverter(
            clickIntents = clickIntents,
            appCurrency = appCurrency,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            fees = feeSelectorUM.fees,
        )

        val updatedFeeSelectorState = feeSelectorUM.copy(
            selectedType = feeType,
            selectedFee = feeConverter.convert(
                FeeConverter.Data(
                    customValues = feeSelectorUM.customValues,
                    feeType = feeType,
                ),
            ),
        )

        val isCustomFeeWasSelected = state.isCustomSelected || updatedFeeSelectorState.selectedType == FeeType.Custom
        return state.copy(
            isCustomSelected = isCustomFeeWasSelected,
            feeSelectorUM = updatedFeeSelectorState,
        )
    }
}