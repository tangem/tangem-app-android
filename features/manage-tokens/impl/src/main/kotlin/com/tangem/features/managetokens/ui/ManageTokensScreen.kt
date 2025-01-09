package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.components.rows.ArrowRow
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.ChainRowContainer
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.core.ui.utils.rememberHideKeyboardNestedScrollConnection
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.component.preview.PreviewManageTokensComponent
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.item.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.ImmutableList

private const val CHEVRON_ROTATION_EXPANDED = 180f
private const val CHEVRON_ROTATION_COLLAPSED = 0f
private const val LOAD_ITEMS_BUFFER = 10

@Composable
internal fun ManageTokensScreen(state: ManageTokensUM, modifier: Modifier = Modifier) {
    val nestedScrollConnection = rememberHideKeyboardNestedScrollConnection()

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
                    showProgress = state.isSavingInProgress,
                    showIcon = state.needToAddDerivations,
                    onClick = state.saveChanges,
                )
            }
        },
    )
}

@Composable
private fun ManageTokensTopBar(topBar: ManageTokensTopBarUM?, search: SearchBarUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
    ) {
        if (topBar != null) {
            TangemTopAppBar(
                title = topBar.title.resolveReference(),
                startButton = TopAppBarButtonUM.Back(topBar.onBackButtonClick),
                endButton = when (topBar) {
                    is ManageTokensTopBarUM.ManageContent -> topBar.endButton
                    is ManageTokensTopBarUM.ReadContent -> null
                },
            )
        }
        SearchBar(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing12)
                .padding(horizontal = TangemTheme.dimens.spacing16),
            state = search,
        )
    }
}

@Composable
private fun SaveChangesButton(
    isVisible: Boolean,
    showProgress: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        label = "save_button_visibility",
    ) {
        TangemButton(
            text = stringResourceSafe(id = R.string.common_save),
            icon = if (showIcon) {
                TangemButtonIconPosition.End(R.drawable.ic_tangem_24)
            } else {
                TangemButtonIconPosition.None
            },
            showProgress = showProgress,
            colors = TangemButtonsDefaults.primaryButtonColors,
            textStyle = TangemTheme.typography.subtitle1,
            enabled = true,
            animateContentChange = true,
            onClick = onClick,
        )
    }
}

@Composable
private fun Content(state: ManageTokensUM, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Box(modifier = modifier) {
        Currencies(
            modifier = Modifier.fillMaxSize(),
            listState = listState,
            items = state.items,
            showLoadingItem = state.isNextBatchLoading,
            onLoadMore = state.loadMore,
            isEditable = state is ManageTokensUM.ManageContent,
        )

        BottomFade(modifier = Modifier.align(Alignment.BottomCenter))
    }

    EventEffect(event = state.scrollToTop) {
        listState.animateScrollToItem(index = 0)
    }
}

@Composable
internal fun Currencies(
    listState: LazyListState,
    items: ImmutableList<CurrencyItemUM>,
    showLoadingItem: Boolean,
    isEditable: Boolean,
    onLoadMore: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val bottomBarHeight = with(LocalDensity.current) {
        WindowInsets.systemBars.getBottom(density = this).toDp()
    }

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
                is CurrencyItemUM.Loading -> {
                    LoadingItem(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is CurrencyItemUM.SearchNothingFound -> {
                    SearchNothingFoundText(modifier = Modifier.fillParentMaxSize())
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
private fun SearchNothingFoundText(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_search_token_no_result_title),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.tertiary,
        )
    }
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
private fun LoadingItem(modifier: Modifier = Modifier) {
    ChainRowContainer(
        modifier = modifier,
        icon = {
            CurrencyIcon(CurrencyIconState.Loading)
        },
        text = {
            TextShimmer(
                modifier = Modifier.width(70.dp),
                style = TangemTheme.typography.subtitle2,
            )
        },
        action = {
            RectangleShimmer(
                modifier = Modifier.size(
                    width = 24.dp,
                    height = 16.dp,
                ),
            )
        },
    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetworksList(
    networks: NetworksUM,
    currencyId: String,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
) {
    val hapticManager = LocalHapticManager.current

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
                            modifier = Modifier
                                .padding(end = TangemTheme.dimens.spacing8)
                                .combinedClickable(
                                    onLongClick = network.onLongClick,
                                    onClick = {},
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ),
                            model = with(network) {
                                BlockchainRowUM(
                                    id = id,
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
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                hapticManager.perform(TangemHapticEffect.View.ToggleOn)
                                            } else {
                                                hapticManager.perform(TangemHapticEffect.View.ToggleOff)
                                            }
                                            network.onSelectedStateChange(checked)
                                        },
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
private fun Preview_ManageTokens(
    @PreviewParameter(PreviewManageTokensComponentProvider::class) component: ManageTokensComponent,
) {
    TangemThemePreview {
        component.Content(Modifier.fillMaxWidth())
    }
}

private class PreviewManageTokensComponentProvider : PreviewParameterProvider<ManageTokensComponent> {
    override val values: Sequence<ManageTokensComponent>
        get() = sequenceOf(
            PreviewManageTokensComponent(
                isLoading = true,
                showTangemIcon = true,
                params = ManageTokensComponent.Params(
                    source = ManageTokensSource.ONBOARDING,
                    userWalletId = UserWalletId("wallet_id"),
                ),
            ),
            PreviewManageTokensComponent(
                isLoading = false,
                showTangemIcon = true,
                params = ManageTokensComponent.Params(source = ManageTokensSource.ONBOARDING, userWalletId = null),
            ),
            PreviewManageTokensComponent(
                isLoading = false,
                showTangemIcon = false,
                params = ManageTokensComponent.Params(
                    source = ManageTokensSource.ONBOARDING,
                    userWalletId = UserWalletId("wallet_id"),
                ),
            ),
        )
}
// endregion Preview