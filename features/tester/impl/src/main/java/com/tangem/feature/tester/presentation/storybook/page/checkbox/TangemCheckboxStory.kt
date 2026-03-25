@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.checkbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.checkbox.TangemCheckbox
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemCheckboxStory

private const val STATE_LABEL_WIDTH = 80

@Composable
internal fun TangemCheckboxStory(state: TangemCheckboxStory, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("grid") {
            CheckboxGrid(state = state)
        }
    }
}

@Composable
private fun CheckboxGrid(state: TangemCheckboxStory) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(TangemTheme.colors2.surface.level1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        ColumnHeaderRow()
        CheckboxRow(
            label = "Rounded",
            isChecked = state.isRoundedChecked,
            onCheckedChange = state.onRoundedCheckedChange,
            isRounded = true,
        )
        CheckboxRow(
            label = "Circle",
            isChecked = state.isCircleChecked,
            onCheckedChange = state.onCircleCheckedChange,
            isRounded = false,
        )
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ColumnHeaderRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(STATE_LABEL_WIDTH.dp))
        Text(
            text = "Enabled",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Disabled",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Disabled (on)",
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CheckboxRow(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit, isRounded: Boolean) {
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
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            TangemCheckbox(
                isChecked = isChecked,
                isRounded = isRounded,
                isEnabled = true,
                onCheckedChange = onCheckedChange,
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            TangemCheckbox(
                isChecked = false,
                isRounded = isRounded,
                isEnabled = false,
                onCheckedChange = {},
            )
        }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f),
        ) {
            TangemCheckbox(
                isChecked = true,
                isRounded = isRounded,
                isEnabled = false,
                onCheckedChange = {},
            )
        }
    }
}