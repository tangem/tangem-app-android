package com.tangem.feature.tester.presentation.storybook.page.contextmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuCheckboxItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.rememberHazeState
import com.tangem.feature.tester.presentation.storybook.entity.TangemContextMenuStory as TangemContextMenuStoryState

@Composable
internal fun TangemContextMenuStory(state: TangemContextMenuStoryState, modifier: Modifier = Modifier) {
    val hazeState = rememberHazeState()
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1)
            .hazeSourceTangem(state = hazeState, zIndex = -1f),
    ) {
        item("context_menu") {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "TangemContextMenu",
                    style = TangemTheme.typography2.headingSemibold17,
                    color = TangemTheme.colors2.text.neutral.primary,
                )

                Box {
                    PrimaryButton(
                        text = "Show Context Menu",
                        onClick = { state.onExpandedChange(true) },
                    )

                    TangemContextMenu(
                        expanded = state.isExpanded,
                        onDismissRequest = { state.onExpandedChange(false) },
                        offset = DpOffset(0.dp, 4.dp),
                        modifier = Modifier.hazeEffectTangem(hazeState),
                    ) {
                        TangemContextMenuCheckboxItem(
                            title = TextReference.Str("Sort by balance"),
                            isChecked = true,
                            onClick = {},
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = TangemTheme.colors2.border.neutral.quaternary,
                        )
                        TangemContextMenuCheckboxItem(
                            title = TextReference.Str("Group tokens"),
                            isChecked = false,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}