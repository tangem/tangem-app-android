package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface TangemPayDailyLimitBlockState {
    data object Loading : TangemPayDailyLimitBlockState

    data object Error : TangemPayDailyLimitBlockState

    data class Content(
        val limit: String,
        val onChangeClick: () -> Unit,
    ) : TangemPayDailyLimitBlockState {
        companion object {
            fun stub() = Content(
                limit = "$5,000",
                onChangeClick = {},
            )
        }
    }
}