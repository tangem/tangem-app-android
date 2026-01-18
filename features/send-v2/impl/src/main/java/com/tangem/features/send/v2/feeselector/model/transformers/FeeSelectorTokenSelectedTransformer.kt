package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

class FeeSelectorTokenSelectedTransformer(
    private val selectedToken: CryptoCurrencyStatus,
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
                ),
            )
        } else {
            prevState
        }
    }
}