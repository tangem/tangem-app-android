package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.previewdata.ValidatorStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.state.utils.getRewardTypeLongText
import com.tangem.features.staking.impl.presentation.ui.utils.toImageReference
import com.tangem.utils.extensions.orZero

/**
 * Staking screen with validators
 */
@Composable
internal fun StakingValidatorListContent(
    state: StakingStates.ValidatorState,
    clickIntents: StakingClickIntents,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomBarHeight),
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        if (state is StakingStates.ValidatorState.Data) {
            val targets = state.availableTargets
            items(
                count = targets.size,
                key = { targets[it].address },
                contentType = { targets[it]::class.java },
            ) { index ->
                ValidatorListItem(
                    item = targets[index],
                    isSelected = targets[index] == state.chosenTarget,
                    index = index,
                    lastIndex = targets.lastIndex,
                    onSelect = { clickIntents.onTargetSelect(targets[index]) },
                )
            }
        }
    }
}

@Composable
private fun ValidatorListItem(
    item: StakingTarget,
    isSelected: Boolean,
    index: Int,
    lastIndex: Int,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .roundedShapeItemDecoration(
                currentIndex = index,
                lastIndex = lastIndex,
                radius = TangemTheme.dimens.radius12,
                addDefaultPadding = false,
            )
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onSelect,
            )
            .padding(TangemTheme.dimens.spacing12),
    ) {
        StakingTargetIcon(
            image = item.image.toImageReference(),
            onImageError = { ValidatorImagePlaceholder() },
            modifier = Modifier
                .size(TangemTheme.dimens.spacing36)
                .clip(TangemTheme.shapes.roundedCornersXLarge),
        )
        SpacerW12()
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = stringReference(item.name).resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                ValidatorLabel(item.isStrategicPartner)
            }
            Text(
                text = item.getAprTextNeutral().resolveAnnotatedReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing2),
            )
        }
        SpacerW12()
        AnimatedVisibility(visible = isSelected) {
            Icon(
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(id = R.drawable.ic_check_24),
                ),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
            )
        }
    }
}

/**
 * For FCA fixes remove coloring for now
 */
@Suppress("UnusedPrivateMember")
@Composable
private fun StakingTarget.getAprTextColored() = combinedReference(
    getRewardTypeLongText(rewardInfo?.type ?: RewardType.UNKNOWN),
    annotatedReference {
        appendSpace()
        appendColored(
            text = rewardInfo?.rate.orZero().format { percent() },
            color = TangemTheme.colors.text.accent,
        )
    },
)

@Composable
private fun StakingTarget.getAprTextNeutral() = combinedReference(
    getRewardTypeLongText(rewardInfo?.type ?: RewardType.UNKNOWN),
    stringReference(" " + rewardInfo?.rate.orZero().format { percent() }),
)

@Composable
private fun RowScope.ValidatorLabel(isStrategicPartner: Boolean) {
    if (isStrategicPartner) {
        Text(
            text = stringResourceSafe(R.string.staking_validators_label),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.icon.constant,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(TangemTheme.colors.text.accent)
                .padding(horizontal = 8.dp),
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingValidatorListContent_Preview(
    @PreviewParameter(StakingValidatorListContentPreviewProvider::class)
    data: StakingStates.ValidatorState,
) {
    TangemThemePreview {
        StakingValidatorListContent(
            state = data,
            clickIntents = StakingClickIntentsStub,
        )
    }
}

private class StakingValidatorListContentPreviewProvider : PreviewParameterProvider<StakingStates.ValidatorState> {
    override val values: Sequence<StakingStates.ValidatorState>
        get() = sequenceOf(ValidatorStatePreviewData.validatorState)
}
// endregion