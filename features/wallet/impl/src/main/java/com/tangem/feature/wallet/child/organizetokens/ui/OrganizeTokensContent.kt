package com.tangem.feature.wallet.child.organizetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.TangemButton
import com.tangem.core.ui.ds.row.header.TangemHeaderRow
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarActionContent
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.reordarable.ReorderableItem
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.OrganizeTokensScreenTestTags
import com.tangem.core.ui.utils.lazyListItemPosition
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.child.organizetokens.entity.RoundingModeUM
import com.tangem.feature.wallet.child.organizetokens.model.DragAndDropIntents
import com.tangem.feature.wallet.child.organizetokens.ui.preview.OrganizeTokensPreview
import com.tangem.feature.wallet.impl.R
import dev.chrisbanes.haze.rememberHazeState
import org.burnoutcrew.reorderable.ItemPosition
import org.burnoutcrew.reorderable.ReorderableLazyListState
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
internal fun OrganizeTokensContent(
    organizeTokensUM: OrganizeTokensUM,
    dragAndDropIntents: DragAndDropIntents,
    onDismiss: () -> Unit,
) {
    var isShowDropdownMenu by rememberSaveable { mutableStateOf(false) }

    val hazeState = rememberHazeState()

    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors2.surface.level2,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.organize_tokens_title),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemTopBarActionContent(
                        actionUM = TangemTopBarActionUM(
                            iconRes = R.drawable.ic_exchange_mini_24,
                            isActionable = true,
                            onClick = { isShowDropdownMenu = true },
                            ghostModeProgress = 0f,
                        ),
                        type = TangemTopBarType.BottomSheet,
                    )
                    OrganizeDropDownMenu(
                        organizeMenuUM = organizeTokensUM.organizeMenuUM,
                        showDropdownMenu = isShowDropdownMenu,
                        onDropdownDismiss = { isShowDropdownMenu = false },
                        modifier = Modifier.hazeEffectTangem(hazeState),
                    )
                },
            )
        },
        footer = {
            BottomButtons(organizeTokensUM = organizeTokensUM)
        },
        content = {
            TokenList(
                organizeTokensUM = organizeTokensUM,
                dragAndDropIntents = dragAndDropIntents,
                modifier = Modifier.hazeSourceTangem(hazeState, zIndex = -1f),
            )
        },
    )
}

@Suppress("MagicNumber")
@Composable
private fun TokenList(
    organizeTokensUM: OrganizeTokensUM,
    dragAndDropIntents: DragAndDropIntents,
    modifier: Modifier = Modifier,
) {
    val tokensListState = rememberLazyListState()

    val hapticFeedback = LocalHapticFeedback.current
    val tokenList = organizeTokensUM.tokenList
    Box(
        modifier = modifier.background(TangemTheme.colors2.surface.level2),
    ) {
        val onDragEnd: (Int, Int) -> Unit = remember {
            { _, _ ->
                dragAndDropIntents.onItemDraggingEnd()
            }
        }
        val reorderableListState = rememberReorderableLazyListState(
            onMove = dragAndDropIntents::onItemDragged,
            listState = tokensListState,
            canDragOver = dragAndDropIntents::canDragItemOver,
            onDragEnd = onDragEnd,
        )

        val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }

        val listContentPadding = PaddingValues(
            top = TangemTheme.dimens2.x1,
            bottom = TangemTheme.dimens2.x1 + bottomBarHeight,
            start = TangemTheme.dimens2.x3,
            end = TangemTheme.dimens2.x3,
        )

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .reorderable(reorderableListState)
                .testTag(OrganizeTokensScreenTestTags.TOKENS_LAZY_LIST)
                .hazeSourceTangem(zIndex = 1f),
            state = reorderableListState.listState,
            contentPadding = listContentPadding,
        ) {
            itemsIndexed(
                items = tokenList,
                key = { _, item -> item.id },
            ) { index, item ->

                val onDragStart = remember(item) {
                    {
                        dragAndDropIntents.onItemDraggingStart(item)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }

                DraggableItem(
                    index = index,
                    item = item,
                    reorderableState = reorderableListState,
                    onDragStart = onDragStart,
                    isBalanceHidden = organizeTokensUM.isBalanceHidden,
                )
            }

            item {
                SpacerH(TangemTheme.dimens2.x20)
            }
        }
    }
}

