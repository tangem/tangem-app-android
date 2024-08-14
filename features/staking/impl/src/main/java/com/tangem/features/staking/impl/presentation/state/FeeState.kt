package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

@Immutable
sealed class FeeState {

    data class Content(
        val fee: Fee?,
        val rate: BigDecimal?,
        val isFeeConvertibleToFiat: Boolean,
        val appCurrency: AppCurrency,
        val isFeeApproximate: Boolean,
    ) : FeeState()

    data object Loading : FeeState()

    data object Error : FeeState()
}
