package com.tangem.features.send.v2.api.entity

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.R
import com.tangem.features.send.v2.api.entity.FeeItem.*
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
    ) : FeeSelectorUM() {
        fun toAnalyticType(): AnalyticsParam.FeeType = when (fees) {
            is TransactionFee.Single -> AnalyticsParam.FeeType.Fixed
            is TransactionFee.Choosable -> when (selectedFeeItem) {
                is Suggested,
                is Custom,
                -> AnalyticsParam.FeeType.Custom
                is Fast -> AnalyticsParam.FeeType.Max
                is Market -> AnalyticsParam.FeeType.Normal
                is Slow -> AnalyticsParam.FeeType.Min
            }
        }
    }
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
    abstract val title: TextReference
    abstract val iconRes: Int

    fun isSameClass(other: FeeItem): Boolean {
        return this::class == other::class
    }

    data class Suggested(
        override val title: TextReference,
        override val fee: Fee,
    ) : FeeItem() {
        override val iconRes: Int = R.drawable.ic_star_mini_24
    }

    data class Slow(override val fee: Fee) : FeeItem() {
        override val title: TextReference = resourceReference(R.string.common_fee_selector_option_slow)
        override val iconRes: Int = R.drawable.ic_tortoise_24
    }

    data class Market(override val fee: Fee) : FeeItem() {
        override val title: TextReference = resourceReference(R.string.common_fee_selector_option_market)
        override val iconRes: Int = R.drawable.ic_bird_24
    }

    data class Fast(override val fee: Fee) : FeeItem() {
        override val title: TextReference = resourceReference(R.string.common_fee_selector_option_fast)
        override val iconRes: Int = R.drawable.ic_hare_24
    }

    data class Custom(override val fee: Fee, val customValues: ImmutableList<CustomFeeFieldUM>) : FeeItem() {
        override val title: TextReference = resourceReference(R.string.common_custom)
        override val iconRes: Int = R.drawable.ic_edit_v2_24
    }
}