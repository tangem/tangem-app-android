package com.tangem.core.ui.ds.button.action

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.orEmpty
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Action buttons row
 *
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=9467-37845&m=dev)
 *
 * @param buttons list of buttons
 * @param modifier modifier
 */
@Composable
fun ActionButtons(buttons: ImmutableList<TangemButtonUM>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        buttons.forEachIndexed { index, button ->
            key(button.text to index) {
                val textColor = if (button.isEnabled) {
                    TangemTheme.colors2.text.neutral.primary
                } else {
                    TangemTheme.colors2.text.status.disabled
                }
                Column(
                    modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x2_5),
                    verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SecondaryTangemButton(
                        tangemIconUM = button.tangemIconUM,
                        onClick = button.onClick,
                        isEnabled = button.isEnabled,
                        shape = TangemButtonShape.Rounded,
                    )
                    Text(
                        text = button.text.orEmpty().resolveReference(),
                        style = TangemTheme.typography2.calloutSemibold15,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ActionButtons_Preview(
    @PreviewParameter(ActionButtonsPreviewProvider::class) params: ImmutableList<TangemButtonUM>,
) {
    TangemThemePreviewRedesign {
        ActionButtons(
            buttons = params,
        )
    }
}

private class ActionButtonsPreviewProvider : PreviewParameterProvider<ImmutableList<TangemButtonUM>> {
    override val values: Sequence<ImmutableList<TangemButtonUM>>
        get() = sequenceOf(
            persistentListOf(
                TangemButtonUM(
                    text = stringReference("Send"),
                    tangemIconUM = previewIcon(R.drawable.ic_arrow_up_24, isEnabled = true),
                    onClick = { },
                    isEnabled = true,
                    type = TangemButtonType.Secondary,
                ),
                TangemButtonUM(
                    text = stringReference("Receive"),
                    tangemIconUM = previewIcon(R.drawable.ic_exchange_default_24, isEnabled = true),
                    onClick = { },
                    isEnabled = true,
                    type = TangemButtonType.Secondary,
                ),
                TangemButtonUM(
                    text = stringReference("Swap"),
                    tangemIconUM = previewIcon(R.drawable.ic_dollar_default_24, isEnabled = false),
                    onClick = { },
                    isEnabled = false,
                    type = TangemButtonType.Secondary,
                ),
            ),
        )
}

private fun previewIcon(iconRes: Int, isEnabled: Boolean): TangemIconUM = TangemIconUM.Icon(
    iconRes = iconRes,
    tintReference = {
        if (isEnabled) {
            TangemTheme.colors2.graphic.neutral.primary
        } else {
            TangemTheme.colors2.graphic.neutral.quaternary
        }
    },
)
// endregion