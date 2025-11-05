package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.inputrow.inner.InputRowAsyncImage
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.StakingSendDetailsScreenTestTags
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.utils.getRewardTypeShortText
import com.tangem.features.staking.impl.presentation.ui.ValidatorImagePlaceholder
import com.tangem.utils.extensions.orZero

@Composable
internal fun ValidatorBlock(validatorState: StakingStates.ValidatorState, isClickable: Boolean, onClick: () -> Unit) {
    val state = validatorState as? StakingStates.ValidatorState.Data ?: return
    if (!state.isVisibleOnConfirmation) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                enabled = state.isClickable && isClickable,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .testTag(StakingSendDetailsScreenTestTags.VALIDATOR_BLOCK),
    ) {
        Text(
            text = stringResourceSafe(R.string.staking_validator),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            InputRowAsyncImage(
                imageUrl = validatorState.chosenValidator.image.orEmpty(),
                onImageError = { ValidatorImagePlaceholder() },
                modifier = Modifier
                    .size(24.dp)
                    .clip(TangemTheme.shapes.roundedCornersXLarge),
            )
            Text(
                text = validatorState.chosenValidator.name,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerWMax()
            Text(
                text = validatorState.getInfoTitleNeutral().resolveReference(),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

/**
 * For FCA fixes remove coloring for now
 */
@Suppress("UnusedPrivateMember")
@Composable
private fun StakingStates.ValidatorState.Data.getInfoTitleColored() = combinedReference(
    annotatedReference {
        append(getRewardTypeShortText(chosenValidator.rewardInfo?.type ?: Yield.RewardType.UNKNOWN).resolveReference())
        appendSpace()
        appendColored(
            text = chosenValidator.rewardInfo?.rate.orZero().format { percent() },
            color = TangemTheme.colors.text.accent,
        )
    },
)

private fun StakingStates.ValidatorState.Data.getInfoTitleNeutral() = combinedReference(
    getRewardTypeShortText(chosenValidator.rewardInfo?.type ?: Yield.RewardType.UNKNOWN),
    stringReference(" " + chosenValidator.rewardInfo?.rate?.orZero().format { percent() }),
)