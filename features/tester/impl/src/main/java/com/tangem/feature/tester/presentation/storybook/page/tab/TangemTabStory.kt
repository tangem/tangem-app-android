@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.tabs.TangemTab
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemTabStory

@Composable
internal fun TangemTabStory(state: TangemTabStory, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("interactive") {
            TabSection(title = "Interactive") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Markets", "Portfolio", "Activity").forEachIndexed { index, label ->
                        TangemTab(
                            text = stringReference(label),
                            isChecked = state.checkedIndex == index,
                            onCheckedChange = { if (it) state.onCheckedIndexChange(index) },
                        )
                    }
                }
            }
        }

        item("checked") {
            TabSection(title = "Checked") {
                TangemTab(
                    text = stringReference("Markets"),
                    isChecked = true,
                    onCheckedChange = {},
                )
            }
        }

        item("unchecked") {
            TabSection(title = "Unchecked") {
                TangemTab(
                    text = stringReference("Markets"),
                    isChecked = false,
                    onCheckedChange = {},
                )
            }
        }

        item("disabled_checked") {
            TabSection(title = "Disabled (Checked)") {
                TangemTab(
                    text = stringReference("Markets"),
                    isChecked = true,
                    onCheckedChange = {},
                    isEnabled = false,
                )
            }
        }

        item("disabled_unchecked") {
            TabSection(title = "Disabled (Unchecked)") {
                TangemTab(
                    text = stringReference("Markets"),
                    isChecked = false,
                    onCheckedChange = {},
                    isEnabled = false,
                )
            }
        }

        item("multiple_tabs") {
            TabSection(title = "Multiple tabs row") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TangemTab(
                        text = stringReference("All"),
                        isChecked = true,
                        onCheckedChange = {},
                    )
                    TangemTab(
                        text = stringReference("Gainers"),
                        isChecked = false,
                        onCheckedChange = {},
                    )
                    TangemTab(
                        text = stringReference("Losers"),
                        isChecked = false,
                        onCheckedChange = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun TabSection(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        content()
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}