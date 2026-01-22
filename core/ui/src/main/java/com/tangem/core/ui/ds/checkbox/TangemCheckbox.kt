package com.tangem.core.ui.ds.checkbox

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Tangem checkbox component.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8437-82586&m=dev)
 *
 * @param isChecked         Boolean value representing the current state of the checkbox.
 * @param onCheckedChange   Lambda function to be invoked when the checkbox state changes.
 * @param modifier          Modifier to be applied to the badge.
 * @param isRounded         Boolean value to determine if the checkbox has rounded corners or is circular.
 * @param isEnabled         Boolean value to determine if the checkbox is enabled or disabled.
 */
@Composable
fun TangemCheckbox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isRounded: Boolean = true,
    isEnabled: Boolean = true,
) {
    val shape = if (isRounded) {
        RoundedCornerShape(TangemTheme.dimens2.x1)
    } else {
        CircleShape
    }
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x5)
            .then(
                if (isChecked) {
                    Modifier.background(
                        color = TangemTheme.colors2.border.status.accent,
                        shape = shape,
                    )
                } else {
                    Modifier.border(
                        color = TangemTheme.colors2.border.neutral.secondary,
                        shape = shape,
                        width = 1.dp,
                    )
                },
            )
            .clip(shape = shape)
            .toggleable(
                value = isChecked,
                onValueChange = onCheckedChange,
                enabled = isEnabled,
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            ),
    ) {
        if (isChecked) {
            Icon(
                painter = rememberVectorPainter(ImageVector.vectorResource(R.drawable.ic_check_default_24)),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(TangemTheme.dimens2.x4),
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemCheckbox_Preview() {
    val previewData = mutableListOf(
        TangemCheckboxPreviewData(isChecked = false, isRounded = true),
        TangemCheckboxPreviewData(isChecked = false, isRounded = false),
        TangemCheckboxPreviewData(isChecked = true, isRounded = true),
        TangemCheckboxPreviewData(isChecked = true, isRounded = false),
    )
    TangemThemePreviewRedesign {
        Row(
            modifier = Modifier.background(TangemTheme.colors2.surface.level1),
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x6),
        ) {
            previewData.fastForEach { data ->
                TangemCheckbox(
                    isChecked = data.isChecked,
                    isRounded = data.isRounded,
                    onCheckedChange = { },
                    modifier = Modifier,
                )
            }
        }
    }
}

private data class TangemCheckboxPreviewData(
    val isChecked: Boolean,
    val isRounded: Boolean,
)
// endregion