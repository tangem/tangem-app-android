package com.tangem.features.staking.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.inputrow.InputRowImageSelector
import com.tangem.core.ui.components.rows.CornersToRound
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import com.tangem.features.staking.impl.presentation.viewmodel.StakingClickIntents
import java.math.BigDecimal

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
        item(key = "HEADER") {
            Text(
                text = stringResource(R.string.staking_validator),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CornersToRound.TOP_2.getShape())
                    .background(TangemTheme.colors.background.action)
                    .padding(
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                        top = TangemTheme.dimens.spacing12,
                        bottom = TangemTheme.dimens.spacing8,
                    ),
            )
        }
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
                        annotatedReference(
                            buildAnnotatedString {
                                append(" ")
                                withStyle(style = SpanStyle(color = TangemTheme.colors.text.accent)) {
                                    append(
                                        BigDecimalFormatter.formatPercent(item.apr ?: BigDecimal.ZERO, true),
                                    )
                                }
                            },
                        ),
                    ),
                    imageUrl = item.image.orEmpty(),
                    isSelected = item == state.chosenValidator,
                    onSelect = { clickIntents.onValidatorSelect(item) },
                    modifier = Modifier
                        .clip(
                            if (index == validators.lastIndex) {
                                CornersToRound.BOTTOM_2
                            } else {
                                CornersToRound.ZERO
                            }.getShape(),
                        )
                        .background(TangemTheme.colors.background.action),
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