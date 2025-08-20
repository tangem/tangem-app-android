package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.StakingSendDetailsScreenTestTags
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
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
        InputRowImageInfo(
            title = resourceReference(R.string.staking_validator),
            subtitle = stringReference(state.chosenValidator.name),
            infoTitle = state.getInfoTitleNeutral(),
            imageUrl = state.chosenValidator.image.orEmpty(),
            onImageError = { ValidatorImagePlaceholder() },
        )
    }
}

/**
 * For FCA fixes remove coloring for now
 */
@Suppress("UnusedPrivateMember")
@Composable
private fun StakingStates.ValidatorState.Data.getInfoTitleColored() = combinedReference(
    annotatedReference {
        append(resourceReference(R.string.staking_details_apr).resolveReference())
        appendSpace()
        appendColored(
            text = chosenValidator.apr.orZero().format { percent() },
            color = TangemTheme.colors.text.accent,
        )
    },
)

private fun StakingStates.ValidatorState.Data.getInfoTitleNeutral() = combinedReference(
    resourceReference(R.string.staking_details_apr),
    stringReference(" " + chosenValidator.apr.orZero().format { percent() }),
)