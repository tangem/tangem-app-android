package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.entity.FeeFiatRateUM
import com.tangem.features.send.v2.feeselector.entity.FeeItem
import com.tangem.features.send.v2.feeselector.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
internal class FeeSelectorLoadedTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val fees: TransactionFee,
    private val suggestedFeeState: FeeSelectorParams.SuggestedFeeState,
    private val isFeeApproximate: Boolean,
    private val onFeeSelected: (FeeItem) -> Unit,
    private val onCustomFeeValueChange: (Int, String) -> Unit,
    private val onNextClick: () -> Unit,
) : Transformer<FeeSelectorUM> {

    private val feeItemsConverter = FeeItemConverter(
        suggestedFeeState = suggestedFeeState,
        normalFee = fees.normal,
        onCustomFeeValueChange = onCustomFeeValueChange,
        onNextClick = onNextClick,
        appCurrency = appCurrency,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
    )

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        val feeItems: ImmutableList<FeeItem> = feeItemsConverter.convert(fees)
        val selectedFee = feeItems.find { it is FeeItem.Suggested } ?: feeItems.first { it is FeeItem.Market }
        return FeeSelectorUM.Content(
            doneButtonConfig = prevState.doneButtonConfig.copy(enabled = true),
            feeItems = feeItems,
            selectedFeeItem = selectedFee,
            isFeeApproximate = isFeeApproximate,
            onFeeSelected = onFeeSelected,
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