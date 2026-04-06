@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.searchfield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.ds.field.search.TangemFieldShape
import com.tangem.core.ui.ds.field.search.TangemSearchField
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemSearchFieldStory

@Composable
internal fun TangemSearchFieldStory(state: TangemSearchFieldStory, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        stickyHeader("shape_toggle") {
            ShapeToggle(
                selected = state.selectedShape,
                onSelect = state.onShapeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level1)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item("empty") {
            VariantSection(label = "Empty") {
                TangemSearchField(
                    state = SearchBarUM(
                        placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                        query = query,
                        onQueryChange = { query = it },
                        isActive = isActive,
                        onActiveChange = { isActive = it },
                        onClearClick = { query = "" },
                    ),
                    shape = state.selectedShape,
                )
            }
        }

        item("prefilled") {
            VariantSection(label = "Pre-filled") {
                var prefilledQuery by remember { mutableStateOf("Bitcoin") }
                var isPrefilledActive by remember { mutableStateOf(false) }
                TangemSearchField(
                    state = SearchBarUM(
                        placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                        query = prefilledQuery,
                        onQueryChange = { prefilledQuery = it },
                        isActive = isPrefilledActive,
                        onActiveChange = { isPrefilledActive = it },
                        onClearClick = { prefilledQuery = "" },
                    ),
                    shape = state.selectedShape,
                )
            }
        }
    }
}

@Composable
private fun ShapeToggle(
    selected: TangemFieldShape,
    onSelect: (TangemFieldShape) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
                shape = shape,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TangemFieldShape.entries.forEach { fieldShape ->
            ShapeChip(
                label = fieldShape.name,
                selected = fieldShape == selected,
                onClick = { onSelect(fieldShape) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ShapeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun VariantSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        content()
    }
}