package com.tangem.features.tangempay.card.details

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface TangemPayDailyLimitBlockState {
    data object Loading : TangemPayDailyLimitBlockState

    data class Error(val onReloadClick: () -> Unit) : TangemPayDailyLimitBlockState

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