package com.tangem.features.send.v2.feeselector.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.subcomponents.fee.ui.state.CustomFeeFieldUM
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.math.BigInteger

@Immutable
internal sealed class FeeSelectorUM {

    abstract val doneButtonConfig: PrimaryButtonConfig

    data class Loading(private val onDone: () -> Unit) : FeeSelectorUM() {
        override val doneButtonConfig = PrimaryButtonConfig(enabled = false, onClick = onDone)
    }

    data class Error(val error: GetFeeError, private val onDone: () -> Unit) : FeeSelectorUM() {
        override val doneButtonConfig = PrimaryButtonConfig(enabled = false, onClick = onDone)
    }

    data class Content(
        override val doneButtonConfig: PrimaryButtonConfig,
        val feeItems: ImmutableList<FeeItem>,
        val onFeeSelected: (FeeItem) -> Unit,
        val selectedFeeItem: FeeItem,
        val isFeeApproximate: Boolean,
        val feeFiatRateUM: FeeFiatRateUM?,
        val displayNonceInput: Boolean,
        val nonce: BigInteger?,
        val onNonceChange: (String) -> Unit,
    ) : FeeSelectorUM()
}

internal data class PrimaryButtonConfig(val enabled: Boolean, val onClick: () -> Unit)

@Immutable
internal data class FeeFiatRateUM(
    val rate: BigDecimal,
    val appCurrency: AppCurrency,
)

@Immutable
internal sealed class FeeItem {
    abstract val fee: Fee

    data class Suggested(val title: TextReference, override val fee: Fee) : FeeItem()
    data class Slow(override val fee: Fee) : FeeItem()
    data class Market(override val fee: Fee) : FeeItem()
    data class Fast(override val fee: Fee) : FeeItem()
    data class Custom(override val fee: Fee, val customValues: ImmutableList<CustomFeeFieldUM>) : FeeItem()
}