package com.tangem.feature.wallet.presentation.organizetokens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.common.component.DraggableNetworkGroupItem
import com.tangem.feature.wallet.presentation.common.component.DraggableTokenItem
import org.burnoutcrew.reorderable.*

@Composable
internal fun OrganizeTokensScreen(state: OrganizeTokensStateHolder, modifier: Modifier = Modifier) {
    val tokensListState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(state.header, tokensListState)
        },
        content = { paddingValues ->
            TokenList(
                modifier = Modifier.padding(paddingValues),
                listState = tokensListState,
                state = state.itemsState,
                dragConfig = state.dragConfig,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Actions(state.actions)
        },
        containerColor = TangemTheme.colors.background.secondary,
    )
}

@Composable
private fun TokenList(
    listState: LazyListState,
    state: OrganizeTokensListState,
    dragConfig: OrganizeTokensStateHolder.DragConfig,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val reorderableListState = rememberReorderableLazyListState(
            onMove = dragConfig.onItemDragged,
            listState = listState,
            canDragOver = dragConfig.canDragItemOver,
            onDragEnd = { _, _ -> dragConfig.onItemDragEnd() },
        )
        val items = state.items

        LazyColumn(
            modifier = Modifier
                .reorderable(reorderableListState)
                .align(Alignment.TopCenter)
                .padding(horizontal = TangemTheme.dimens.spacing16),
            state = reorderableListState.listState,
            contentPadding = PaddingValues(
                top = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing92,
            ),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
            ) { index, item ->

                val onDragStart = remember(item) {
                    { dragConfig.onDragStart(item) }
                }

                DraggableItem(
                    index = index,
                    item = item,
                    reorderableState = reorderableListState,
                    onDragStart = onDragStart,
                )

                if (item is DraggableItem.GroupPlaceholder) {
                    // This item should be displayed in the list but remain invisible
                    Box(modifier = Modifier.fillMaxWidth())
                }
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
    ReorderableItem(
        reorderableState = reorderableState,
        index = index,
        key = item.id,
    ) { isDragging ->

        if (isDragging) {
            onDragStart()
        }

        val itemModifier = Modifier.applyShapeAndShadow(item.roundingMode, item.showShadow)

        when (item) {
            is DraggableItem.GroupHeader -> DraggableNetworkGroupItem(
                modifier = itemModifier,
                networkName = item.networkName,
                reorderableTokenListState = reorderableState,
            )
            is DraggableItem.Token -> DraggableTokenItem(
                modifier = itemModifier,
                state = item.tokenItemState,
                reorderableTokenListState = reorderableState,
            )
            is DraggableItem.GroupPlaceholder -> Unit
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
    config: OrganizeTokensStateHolder.HeaderConfig,
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
                    text = stringResource(id = R.string.organize_tokens_sort_by_balance),
                    iconResId = R.drawable.ic_sort_24,
                    onClick = config.onSortByBalanceClick,
                ),
                modifier = Modifier.weight(1f),
                color = TangemTheme.colors.background.primary,
            )
            RoundedActionButton(
                config = ActionButtonConfig(
                    text = stringResource(id = R.string.organize_tokens_group),
                    iconResId = R.drawable.ic_group_24,
                    onClick = config.onGroupByNetworkClick,
                ),
                modifier = Modifier.weight(1f),
                color = TangemTheme.colors.background.primary,
            )
        }
    }
}

@Composable
private fun Actions(config: OrganizeTokensStateHolder.ActionsConfig, modifier: Modifier = Modifier) {
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
        )
    }
}

private fun Modifier.applyShapeAndShadow(roundingMode: DraggableItem.RoundingMode, showShadow: Boolean): Modifier =
    composed {
        val radius by animateDpAsState(
            targetValue = if (roundingMode !is DraggableItem.RoundingMode.None) {
                TangemTheme.dimens.radius16
            } else {
                TangemTheme.dimens.radius0
            },
            label = "item_shape_radius",
        )
        val shape = when (roundingMode) {
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

        val paddingValue = TangemTheme.dimens.spacing4
        val padding = if (roundingMode.showGap) {
            when (roundingMode) {
                is DraggableItem.RoundingMode.None -> null
                is DraggableItem.RoundingMode.All -> PaddingValues(vertical = paddingValue)
                is DraggableItem.RoundingMode.Top -> PaddingValues(top = paddingValue)
                is DraggableItem.RoundingMode.Bottom -> PaddingValues(bottom = paddingValue)
            }
        } else {
            null
        }

        this
            .let {
                if (padding != null) {
                    it.padding(padding)
                } else {
                    it
                }
            }
            .shadow(
                elevation = if (showShadow) TangemTheme.dimens.elevation12 else TangemTheme.dimens.elevation0,
                shape = shape,
                clip = true,
            )
    }

// region Preview

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OrganizeTokensScreenPreview_Light(
    @PreviewParameter(OrganizeTokensStateProvider::class) state: OrganizeTokensStateHolder,
) {
    TangemTheme {
        OrganizeTokensScreen(state)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OrganizeTokensScreenPreview_Dark(
    @PreviewParameter(OrganizeTokensStateProvider::class) state: OrganizeTokensStateHolder,
) {
    TangemTheme(isDark = true) {
        OrganizeTokensScreen(state)
    }
}

private class OrganizeTokensStateProvider : CollectionPreviewParameterProvider<OrganizeTokensStateHolder>(
    collection = listOf(
        WalletPreviewData.organizeTokensState,
        WalletPreviewData.groupedOrganizeTokensState,
    ),
)
// endregion Preview
