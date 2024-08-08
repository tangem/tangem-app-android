package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.tangem.core.ui.components.rows.ArrowRow
import com.tangem.core.ui.components.rows.BlockchainRow
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.model.BlockchainRowUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.component.preview.PreviewManageTokensComponent
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.entity.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.ImmutableList

private const val CHEVRON_ROTATION_EXPANDED = 180f
private const val CHEVRON_ROTATION_COLLAPSED = 0f

@Composable
internal fun ManageTokensScreen(state: ManageTokensUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.popBack)

    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.primary,
        topBar = {
            ManageTokensTopBar(
                modifier = Modifier.statusBarsPadding(),
                topBar = state.topBar,
            )
        },
        content = { innerPadding ->
            Content(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                search = state.search,
                items = state.items,
                isLoading = state.isLoading,
                hasChanges = state is ManageTokensUM.ManageContent && state.hasChanges,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (state is ManageTokensUM.ManageContent) {
                SaveChangesButton(
                    modifier = Modifier
                        .padding(horizontal = TangemTheme.dimens.spacing16)
                        .fillMaxWidth(),
                    isVisible = state.hasChanges,
                    onClick = state.onSaveClick,
                )
            }
        },
    )
}

@Composable
private fun ManageTokensTopBar(topBar: ManageTokensTopBarUM, modifier: Modifier = Modifier) {
    TangemTopAppBar(
        modifier = modifier,
        title = topBar.title.resolveReference(),
        startButton = TopAppBarButtonUM.Back(topBar.onBackButtonClick),
        endButton = when (topBar) {
            is ManageTokensTopBarUM.ManageContent -> topBar.endButton
            is ManageTokensTopBarUM.ReadContent -> null
        },
    )
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
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = TangemTheme.colors.icon.accent)
    }
}

@Composable
private fun Content(
    search: SearchBarUM,
    items: ImmutableList<CurrencyItemUM>,
    isLoading: Boolean,
    hasChanges: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Currencies(
            modifier = Modifier.fillMaxSize(),
            items = items,
            search = search,
        )

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            visible = hasChanges,
            label = "bottom_fade_visibility",
        ) {
            BottomFade()
        }
    }

    Crossfade(targetState = isLoading, label = "ManageTokensLoadingContent") {
        if (it) {
            LoadingContent()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Currencies(items: ImmutableList<CurrencyItemUM>, search: SearchBarUM, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
    ) {
        stickyHeader(key = "search") {
            Column(
                modifier = Modifier
                    .background(TangemTheme.colors.background.primary)
                    .padding(
                        top = TangemTheme.dimens.spacing16,
                        bottom = TangemTheme.dimens.spacing12,
                    )
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .fillMaxWidth(),
            ) {
                SearchBar(state = search)
            }
        }

        items(
            items = items,
            key = CurrencyItemUM::id,
        ) { item ->
            when (item) {
                is CurrencyItemUM.Basic -> {
                    BasicCurrencyItem(
                        modifier = Modifier.fillMaxWidth(),
                        item = item,
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
    }
}

@Composable
private fun CustomCurrencyItem(item: CurrencyItemUM.Custom, modifier: Modifier = Modifier) {
    ChainRow(
        modifier = modifier,
        model = item.model,
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
private fun BasicCurrencyItem(item: CurrencyItemUM.Basic, modifier: Modifier = Modifier) {
    val isExpanded = item.networks is NetworksUM.Expanded

    Column(modifier = modifier) {
        ChainRow(
            modifier = Modifier.clickable(onClick = item.onExpandClick),
            model = item.model,
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
            currencyId = item.id,
        )
    }
}

@Composable
private fun NetworksList(networks: NetworksUM, currencyId: String, modifier: Modifier = Modifier) {
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
                                TangemSwitch(
                                    checked = network.isSelected,
                                    onCheckedChange = network.onSelectedStateChange,
                                )
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
