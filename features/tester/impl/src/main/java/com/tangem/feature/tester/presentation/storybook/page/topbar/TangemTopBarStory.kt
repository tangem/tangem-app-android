@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopBarStory
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemTopBarStory(state: TangemTopBarStory, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        stickyHeader("type_toggle") {
            TypeToggle(
                selected = state.selectedType,
                onSelect = state.onTypeChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level1)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item("title_only") {
            TopBarVariant(label = "Title only") {
                TangemTopBar(
                    title = stringReference("Wallet"),
                    type = state.selectedType,
                    startAction = null,
                )
            }
        }

        item("title_subtitle") {
            TopBarVariant(label = "Title + Subtitle") {
                TangemTopBar(
                    title = stringReference("Wallet"),
                    subtitle = stringReference("3 cards"),
                    type = state.selectedType,
                    startAction = null,
                )
            }
        }

        item("back_action") {
            TopBarVariant(label = "Back action + Title") {
                TangemTopBar(
                    title = stringReference("Send"),
                    type = state.selectedType,
                    startAction = TangemTopBarActionUM(
                        iconRes = R.drawable.ic_back_24,
                        onClick = {},
                    ),
                )
            }
        }

        item("back_and_end") {
            TopBarVariant(label = "Back + Title + End action") {
                TangemTopBar(
                    title = stringReference("Token Details"),
                    type = state.selectedType,
                    startAction = TangemTopBarActionUM(
                        iconRes = R.drawable.ic_back_24,
                        onClick = {},
                    ),
                    endActions = persistentListOf(
                        TangemTopBarActionUM(
                            iconRes = R.drawable.ic_more_default_24,
                            onClick = {},
                        ),
                    ),
                )
            }
        }

        item("back_and_two_end") {
            TopBarVariant(label = "Back + Title + 2 End actions") {
                TangemTopBar(
                    title = stringReference("Settings"),
                    type = state.selectedType,
                    startAction = TangemTopBarActionUM(
                        iconRes = R.drawable.ic_back_24,
                        onClick = {},
                    ),
                    endActions = persistentListOf(
                        TangemTopBarActionUM(
                            iconRes = R.drawable.ic_information_24,
                            onClick = {},
                        ),
                        TangemTopBarActionUM(
                            iconRes = R.drawable.ic_close_24,
                            onClick = {},
                        ),
                    ),
                )
            }
        }

        item("ghost_actions") {
            TopBarVariant(label = "Ghost mode actions (progress=1)") {
                TangemTopBar(
                    title = stringReference("Portfolio"),
                    type = state.selectedType,
                    startAction = TangemTopBarActionUM(
                        iconRes = R.drawable.ic_tangem_24,
                        onClick = {},
                        ghostModeProgress = 1f,
                    ),
                    endActions = persistentListOf(
                        TangemTopBarActionUM(
                            iconRes = R.drawable.ic_more_default_24,
                            onClick = {},
                            ghostModeProgress = 1f,
                        ),
                    ),
                )
            }
        }

        item("non_actionable") {
            TopBarVariant(label = "Non-actionable icons") {
                TangemTopBar(
                    title = stringReference("Details"),
                    type = state.selectedType,
                    startAction = TangemTopBarActionUM(
                        iconRes = R.drawable.ic_tangem_24,
                        isActionable = false,
                    ),
                    endActions = persistentListOf(
                        TangemTopBarActionUM(
                            iconRes = R.drawable.ic_more_default_24,
                            isActionable = false,
                        ),
                    ),
                )
            }
        }

        item("title_icon") {
            TopBarVariant(label = "Title with icon") {
                TangemTopBar(
                    title = stringReference("Wallet"),
                    titleIconRes = R.drawable.ic_tangem_24,
                    type = state.selectedType,
                    startAction = TangemTopBarActionUM(
                        iconRes = R.drawable.ic_back_24,
                        onClick = {},
                    ),
                )
            }
        }

        item("no_title") {
            TopBarVariant(label = "No title (end action only)") {
                TangemTopBar(
                    type = state.selectedType,
                    startAction = null,
                    endActions = persistentListOf(
                        TangemTopBarActionUM(
                            iconRes = R.drawable.ic_close_24,
                            onClick = {},
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun TypeToggle(
    selected: TangemTopBarType,
    onSelect: (TangemTopBarType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(width = 1.dp, color = TangemTheme.colors2.border.neutral.secondary, shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TangemTopBarType.entries.forEach { type ->
            TypeChip(
                label = type.name,
                selected = type == selected,
                onClick = { onSelect(type) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2)
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
private fun TopBarVariant(label: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        content()
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}