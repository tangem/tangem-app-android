package com.tangem.features.feeselector.impl.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.Amount
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.feeselector.api.entity.CustomFeeFieldUM
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

@Immutable
internal sealed class FeeSelectorUM {

    data object Loading : FeeSelectorUM()

    data class Content(
        val isDoneEnabled: Boolean,
        val feeItems: ImmutableList<FeeItem>,
        val selectedFeeItem: FeeItem,
        val isFeeApproximate: Boolean,
        val feeFiatRateDataHolder: FeeFiatRateDataHolder?,
    ) : FeeSelectorUM()
}

@Immutable
data class FeeFiatRateDataHolder(
    val rate: BigDecimal,
    val appCurrency: AppCurrency,
)

@Immutable
internal sealed class FeeItem {
    abstract val onSelect: () -> Unit

    data class Suggested(val title: TextReference, val amount: Amount, override val onSelect: () -> Unit) : FeeItem()
    data class Slow(val amount: Amount, override val onSelect: () -> Unit) : FeeItem()
    data class Market(val amount: Amount, override val onSelect: () -> Unit) : FeeItem()
    data class Fast(val amount: Amount, override val onSelect: () -> Unit) : FeeItem()
    data class Custom(val customValues: ImmutableList<CustomFeeFieldUM>, override val onSelect: () -> Unit) : FeeItem()
}