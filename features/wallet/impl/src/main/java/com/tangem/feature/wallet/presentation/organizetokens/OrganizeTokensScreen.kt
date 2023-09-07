package com.tangem.feature.wallet.presentation.organizetokens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.DraggableNetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.TokenItem
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
internal fun OrganizeTokensScreen(state: OrganizeTokensState, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.onBackClick)

    val tokensListState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(state.header, tokensListState)
        },
        content = { paddingValues ->
            TokenList(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                listState = tokensListState,
                state = state.itemsState,
                dndConfig = state.dndConfig,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Actions(state.actions)
        },
        containerColor = TangemTheme.colors.background.secondary,
    )

    EventEffect(state.scrollListToTop) {
        tokensListState.animateScrollToItem(index = 0)
    }
}

@Composable
private fun TokenList(
    listState: LazyListState,
    state: OrganizeTokensListState,
    dndConfig: OrganizeTokensState.DragAndDropConfig,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val onDragEnd: (Int, Int) -> Unit = remember {
            { _, _ ->
                dndConfig.onItemDragEnd()
            }
        }
        val reorderableListState = rememberReorderableLazyListState(
            onMove = dndConfig.onItemDragged,
            listState = listState,
            canDragOver = dndConfig.canDragItemOver,
            onDragEnd = onDragEnd,
        )

        val listContentPadding = PaddingValues(
            top = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing92,
            start = TangemTheme.dimens.spacing16,
            end = TangemTheme.dimens.spacing16,
        )

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .reorderable(reorderableListState),
            state = reorderableListState.listState,
            contentPadding = listContentPadding,
        ) {
            itemsIndexed(
                items = state.items,
                key = { _, item -> item.id },
            ) { index, item ->

                val onDragStart = remember(item) {
                    { dndConfig.onItemDragStart(item) }
                }

                DraggableItem(
                    index = index,
                    item = item,
                    reorderableState = reorderableListState,
                    onDragStart = onDragStart,
                )
            }
        }

        BottomGradient(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LazyItemScope.DraggableItem(
    index: Int,
    item: DraggableItem,
    reorderableState: ReorderableLazyListState,
    onDragStart: () -> Unit,
) {
    var isDragging by remember {
        mutableStateOf(value = false)
    }

    val itemModifier = Modifier.applyShapeAndShadow(item.roundingMode, item.showShadow)

    ReorderableItem(
        reorderableState = reorderableState,
        index = index,
        key = item.id,
    ) { isItemDragging ->
        isDragging = isItemDragging

        when (item) {
            is DraggableItem.GroupHeader -> DraggableNetworkGroupItem(
                modifier = itemModifier,
                networkName = item.networkName,
                reorderableTokenListState = reorderableState,
            )
            is DraggableItem.Token -> TokenItem(
                modifier = itemModifier,
                state = item.tokenItemState,
                reorderableTokenListState = reorderableState,
            )
            // Should be presented in the list but remain invisible
            is DraggableItem.Placeholder -> Box(modifier = Modifier.fillMaxWidth())
        }
    }

    DisposableEffect(isDragging) {
        onDispose {
            if (isDragging) {
                onDragStart()
            }
        }
    }
}

@Composable
private fun BottomGradient(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size116)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        TangemTheme.colors.background.secondary,
                    ),
                ),
            ),
    )
}

@Composable
private fun TopBar(
    config: OrganizeTokensState.HeaderConfig,
    tokensListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val isElevationEnabled by remember {
        derivedStateOf {
            tokensListState.firstVisibleItemScrollOffset > 0
        }
    }
    val elevation by animateDpAsState(
        targetValue = if (isElevationEnabled) AppBarDefaults.TopAppBarElevation else TangemTheme.dimens.elevation0,
        label = "top_bar_shadow_elevation",
    )

    Column(
        modifier = modifier
            .shadow(elevation)
            .background(TangemTheme.colors.background.secondary)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size56),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(id = R.string.organize_tokens_title),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size56),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            RoundedActionButton(
                config = ActionButtonConfig(
                    text = TextReference.Res(id = R.string.organize_tokens_sort_by_balance),
                    iconResId = R.drawable.ic_sort_24,
                    enabled = config.isEnabled,
                    onClick = config.onSortClick,
                    dimContent = !config.isSortedByBalance,
                ),
                modifier = Modifier.weight(1f),
                color = TangemTheme.colors.background.primary,
            )
            RoundedActionButton(
                config = ActionButtonConfig(
                    text = TextReference.Res(
                        id = if (config.isGrouped) {
                            R.string.organize_tokens_ungroup
                        } else {
                            R.string.organize_tokens_group
                        },
                    ),
                    enabled = config.isEnabled,
                    iconResId = R.drawable.ic_group_24,
                    onClick = config.onGroupClick,
                ),
                modifier = Modifier.weight(1f),
                color = TangemTheme.colors.background.primary,
            )
        }
    }
}

