package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable

@Immutable
sealed interface StakingBlockState {

    data object Error : StakingBlockState

    data object Loading : StakingBlockState

    data class Content(
        val percent: String,
        val periodInDays: Int,
        val tokenSymbol: String
    ) : StakingBlockState
}
