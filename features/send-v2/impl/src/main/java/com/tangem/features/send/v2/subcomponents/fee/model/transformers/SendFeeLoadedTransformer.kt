package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.model.converters.FeeConverter
import com.tangem.features.send.v2.subcomponents.fee.model.converters.SendFeeCustomFieldConverter
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer

internal class SendFeeLoadedTransformer(
    clickIntents: SendFeeClickIntents,
    appCurrency: AppCurrency,
    feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val fees: TransactionFee,
    private val isFeeApproximate: Boolean,
) : Transformer<FeeUM> {

    private val customFeeFieldConverter = SendFeeCustomFieldConverter(
        clickIntents = clickIntents,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        normalFee = fees.normal,
    )

    private val feeConverter = FeeConverter(
        clickIntents = clickIntents,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        fees = fees,
    )

    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState
        val feeSelectorUM = state.feeSelectorUM as? FeeSelectorUM.Content

        val updatedFeeSelector = if (feeSelectorUM == null) {
            FeeSelectorUM.Content(
                fees = fees,
                customValues = customFeeFieldConverter.convert(fees.normal),
                selectedFee = fees.normal,
                nonce = prevState.nonce,
            )
        } else {
            FeeSelectorUM.Content(
                fees = fees,
                customValues = feeSelectorUM.customValues,
                selectedType = feeSelectorUM.selectedType,
                selectedFee = feeConverter.convert(
                    FeeConverter.Data(
                        feeType = feeSelectorUM.selectedType,
                        customValues = feeSelectorUM.customValues,
                    ),
                ),
                nonce = prevState.nonce,
            )
        }

        return state.copy(
            feeSelectorUM = updatedFeeSelector,
            isFeeApproximate = isFeeApproximate,
            displayNonceInput = fees.normal is Fee.Ethereum,
            nonce = prevState.nonce,
        )
    }
}