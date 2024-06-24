package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingAvailable
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBalance
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlocksState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenStakingStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val currentStateProvider: Provider<TokenDetailsState>,
) : Converter<Either<Throwable, StakingEntryInfo>, StakingBlocksState> {

    override fun convert(value: Either<Throwable, StakingEntryInfo>): StakingBlocksState {
        value.fold(
            ifLeft = {
                return StakingBlocksState(
                    stakingAvailable = StakingAvailable.Error(
                        iconState = currentStateProvider().tokenInfoBlockState.iconState,
                    ),
                    stakingBalance = StakingBalance.Empty,
                )
            },
            ifRight = {
                return StakingBlocksState(
                    stakingAvailable = StakingAvailable.Content(
                        interestRate = BigDecimalFormatter.formatPercent(
                            percent = it.interestRate,
                            useAbsoluteValue = true,
                        ),
                        periodInDays = it.periodInDays,
                        tokenSymbol = it.tokenSymbol,
                        iconState = currentStateProvider().tokenInfoBlockState.iconState,
                        onStakeClicked = clickIntents::onStakeBannerClick,
                    ),
                    stakingBalance = StakingBalance.Empty,
                )
            },
        )
    }
}