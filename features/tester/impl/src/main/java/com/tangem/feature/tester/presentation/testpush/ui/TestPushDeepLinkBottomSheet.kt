package com.tangem.feature.tester.presentation.testpush.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.OutlineTextField
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMarketsTokenConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMenuConfigUM

@Composable
internal fun TestPushDeeplinkBottomSheet(config: TangemBottomSheetConfig?) {
    when (config?.content) {
        is TestPushMenuConfigUM -> TestPushMenuDeepLinkBottomSheetContent(config)
        is TestPushMarketsTokenConfigUM -> TestPushMarketsTokenBottomSheetContent(config)
    }
}

@Composable
private fun TestPushMenuDeepLinkBottomSheetContent(config: TangemBottomSheetConfig) {
    TangemBottomSheet<TestPushMenuConfigUM>(config) { content ->
        LazyColumn {
            items(
                items = content.itemList.toList(),
                key = { it },
                itemContent = { item ->
                    Text(
                        text = item.name,
                        style = TangemTheme.typography.subtitle1,
                        color = TangemTheme.colors.text.primary1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { content.onItemClick(item) },
                            )
                            .padding(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun TestPushMarketsTokenBottomSheetContent(config: TangemBottomSheetConfig) {
    TangemBottomSheet<TestPushMarketsTokenConfigUM>(config) { content ->
        LazyColumn {
            item(key = "Markets Search") {
                OutlineTextField(
                    label = "Title",
                    value = content.searchValue,
                    onValueChange = content.onSearchValueEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            items(
                items = content.itemList.toList(),
                key = { it.id.value },
                itemContent = { item ->
                    Text(
                        text = "${item.id}: ${item.symbol}",
                        style = TangemTheme.typography.subtitle1,
                        color = TangemTheme.colors.text.primary1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { content.onItemClick(item) },
                            )
                            .padding(16.dp),
                    )
                },
            )
        }
    }
}