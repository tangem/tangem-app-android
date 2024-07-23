package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.components.inputrow.InputRowImageChevron
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import java.math.BigDecimal

@Composable
internal fun ValidatorBlock(validatorState: ValidatorState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                enabled = validatorState.isClickable,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
            )
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResource(R.string.staking_validator),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing6),
        )
        if (validatorState is ValidatorState.Content) {
            InputRowImageChevron(
                subtitle = stringReference(validatorState.chosenValidator.name),
                caption = combinedReference(
                    resourceReference(R.string.staking_details_apr),
                    annotatedReference(
                        buildAnnotatedString {
                            append(" ")
                            withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
                                val apr = validatorState.chosenValidator.apr ?: BigDecimal.ZERO
                                append(BigDecimalFormatter.formatPercent(apr, true))
                            }
                        },
                    ),
                ),
                imageUrl = validatorState.chosenValidator.image.orEmpty(),
                showChevron = validatorState.isClickable,
            )
        }
    }
}