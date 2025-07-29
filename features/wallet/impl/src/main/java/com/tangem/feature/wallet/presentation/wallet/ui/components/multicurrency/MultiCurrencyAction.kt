package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.buttons.actions.ActionBaseButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.ActionButtonContent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.MainScreenTestTags

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun MultiCurrencyAction(
    config: ActionButtonConfig,
    fontSizeValue: TextUnit,
    fontSizeRange: FontSizeRange,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    ActionBaseButton(
        config = config,
        shape = RoundedCornerShape(size = TangemTheme.dimens.radius12),
        content = { contentModifier ->
            ActionButtonContent(
                config = config,
                text = { color ->
                    ResizableText(
                        text = config.text.resolveReference(),
                        fontSizeValue = fontSizeValue,
                        fontSizeRange = fontSizeRange,
                        onFontSizeChange = onFontSizeChange,
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
        modifier = modifier.testTag(MainScreenTestTags.MULTI_CURRENCY_ACTION_BUTTON),
        color = TangemTheme.colors.button.secondary,
    )
}