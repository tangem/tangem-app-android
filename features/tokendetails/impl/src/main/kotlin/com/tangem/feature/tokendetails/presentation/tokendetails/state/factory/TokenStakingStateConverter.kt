package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenStakingStateConverter(
    private val currentStateProvider: Provider<TokenDetailsState>,
) : Converter<Either<Throwable, StakingEntryInfo>, StakingBlockState> {

    override fun convert(value: Either<Throwable, StakingEntryInfo>): StakingBlockState {
        value.fold(
            ifLeft = {
                return StakingBlockState.Error(
                    iconState = currentStateProvider().tokenInfoBlockState.iconState,
                )
            },
            ifRight = {
                return StakingBlockState.Content(
                    percent = it.percent,
                    periodInDays = it.periodInDays,
                    tokenSymbol = it.tokenSymbol,
                    iconState = currentStateProvider().tokenInfoBlockState.iconState,
                )
            },
        )
    }
}
