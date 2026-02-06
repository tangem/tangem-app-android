package com.tangem.core.ui.ds.tabs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Tangem tab component.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8448-74386&m=dev)
 *
 * @param text              TextReference representing the text to be displayed on the tab.
 * @param isChecked         Boolean value representing the current state of the tab.
 * @param onCheckedChange   Lambda function to be invoked when the tab state changes.
 * @param modifier          Modifier to be applied to the tab.
 * @param isEnabled         Boolean value to determine if the tab is enabled or disabled.
 */
@Composable
fun TangemTab(
    text: TextReference,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val shape = RoundedCornerShape(TangemTheme.dimens2.x25)
    val backgroundColor = if (isChecked) {
        TangemTheme.colors2.tabs.backgroundPrimary
    } else {
        TangemTheme.colors2.tabs.textPrimary
    }
    val textColor = if (isChecked) {
        TangemTheme.colors2.tabs.textPrimary
    } else {
        TangemTheme.colors2.tabs.textSecondary
    }
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = shape,
            )
            .clip(shape = shape)
            .padding(all = TangemTheme.dimens2.x3)
            .toggleable(
                value = isChecked,
                onValueChange = onCheckedChange,
                enabled = isEnabled,
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
    ) {
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography2.bodySemibold16,
            color = textColor,
            maxLines = 1,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemTab_Preview() {
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x6),
            modifier = Modifier.background(TangemTheme.colors2.surface.level1),
        ) {
            TangemTab(
                text = stringReference("Tab Title"),
                isChecked = true,
                onCheckedChange = {},
            )
            TangemTab(
                text = stringReference("Tab Title"),
                isChecked = false,
                onCheckedChange = {},
            )
        }
    }
}

// endregion