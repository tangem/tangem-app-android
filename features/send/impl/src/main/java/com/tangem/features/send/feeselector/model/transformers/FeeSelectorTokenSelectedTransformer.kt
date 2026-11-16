package com.tangem.features.send.feeselector.model.transformers

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

class FeeSelectorTokenSelectedTransformer(
    private val selectedToken: CryptoCurrencyStatus,
    private val isSponsoredFee: Boolean = false,
) : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        return if (prevState is FeeSelectorUM.Content) {
            prevState.copy(
                isPrimaryButtonEnabled = false,
                selectedFeeItem = FeeItem.Loading,
                feeItems = persistentListOf(FeeItem.Loading),
                feeExtraInfo = prevState.feeExtraInfo.copy(
                    feeCryptoCurrencyStatus = selectedToken,
                    isNotEnoughFunds = false,
                    isFeeTokenSelectedByUser = true,
                    sponsoredFee = prevState.feeExtraInfo.sponsoredFee.copy(
                        isSelected = isSponsoredFee,
                        isSmartGasPickedByUser = isSponsoredFee,
                    ),
                ),
            )
        } else {
            prevState
        }
    }
}