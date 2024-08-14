package com.tangem.features.staking.impl.presentation.ui.block

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.tangem.core.ui.components.inputrow.InputRowImageInfo
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.utils.extensions.orZero

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
            ),
    ) {
        if (validatorState is ValidatorState.Content) {
            InputRowImageInfo(
                title = resourceReference(R.string.staking_validator),
                subtitle = stringReference(validatorState.chosenValidator.name),
                infoTitle = annotatedReference {
                    append(resourceReference(R.string.staking_details_apr).resolveReference())
                    appendSpace()
                    appendColored(
                        text = BigDecimalFormatter.formatPercent(validatorState.chosenValidator.apr.orZero(), true),
                        color = TangemTheme.colors.text.accent,
                    )
                },
                imageUrl = validatorState.chosenValidator.image.orEmpty(),
            )
        }
    }
}