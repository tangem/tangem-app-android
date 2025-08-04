package com.tangem.features.send.v2.api.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.error.GetFeeError
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.math.BigInteger

@Immutable
sealed class FeeSelectorUM {

    abstract val isPrimaryButtonEnabled: Boolean

    data object Loading : FeeSelectorUM() {
        override val isPrimaryButtonEnabled = false
    }

    data class Error(val error: GetFeeError) : FeeSelectorUM() {
        override val isPrimaryButtonEnabled = false
    }

    data class Content(
        override val isPrimaryButtonEnabled: Boolean,
        val fees: TransactionFee,
        val feeItems: ImmutableList<FeeItem>,
        val selectedFeeItem: FeeItem,
        val feeExtraInfo: FeeExtraInfo,
        val feeFiatRateUM: FeeFiatRateUM?,
        val feeNonce: FeeNonce,
    ) : FeeSelectorUM()
}

@Immutable
data class FeeFiatRateUM(
    val rate: BigDecimal,
    val appCurrency: AppCurrency,
)

@Immutable
data class FeeExtraInfo(
    val isFeeApproximate: Boolean,
    val isFeeConvertibleToFiat: Boolean,
    val isTronToken: Boolean,
)

sealed class FeeNonce {
    data object None : FeeNonce()
    data class Nonce(
        val nonce: BigInteger?,
        val onNonceChange: (String) -> Unit,
    ) : FeeNonce()
}

@Immutable
sealed class FeeItem {
    abstract val fee: Fee

    fun isSameClass(other: FeeItem): Boolean {
        return this::class == other::class
    }

    data class Suggested(val title: TextReference, override val fee: Fee) : FeeItem()
    data class Slow(override val fee: Fee) : FeeItem()
    data class Market(override val fee: Fee) : FeeItem()
    data class Fast(override val fee: Fee) : FeeItem()
    data class Custom(override val fee: Fee, val customValues: ImmutableList<CustomFeeFieldUM>) : FeeItem()
}