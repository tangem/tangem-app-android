package com.tangem.features.tangempay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.containers.pullToRefresh.TangemPullToRefreshContainer
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM

@Composable
internal fun TangemPayDetailsScreen(state: TangemPayDetailsUM, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TangemPayDetailsTopAppBar(config = state.topBarConfig) },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars),
        containerColor = TangemTheme.colors.background.secondary,
    ) { scaffoldPaddings ->
        val listState = rememberLazyListState()
        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

        TangemPullToRefreshContainer(
            config = state.pullToRefreshConfig,
            modifier = Modifier.padding(scaffoldPaddings),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    bottom = TangemTheme.dimens.spacing16 + bottomBarHeight,
                ),
            ) {

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TangemPayDetailsTopAppBar(config: TangemPayDetailsTopBarConfig, modifier: Modifier = Modifier) {
    var showDropdownMenu by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = config.onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = "Back",
                    modifier = Modifier.testTag(TokenDetailsTopBarTestTags.BACK_BUTTON),
                )
            }
        },
        title = {},
        actions = {
            AnimatedVisibility(visible = config.items != null && config.items.isNotEmpty()) {
                IconButton(onClick = { showDropdownMenu = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vertical_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = "More",
                        modifier = Modifier.testTag(TokenDetailsTopBarTestTags.MORE_BUTTON),
                    )
                }
            }

            TangemDropdownMenu(
                expanded = showDropdownMenu,
                modifier = Modifier.background(TangemTheme.colors.background.primary),
                onDismissRequest = { showDropdownMenu = false },
                content = {
                    config.items?.fastForEach {
                        TangemDropdownItem(
                            item = it,
                            dismissParent = { showDropdownMenu = false },
                        )
                    }
                },
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TangemTheme.colors.background.secondary,
            titleContentColor = TangemTheme.colors.icon.primary1,
            actionIconContentColor = TangemTheme.colors.icon.primary1,
        ),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
    )
}