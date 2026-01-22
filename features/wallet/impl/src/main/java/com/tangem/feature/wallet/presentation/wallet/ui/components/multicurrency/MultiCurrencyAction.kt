package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.buttons.actions.ActionBaseButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.ActionButtonContent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun MultiCurrencyAction(config: ActionButtonConfig, modifier: Modifier = Modifier) {
    ActionBaseButton(
        config = config,
        shape = RoundedCornerShape(size = TangemTheme.dimens.radius12),
        content = { contentModifier ->
            ActionButtonContent(
                config = config,
                text = { color ->
                    Text(
                        text = config.text.resolveReference(),
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 10.sp,
                            maxFontSize = TangemTheme.typography.button.fontSize,
                        ),
                        color = color,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = TangemTheme.typography.button,
                    )
                },
                modifier = contentModifier.padding(horizontal = 4.dp),
                paddingBetweenIconAndText = 4.dp,
            )
        },
        modifier = modifier,
        color = TangemTheme.colors.button.secondary,
    )
}