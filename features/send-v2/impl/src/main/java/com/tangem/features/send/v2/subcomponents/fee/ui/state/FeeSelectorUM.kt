package com.tangem.features.send.v2.subcomponents.fee.ui.state

import androidx.compose.runtime.Stable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.transaction.error.GetFeeError
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigInteger

@Stable
internal sealed class FeeSelectorUM {

    data class Content(
        val fees: TransactionFee,
        val selectedType: FeeType = FeeType.Market,
        val selectedFee: Fee?,
        val customValues: ImmutableList<CustomFeeFieldUM> = persistentListOf(),
        val nonce: BigInteger? = null,
    ) : FeeSelectorUM()

    data object Loading : FeeSelectorUM()

    data class Error(
        val error: GetFeeError,
    ) : FeeSelectorUM()
}

internal enum class FeeType {
    Slow,
    Market,
    Fast,
    Custom,
    ;

    fun toAnalyticType(feeSelectorUM: FeeSelectorUM.Content): AnalyticsParam.FeeType = when (feeSelectorUM.fees) {
        is TransactionFee.Single -> AnalyticsParam.FeeType.Fixed
        is TransactionFee.Choosable -> when (feeSelectorUM.selectedType) {
            Slow -> AnalyticsParam.FeeType.Min
            Market -> AnalyticsParam.FeeType.Normal
            Fast -> AnalyticsParam.FeeType.Max
            Custom -> AnalyticsParam.FeeType.Custom
        }
    }
}