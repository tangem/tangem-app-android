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
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme

private const val STATE_LABEL_WIDTH = 80

@Suppress("LongMethod", "CyclomaticComplexMethod")
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
            ButtonSection(title = "Primary") { isEnabled, isLoading, text, shape, iconPosition ->
                PrimaryTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primaryInverted
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("secondary") {
            ButtonSection(title = "Secondary") { isEnabled, isLoading, text, shape, iconPosition ->
                SecondaryTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primary
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("primary_inverse") {
            ButtonSection(
                title = "PrimaryInverse",
                background = TangemTheme.colors2.surface.level2,
            ) { isEnabled, isLoading, text, shape, iconPosition ->
                PrimaryInverseTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primary
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("outline") {
            ButtonSection(title = "Outline") { isEnabled, isLoading, text, shape, iconPosition ->
                OutlineTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primary
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("accent") {
            ButtonSection(title = "Accent") { isEnabled, isLoading, text, shape, iconPosition ->
                StatusTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primaryInvertedConstant
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("positive") {
            ButtonSection(title = "Positive") { isEnabled, isLoading, text, shape, iconPosition ->
                StatusTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primaryInverted
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    type = TangemButtonType.Positive,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("ghost") {
            ButtonSection(title = "Ghost") { isEnabled, isLoading, text, shape, iconPosition ->
                GhostTangemButton(
                    onClick = {},
                    text = if (text) stringReference("Continue") else null,
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = {
                            if (isEnabled) {
                                TangemTheme.colors2.graphic.neutral.primary
                            } else {
                                TangemTheme.colors2.graphic.neutral.quaternary
                            }
                        },
                    ),
                    iconPosition = iconPosition,
                    size = TangemButtonSize.X10,
                    isEnabled = isEnabled,
                    isLoading = isLoading,
                    shape = shape,
                )
            }
        }
        item("sizes") {
            SizeShowcase()
        }
    }
}

@Composable
private fun SizeShowcase() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Sizes",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        TangemButtonSize.entries.forEach { size ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = size.name,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.width(STATE_LABEL_WIDTH.dp),
                )
                PrimaryTangemButton(
                    onClick = {},
                    text = stringReference("Button"),
                    tangemIconUM = TangemIconUM.Icon(
                        iconRes = R.drawable.ic_tangem_24,
                        tintReference = { TangemTheme.colors2.graphic.neutral.primaryInverted },
                    ),
                    size = size,
                )
            }
        }
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ButtonSection(
    title: String,
    background: Color = TangemTheme.colors2.surface.level1,
    shapes: List<TangemButtonShape> = TangemButtonShape.entries,
    button: @Composable (
        isEnabled: Boolean,
        isLoading: Boolean,
        text: Boolean,
        shape: TangemButtonShape,
        iconPosition: TangemButtonIconPosition,
    ) -> Unit,
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
            TangemButtonIconPosition.entries.forEach { iconPosition ->
                ShapeGroup(shape = shape, iconPosition = iconPosition, button = button)
            }
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
    iconPosition: TangemButtonIconPosition,
    button: @Composable (
        isEnabled: Boolean,
        isLoading: Boolean,
        text: Boolean,
        shape: TangemButtonShape,
        iconPosition: TangemButtonIconPosition,
    ) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${shape.name} / Icon ${iconPosition.name}",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        ColumnHeaderRow()
        StateRow(
            label = "Enabled",
            isEnabled = true,
            isLoading = false,
            shape = shape,
            iconPosition = iconPosition,
            button = button,
        )
        StateRow(
            label = "Disabled",
            isEnabled = false,
            isLoading = false,
            shape = shape,
            iconPosition = iconPosition,
            button = button,
        )
        StateRow(
            label = "Loading",
            isEnabled = true,
            isLoading = true,
            shape = shape,
            iconPosition = iconPosition,
            button = button,
        )
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

@Suppress("LongParameterList")
@Composable
private fun StateRow(
    label: String,
    isEnabled: Boolean,
    isLoading: Boolean,
    shape: TangemButtonShape,
    iconPosition: TangemButtonIconPosition,
    button: @Composable (
        isEnabled: Boolean,
        isLoading: Boolean,
        text: Boolean,
        shape: TangemButtonShape,
        iconPosition: TangemButtonIconPosition,
    ) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.width(STATE_LABEL_WIDTH.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            button(isEnabled, isLoading, true, shape, iconPosition)
        }
        Box(modifier = Modifier.weight(1f)) {
            button(isEnabled, isLoading, false, shape, iconPosition)
        }
    }
}