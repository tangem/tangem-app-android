package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class TokenStakingStateConverter(
    private val clickIntents: TokenDetailsClickIntents,
    private val currentStateProvider: Provider<TokenDetailsState>,
) : Converter<Either<StakingError, StakingEntryInfo>, StakingBlockUM> {

    override fun convert(value: Either<StakingError, StakingEntryInfo>): StakingBlockUM {
        val state = currentStateProvider()
        if (state.stakingBlocksState is StakingBlockUM.Staked) return state.stakingBlocksState

        val iconState = state.tokenInfoBlockState.iconState
        return value.fold(
            ifLeft = {
                StakingBlockUM.Error(iconState = iconState)
            },
            ifRight = {
                StakingBlockUM.StakeAvailable(
                    interestRate = BigDecimalFormatter.formatPercent(
                        percent = it.interestRate,
                        useAbsoluteValue = true,
                    ),
                    periodInDays = it.periodInDays,
                    tokenSymbol = it.tokenSymbol,
                    iconState = iconState,
                    onStakeClicked = clickIntents::onStakeBannerClick,
                )
            },
        )
    }
}
