package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.inputrow.InputRowImageSelector
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import com.tangem.utils.extensions.orZero

/**
 * Staking screen with validators
 */
@Composable
internal fun StakingValidatorListContent(
    state: ValidatorState,
    clickIntents: StakingClickIntents,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomBarHeight),
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        if (state is ValidatorState.Content) {
            val validators = state.availableValidators
            items(
                count = validators.size,
                key = { validators[it].address },
                contentType = { validators[it]::class.java },
            ) { index ->
                val item = validators[index]

                InputRowImageSelector(
                    subtitle = stringReference(item.name),
                    caption = combinedReference(
                        resourceReference(R.string.staking_details_apr),
                        annotatedReference {
                            appendSpace()
                            appendColored(
                                text = BigDecimalFormatter.formatPercent(item.apr.orZero(), true),
                                color = TangemTheme.colors.text.accent,
                            )
                        },
                    ),
                    imageUrl = item.image.orEmpty(),
                    isSelected = item == state.chosenValidator,
                    onSelect = { clickIntents.onValidatorSelect(item) },
                    modifier = Modifier
                        .roundedShapeItemDecoration(
                            currentIndex = index,
                            lastIndex = validators.lastIndex,
                            radius = TangemTheme.dimens.radius12,
                            addDefaultPadding =
                            false,
                        )
                        .background(TangemTheme.colors.background.action),
                    selectorContent = { checked, _, _ ->
                        AnimatedVisibility(
                            modifier = Modifier,
                            visible = checked,
                        ) {
                            Icon(
                                painter = rememberVectorPainter(
                                    image = ImageVector.vectorResource(id = R.drawable.ic_check_24),
                                ),
                                tint = TangemTheme.colors.icon.accent,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StakingValidatorListContent_Preview(
    @PreviewParameter(StakingValidatorListContentPreviewProvider::class)
    data: ValidatorState,
) {
    TangemThemePreview {
        StakingValidatorListContent(
            state = data,
            clickIntents = StakingClickIntentsStub,
        )
    }
}

private class StakingValidatorListContentPreviewProvider : PreviewParameterProvider<ValidatorState> {
    override val values: Sequence<ValidatorState>
        get() = sequenceOf(ConfirmationStatePreviewData.assentStakingState.validatorState)
}
// endregion