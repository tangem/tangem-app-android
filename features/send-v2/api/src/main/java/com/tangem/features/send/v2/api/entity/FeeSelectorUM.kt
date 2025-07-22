package com.tangem.features.send.v2.api.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.error.GetFeeError
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.math.BigInteger

@Immutable
sealed class FeeSelectorUM {

    data object Loading : FeeSelectorUM()

    data class Error(val error: GetFeeError) : FeeSelectorUM()

    data class Content(
        val feeItems: ImmutableList<FeeItem>,
        val selectedFeeItem: FeeItem,
        val isFeeApproximate: Boolean,
        val feeFiatRateUM: FeeFiatRateUM?,
        val displayNonceInput: Boolean,
        val nonce: BigInteger?,
        val onNonceChange: (String) -> Unit,
    ) : FeeSelectorUM()
}

@Immutable
data class FeeFiatRateUM(
    val rate: BigDecimal,
    val appCurrency: AppCurrency,
)

@Immutable
sealed class FeeItem {
    abstract val fee: Fee

    fun isSame(other: FeeItem): Boolean {
        return this::class == other::class
    }

    data class Suggested(val title: TextReference, override val fee: Fee) : FeeItem()
    data class Slow(override val fee: Fee) : FeeItem()
    data class Market(override val fee: Fee) : FeeItem()
    data class Fast(override val fee: Fee) : FeeItem()
    data class Custom(override val fee: Fee, val customValues: ImmutableList<CustomFeeFieldUM>) : FeeItem()
}