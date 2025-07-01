package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeFiatRateUM
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.model.FeeSelectorIntents
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
internal class FeeSelectorLoadedTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val fees: TransactionFee,
    private val suggestedFeeState: FeeSelectorParams.SuggestedFeeState,
    private val isFeeApproximate: Boolean,
    private val feeSelectorIntents: FeeSelectorIntents,
) : Transformer<FeeSelectorUM> {

    private val feeItemsConverter = FeeItemConverter(
        suggestedFeeState = suggestedFeeState,
        normalFee = fees.normal,
        feeSelectorIntents = feeSelectorIntents,
        appCurrency = appCurrency,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
    )

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        val feeItems: ImmutableList<FeeItem> = feeItemsConverter.convert(fees)

        val selectedFee = when (prevState) {
            is FeeSelectorUM.Content -> feeItems.first { it.isSame(prevState.selectedFeeItem) }
            is FeeSelectorUM.Error,
            FeeSelectorUM.Loading,
            -> feeItems.find { it is FeeItem.Suggested } ?: feeItems.first { it is FeeItem.Market }
        }

        return FeeSelectorUM.Content(
            feeItems = feeItems,
            selectedFeeItem = selectedFee,
            isFeeApproximate = isFeeApproximate,
            feeFiatRateUM = cryptoCurrencyStatus.value.fiatRate?.let { rate ->
                FeeFiatRateUM(
                    rate = rate,
                    appCurrency = appCurrency,
                )
            },
            displayNonceInput = false,
            nonce = null,
            onNonceChange = {},
        )
    }
}