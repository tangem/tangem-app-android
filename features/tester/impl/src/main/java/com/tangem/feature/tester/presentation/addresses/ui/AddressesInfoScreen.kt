package com.tangem.feature.tester.presentation.addresses.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.addresses.state.AddressesInfoScreenUM
import com.tangem.feature.tester.presentation.addresses.state.CopyType
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Screen with addresses info in text and JSON format
 *
 * @param uiModel screen state
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddressesInfoScreen(uiModel: AddressesInfoScreenUM) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Text", "JSON")

    Scaffold(
        topBar = {
            AppBarWithBackButton(
                onBackClick = uiModel.onBackClick,
                text = stringResourceSafe(id = uiModel.title),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        containerColor = TangemTheme.colors.background.secondary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TangemTheme.colors.background.secondary,
                contentColor = TangemTheme.colors.text.primary1,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = TangemTheme.typography.subtitle2,
                                color = if (selectedTab == index) {
                                    TangemTheme.colors.text.primary1
                                } else {
                                    TangemTheme.colors.text.secondary
                                },
                            )
                        },
                        selectedContentColor = TangemTheme.colors.text.primary1,
                        unselectedContentColor = TangemTheme.colors.text.secondary,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = {
                        uiModel.onCopyClick(
                            if (selectedTab == 0) CopyType.TEXT else CopyType.JSON,
                        )
                    },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_copy_24),
                        contentDescription = "Copy",
                        tint = TangemTheme.colors.icon.primary1,
                    )
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            SelectionContainer {
                                Text(
                                    text = uiModel.addressesText,
                                    style = TangemTheme.typography.body1,
                                    color = TangemTheme.colors.text.primary1,
                                )
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            SelectionContainer {
                                Text(
                                    text = uiModel.addressesJson,
                                    style = TangemTheme.typography.body1,
                                    color = TangemTheme.colors.text.primary1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}