@Composable
private fun Actions(config: OrganizeTokensState.ActionsConfig, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        SecondaryButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_cancel),
            onClick = config.onCancelClick,
        )
        PrimaryButton(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.common_apply),
            onClick = config.onApplyClick,
            showProgress = config.showApplyProgress,
            enabled = config.canApply,
        )
    }
}

private fun Modifier.applyShapeAndShadow(roundingMode: DraggableItem.RoundingMode, showShadow: Boolean): Modifier {
    return composed {
        val radius by animateDpAsState(
            targetValue = when (roundingMode) {
                is DraggableItem.RoundingMode.None -> TangemTheme.dimens.radius0
                is DraggableItem.RoundingMode.All -> TangemTheme.dimens.radius12
                is DraggableItem.RoundingMode.Bottom,
                is DraggableItem.RoundingMode.Top,
                -> TangemTheme.dimens.radius16
            },
            label = "item_shape_radius",
        )
        val elevation by animateDpAsState(
            targetValue = if (showShadow) {
                TangemTheme.dimens.elevation8
            } else {
                TangemTheme.dimens.elevation0
            },
            label = "item_elevation",
        )

        this
            .padding(paddingValues = getItemGap(roundingMode))
            .shadow(
                elevation = elevation,
                shape = getItemShape(roundingMode, radius),
                clip = true,
            )
    }
}

@Composable
@ReadOnlyComposable
private fun getItemGap(roundingMode: DraggableItem.RoundingMode): PaddingValues {
    val paddingValue = TangemTheme.dimens.spacing4

    return if (roundingMode.showGap) {
        when (roundingMode) {
            is DraggableItem.RoundingMode.None -> PaddingValues(all = 0.dp)
            is DraggableItem.RoundingMode.All -> PaddingValues(vertical = paddingValue)
            is DraggableItem.RoundingMode.Top -> PaddingValues(top = paddingValue)
            is DraggableItem.RoundingMode.Bottom -> PaddingValues(bottom = paddingValue)
        }
    } else {
        PaddingValues(all = 0.dp)
    }
}

@Stable
private fun getItemShape(roundingMode: DraggableItem.RoundingMode, radius: Dp): Shape {
    return when (roundingMode) {
        is DraggableItem.RoundingMode.None -> RectangleShape
        is DraggableItem.RoundingMode.Top -> RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
        )
        is DraggableItem.RoundingMode.Bottom -> RoundedCornerShape(
            bottomStart = radius,
            bottomEnd = radius,
        )
        is DraggableItem.RoundingMode.All -> RoundedCornerShape(
            size = radius,
        )
    }
}

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OrganizeTokensScreenPreview_Light(
    @PreviewParameter(OrganizeTokensStateProvider::class) state: OrganizeTokensState,
) {
    TangemTheme {
        OrganizeTokensScreen(state)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OrganizeTokensScreenPreview_Dark(
    @PreviewParameter(OrganizeTokensStateProvider::class) state: OrganizeTokensState,
) {
    TangemTheme(isDark = true) {
        OrganizeTokensScreen(state)
    }
}

private class OrganizeTokensStateProvider : CollectionPreviewParameterProvider<OrganizeTokensState>(
    collection = listOf(
        WalletPreviewData.organizeTokensState,
        WalletPreviewData.groupedOrganizeTokensState,
    ),
)
// endregion Preview
