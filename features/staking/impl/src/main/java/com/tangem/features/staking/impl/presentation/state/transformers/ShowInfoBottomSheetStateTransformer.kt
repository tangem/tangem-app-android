package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingInfoBottomSheetConfig
import com.tangem.utils.transformer.Transformer

internal class ShowInfoBottomSheetStateTransformer(
    private val infoType: InfoType,
    private val onDismiss: () -> Unit,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                onDismissRequest = onDismiss,
                isShow = true,
                content = when (infoType) {
                    InfoType.APY -> StakingInfoBottomSheetConfig(
                        title = resourceReference(R.string.staking_details_apy),
                        text = resourceReference(R.string.staking_details_apy_info),
                    )
                    InfoType.UNBONDING_PERIOD -> StakingInfoBottomSheetConfig(
                        title = resourceReference(R.string.staking_details_unbonding_period),
                        text = resourceReference(R.string.staking_details_unbonding_period_info),
                    )
                    InfoType.REWARD_CLAIMING -> StakingInfoBottomSheetConfig(
                        title = resourceReference(R.string.staking_details_reward_claiming),
                        text = resourceReference(R.string.staking_details_reward_claiming_info),
                    )
                    InfoType.WARMUP_PERIOD -> StakingInfoBottomSheetConfig(
                        title = resourceReference(R.string.staking_details_warmup_period),
                        text = resourceReference(R.string.staking_details_warmup_period_info),
                    )
                    InfoType.REWARD_SCHEDULE -> StakingInfoBottomSheetConfig(
                        title = resourceReference(R.string.staking_details_reward_schedule),
                        text = resourceReference(R.string.staking_details_reward_schedule_info),
                    )
                },
            ),
        )
    }
}

enum class InfoType {
    APY,
    UNBONDING_PERIOD,
    REWARD_CLAIMING,
    WARMUP_PERIOD,
    REWARD_SCHEDULE,
}
