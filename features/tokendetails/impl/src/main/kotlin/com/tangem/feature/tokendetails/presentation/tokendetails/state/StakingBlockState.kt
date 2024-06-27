package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface StakingBlockState {

    val iconState: IconState

    data class Error(override val iconState: IconState) : StakingBlockState

    data class Loading(override val iconState: IconState) : StakingBlockState

    data class Content(
        override val iconState: IconState,
        val interestRate: String,
        val periodInDays: Int,
        val tokenSymbol: String,
    ) : StakingBlockState
}