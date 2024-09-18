package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.feature.tokendetails.presentation.tokendetails.state.StakingBlockUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
import com.tangem.features.tokendetails.impl.R
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
            ifRight = { stakingEntryInfo ->
                val apr = BigDecimalFormatter.formatPercent(
                    percent = stakingEntryInfo.apr,
                    useAbsoluteValue = true,
                )
                StakingBlockUM.StakeAvailable(
                    titleText = resourceReference(
                        id = R.string.token_details_staking_block_title,
                        formatArgs = wrappedList(apr),
                    ),
                    subtitleText = resourceReference(
                        id = R.string.staking_notification_earn_rewards_text,
                        formatArgs = wrappedList(stakingEntryInfo.tokenSymbol),
                    ),
                    iconState = iconState,
                    onStakeClicked = clickIntents::onStakeBannerClick,
                )
            },
        )
    }
}