@Composable
private fun LazyItemScope.DraggableItem(
    index: Int,
    item: OrganizeRowItemUM,
    reorderableState: ReorderableLazyListState,
    onDragStart: () -> Unit,
    isBalanceHidden: Boolean,
) {
    var isDragging by remember {
        mutableStateOf(value = false)
    }

    val itemModifier = Modifier.applyShapeAndShadow(item.roundingModeUM, item.isShowShadow)

    ReorderableItem(
        reorderableState = reorderableState,
        index = index,
        key = item.id,
    ) { isItemDragging ->
        isDragging = isItemDragging

        val modifierWithBackground = itemModifier
            .background(color = TangemTheme.colors.background.primary)
            .semantics { lazyListItemPosition = index }

        when (item) {
            is OrganizeRowItemUM.Network -> TangemHeaderRow(
                modifier = modifierWithBackground,
                reorderableState = reorderableState,
                headerRowUM = item.headerRowUM,
            )
            is OrganizeRowItemUM.Portfolio -> TangemHeaderRow(
                modifier = modifierWithBackground,
                headerRowUM = item.headerRowUM,
                isBalanceHidden = isBalanceHidden,
            )
            is OrganizeRowItemUM.Token -> TangemTokenRow(
                modifier = modifierWithBackground,
                tokenRowUM = item.tokenRowUM,
                reorderableState = reorderableState,
                isBalanceHidden = isBalanceHidden,
            )
            // Should be presented in the list but remain invisible
            is OrganizeRowItemUM.Placeholder -> Box(modifier = Modifier.fillMaxWidth())
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
private fun BoxScope.BottomButtons(organizeTokensUM: OrganizeTokensUM) {
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(bottom = bottomBarHeight + TangemTheme.dimens2.x4),
    ) {
        TangemButton(
            buttonUM = organizeTokensUM.cancelButton,
            modifier = Modifier.weight(1f),
        )
        TangemButton(
            buttonUM = organizeTokensUM.applyButton,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Modifier.applyShapeAndShadow(roundingMode: RoundingModeUM, showShadow: Boolean): Modifier {
    return composed {
        val radius by animateDpAsState(
            targetValue = when (roundingMode) {
                is RoundingModeUM.None -> TangemTheme.dimens2.x0
                is RoundingModeUM.All -> TangemTheme.dimens2.x3
                is RoundingModeUM.Bottom,
                is RoundingModeUM.Top,
                -> TangemTheme.dimens2.x4
            },
            label = "item_shape_radius",
        )
        val elevation by animateDpAsState(
            targetValue = if (showShadow) {
                TangemTheme.dimens2.x2
            } else {
                TangemTheme.dimens2.x0
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
private fun getItemGap(roundingMode: RoundingModeUM): PaddingValues {
    val paddingValue = TangemTheme.dimens2.x1

    return if (roundingMode.isShowGap) {
        when (roundingMode) {
            is RoundingModeUM.None -> PaddingValues(all = TangemTheme.dimens2.x0)
            is RoundingModeUM.All -> PaddingValues(vertical = paddingValue)
            is RoundingModeUM.Top -> PaddingValues(top = paddingValue)
            is RoundingModeUM.Bottom -> PaddingValues(bottom = paddingValue)
        }
    } else {
        PaddingValues(all = TangemTheme.dimens2.x0)
    }
}

@Stable
private fun getItemShape(roundingMode: RoundingModeUM, radius: Dp): Shape {
    return when (roundingMode) {
        is RoundingModeUM.None -> RectangleShape
        is RoundingModeUM.Top -> RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
        )
        is RoundingModeUM.Bottom -> RoundedCornerShape(
            bottomStart = radius,
            bottomEnd = radius,
        )
        is RoundingModeUM.All -> RoundedCornerShape(
            size = radius,
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OrganizeTokensContent_Preview(
    @PreviewParameter(OrganizeTokensContentPreviewProvider::class) params: OrganizeTokensUM,
) {
    TangemThemePreviewRedesign {
        OrganizeTokensContent(
            organizeTokensUM = params,
            dragAndDropIntents = object : DragAndDropIntents {
                override fun onItemDragged(from: ItemPosition, to: ItemPosition) {}

                override fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean = false

                override fun onItemDraggingStartLegacy(item: DraggableItem) {}

                override fun onItemDraggingStart(item: OrganizeRowItemUM) {}

                override fun onItemDraggingEnd() {}
            },
            onDismiss = {},
        )
    }
}

private class OrganizeTokensContentPreviewProvider : PreviewParameterProvider<OrganizeTokensUM> {
    override val values: Sequence<OrganizeTokensUM>
        get() = sequenceOf(
            OrganizeTokensPreview.defaultState,
            OrganizeTokensPreview.defaultState.copy(isGrouped = false),
        )
}
// endregion