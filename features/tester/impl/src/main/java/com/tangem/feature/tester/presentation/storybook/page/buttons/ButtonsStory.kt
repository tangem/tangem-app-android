@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.*
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme

private const val STATE_LABEL_WIDTH = 80

@Suppress("LongMethod")
@Composable
internal fun ButtonsStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("primary") {
            ButtonSection(title = "Primary") { isEnabled, text, shape ->
                PrimaryTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
        item("secondary") {
            ButtonSection(title = "Secondary") { isEnabled, text, shape ->
                SecondaryTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
        item("primary_inverse") {
            ButtonSection(
                title = "PrimaryInverse",
                background = TangemTheme.colors2.surface.level2,
            ) { isEnabled, text, shape ->
                PrimaryInverseTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
        item("outline") {
            ButtonSection(title = "Outline") { isEnabled, text, shape ->
                OutlineTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
        item("accent") {
            ButtonSection(title = "Accent") { isEnabled, text, shape ->
                StatusTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
        item("positive") {
            ButtonSection(title = "Positive") { isEnabled, text, shape ->
                StatusTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    type = TangemButtonType.Positive,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
        item("ghost") {
            ButtonSection(title = "Ghost") { isEnabled, text, shape ->
                GhostTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    iconRes = R.drawable.ic_tangem_24,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    shape = shape,
                )
            }
        }
    }
}

@Composable
private fun ButtonSection(
    title: String,
    background: Color = TangemTheme.colors2.surface.level1,
    shapes: List<TangemButtonShape> = TangemButtonShape.entries,
    button: @Composable (isEnabled: Boolean, text: Boolean, shape: TangemButtonShape) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        shapes.forEach { shape ->
            ShapeGroup(shape = shape, button = button)
        }
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ShapeGroup(
    shape: TangemButtonShape,
    button: @Composable (isEnabled: Boolean, text: Boolean, shape: TangemButtonShape) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = shape.name,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        ColumnHeaderRow()
        StateRow(isEnabled = true, shape = shape, button = button)
        StateRow(isEnabled = false, shape = shape, button = button)
    }
}

@Composable
private fun ColumnHeaderRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(STATE_LABEL_WIDTH.dp))
        Text(
            text = "Text + Icon",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Icon only",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StateRow(
    isEnabled: Boolean,
    shape: TangemButtonShape,
    button: @Composable (isEnabled: Boolean, text: Boolean, shape: TangemButtonShape) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isEnabled) "Enabled" else "Disabled",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.width(STATE_LABEL_WIDTH.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            button(isEnabled, true, shape)
        }
        Box(modifier = Modifier.weight(1f)) {
            button(isEnabled, false, shape)
        }
    }
}