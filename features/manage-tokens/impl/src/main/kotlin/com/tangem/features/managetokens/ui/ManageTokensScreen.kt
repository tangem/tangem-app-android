package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.PrimaryButtonIconEnd
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.rows.ArrowRow
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.features.managetokens.component.preview.PreviewManageTokensComponent
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.entity.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.ImmutableList

private const val CHEVRON_ROTATION_EXPANDED = 180f
private const val CHEVRON_ROTATION_COLLAPSED = 0f
private const val LOAD_ITEMS_BUFFER = 10

@Composable
internal fun ManageTokensScreen(state: ManageTokensUM, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                keyboardController?.hide()

                return super.onPreScroll(available, source)
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(nestedScrollConnection),
        containerColor = TangemTheme.colors.background.primary,
        contentWindowInsets = WindowInsetsZero,
        topBar = {
            ManageTokensTopBar(
                modifier = Modifier.statusBarsPadding(),
                topBar = state.topBar,
                search = state.search,
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                state = state,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (state is ManageTokensUM.ManageContent) {
                SaveChangesButton(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                    isVisible = state.hasChanges,
                    onClick = state.saveChanges,
                )
            }
        },
    )
}

@Composable
private fun ManageTokensTopBar(topBar: ManageTokensTopBarUM, search: SearchBarUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        TangemTopAppBar(
            title = topBar.title.resolveReference(),
            startButton = TopAppBarButtonUM.Back(topBar.onBackButtonClick),
            endButton = when (topBar) {
                is ManageTokensTopBarUM.ManageContent -> topBar.endButton
                is ManageTokensTopBarUM.ReadContent -> null
            },
        )
        SearchBar(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing12)
                .padding(horizontal = TangemTheme.dimens.spacing16),
            state = search,
        )
    }
}

@Composable
private fun SaveChangesButton(isVisible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        label = "save_button_visibility",
    ) {
        PrimaryButtonIconEnd(
            text = stringResource(id = R.string.common_save),
            iconResId = R.drawable.ic_tangem_24,
            onClick = onClick,
        )
    }
}

@Composable
private fun Content(state: ManageTokensUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Currencies(
            modifier = Modifier.fillMaxSize(),
            items = state.items,
            showLoadingItem = state.isNextBatchLoading,
            onLoadMore = state.loadMore,
            isEditable = state is ManageTokensUM.ManageContent,
        )

        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }

    Crossfade(targetState = state.isInitialBatchLoading, label = "ManageTokensLoadingContent") { isVisible ->
        if (isVisible) {
            ProgressIndicator(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Currencies(
    items: ImmutableList<CurrencyItemUM>,
    showLoadingItem: Boolean,
    isEditable: Boolean,
    onLoadMore: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) {
        WindowInsets.systemBars.getBottom(density = this).toDp()
    }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            bottom = TangemTheme.dimens.spacing76 + bottomBarHeight,
        ),
    ) {
        items(
            items = items,
            key = { it.id.value },
        ) { item ->
            when (item) {
                is CurrencyItemUM.Basic -> {
                    BasicCurrencyItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = item,
                        isEditable = isEditable,
                    )
                }
                is CurrencyItemUM.Custom -> {
                    CustomCurrencyItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = item,
                    )
                }
            }
        }

        if (showLoadingItem) {
            item(key = "loading_item") {
                ProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                )
            }
        }
    }

    InfiniteListHandler(
        listState = listState,
        buffer = LOAD_ITEMS_BUFFER,
        onLoadMore = onLoadMore,
    )
}

@Composable
private fun ProgressIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(color = TangemTheme.colors.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.informative)
    }
}

@Composable
private fun CustomCurrencyItem(item: CurrencyItemUM.Custom, modifier: Modifier = Modifier) {
    ChainRow(
        modifier = modifier,
        model = with(item) {
            ChainRowUM(
                name = name,
                type = symbol,
                icon = icon,
                showCustom = true,
            )
        },
        action = {
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = resourceReference(R.string.manage_tokens_remove),
                    onClick = item.onRemoveClick,
                ),
            )
        },
    )
}

@Composable
private fun BasicCurrencyItem(item: CurrencyItemUM.Basic, isEditable: Boolean, modifier: Modifier = Modifier) {
    val isExpanded = item.networks is NetworksUM.Expanded

    Column(modifier = modifier) {
        ChainRow(
            modifier = Modifier.clickable(onClick = item.onExpandClick),
            model = with(item) {
                ChainRowUM(
                    name = name,
                    type = symbol,
                    icon = icon,
                    showCustom = false,
                )
            },
            action = {
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) {
                        CHEVRON_ROTATION_EXPANDED
                    } else {
                        CHEVRON_ROTATION_COLLAPSED
                    },
                    label = "chevron_rotation",
                )

                Icon(
                    modifier = Modifier
                        .rotate(rotation)
                        .size(TangemTheme.dimens.size24),
                    painter = painterResource(id = R.drawable.ic_chevron_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            },
        )

        NetworksList(
            modifier = Modifier.padding(
                start = TangemTheme.dimens.spacing10,
                end = TangemTheme.dimens.spacing8,
            ),
            networks = item.networks,
            currencyId = item.id.value,
            isEditable = isEditable,
        )
    }
}

@Composable
private fun NetworksList(
    networks: NetworksUM,
    currencyId: String,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = networks is NetworksUM.Expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        label = "networks_visibility",
    ) {
        Column {
            val items = (networks as? NetworksUM.Expanded)?.networks

            // To keep items on collapse and avoid animation cancellation
            val rememberedItems = remember(key1 = currencyId) { items }
            val currentItems = items ?: rememberedItems

            currentItems?.fastForEachIndexed { index, network ->
                ArrowRow(
                    isLastItem = index == currentItems.lastIndex,
                    content = {
                        BlockchainRow(
                            modifier = Modifier.padding(end = TangemTheme.dimens.spacing8),
                            model = with(network) {
                                BlockchainRowUM(
                                    name = name,
                                    type = type,
                                    iconResId = iconResId,
                                    isMainNetwork = isMainNetwork,
                                    isSelected = isSelected,
                                )
                            },
                            action = {
                                if (isEditable) {
                                    TangemSwitch(
                                        checked = network.isSelected,
                                        onCheckedChange = network.onSelectedStateChange,
                                    )
                                }
                            },
                        )
                    },
                )
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ManageTokens() {
    TangemThemePreview {
        PreviewManageTokensComponent().Content(Modifier.fillMaxWidth())
    }
}
// endregion Preview