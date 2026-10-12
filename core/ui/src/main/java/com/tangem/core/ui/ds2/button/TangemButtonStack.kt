package com.tangem.core.ui.ds2.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 button stack — secondary actions grouped side by side in a translucent material
 * pill. Used as the secondary-actions slot of [com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9434-14086)
 *
 * ```
 * TangemButtonStack {
 *     TangemButton.GroupEntry(iconUM = search, onClick = onSearch)
 *     TangemButton.GroupEntry(iconUM = more, onClick = onMore)
 * }
 * ```
 *
 * @param modifier Modifier applied to the stack.
 * @param variant Container style behind the entries. See [TangemButtonStack.Variant].
 * @param content Entries, in display order. Expected to emit `Ghost` [TangemButton]s of size `X9` —
 *   see [TangemButton.GroupEntry] for the icon-only preset. Laid out in a `Row`, so `RowScope`
 *   modifiers are available.
 */
@Composable
fun TangemButtonStack(
    modifier: Modifier = Modifier,
    variant: TangemButtonStack.Variant = TangemButtonStack.Variant.Material,
    content: @Composable RowScope.() -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        color = Color.Transparent,
        isMaterial = variant == TangemButtonStack.Variant.Material,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(all = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

object TangemButtonStack {

    /**
     * Container style behind the entries.
     *
     * - [Material] — the design default: a translucent material pill with a full corner radius.
     * - [Ghost] — no container at all: the entries alone, without the pill. For surfaces that
     *   already separate the actions from the content behind them.
     */
    enum class Variant {
        Material,
        Ghost,
    }
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemButtonStackPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        ) {
            val icon = TangemIconUM.Icon(iconRes = R.drawable.ic_information_24)

            PreviewSection(label = "Material — icons") {
                TangemButtonStack {
                    repeat(times = 3) {
                        TangemButton.GroupEntry(iconUM = icon, onClick = {})
                    }
                }
            }
            PreviewSection(label = "Material — label") {
                TangemButtonStack {
                    TangemButton(
                        variant = TangemButton.Variant.Ghost,
                        size = TangemButton.Size.X9,
                        text = stringReference(value = "Label"),
                        onClick = {},
                    )
                }
            }
            PreviewSection(label = "Material — icon + label") {
                TangemButtonStack {
                    TangemButton(
                        variant = TangemButton.Variant.Ghost,
                        size = TangemButton.Size.X9,
                        iconStart = icon,
                        text = stringReference(value = "Label"),
                        onClick = {},
                    )
                }
            }
            PreviewSection(label = "Ghost") {
                TangemButtonStack(variant = TangemButtonStack.Variant.Ghost) {
                    repeat(times = 3) {
                        TangemButton.GroupEntry(iconUM = icon, onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(
            text = label,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        content()
    }
}

// endregion