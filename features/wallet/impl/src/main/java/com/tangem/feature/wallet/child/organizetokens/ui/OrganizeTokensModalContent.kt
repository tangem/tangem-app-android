package com.tangem.feature.wallet.child.organizetokens.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRow
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.draggable.TangemDraggable
import com.tangem.core.ui.ds2.draggable.TangemDraggableState
import com.tangem.core.ui.ds2.draggable.rememberTangemDraggableState
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.tokenrow.Organize
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.stringResourceSafe
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
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import org.burnoutcrew.reorderable.ItemPosition

@Composable
internal fun OrganizeTokensModalContent(
    organizeTokensUM: OrganizeTokensUM,
    dragAndDropIntents: DragAndDropIntents,
    onDismiss: () -> Unit,
) {
    val topBarHeight = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = onDismiss,
        containerColor = TangemTheme.colors3.bg.primary,
        scrollableContent = false,
        chromePlacement = TangemModal.ChromePlacement.Overlay,
        title = {
            TopBar(
                organizeTokensUM = organizeTokensUM,
                modifier = Modifier.onSizeChanged { size ->
                    topBarHeight.value = with(density) { size.height.toDp() }
                },
            )
        },
        content = {
            TokenList(
                organizeTokensUM = organizeTokensUM,
                dragAndDropIntents = dragAndDropIntents,
                headerHeight = ModalDragIndicatorHeight + topBarHeight.value + HeaderBottomPadding,
                modifier = Modifier
                    .weight(1f)
                    .fullHeightIntrinsics(),
            )
            ApplyButton(applyButton = organizeTokensUM.applyButton)
        },
    )
}

@Composable
private fun TopBar(organizeTokensUM: OrganizeTokensUM, modifier: Modifier = Modifier) {
    var isShowDropdownMenu by rememberSaveable { mutableStateOf(false) }

    TangemModal.TopNavigation(
        modifier = modifier,
        startButton = {
            Box {
                TangemButton(
                    modifier = Modifier.testTag(OrganizeTokensScreenTestTags.MENU_BUTTON),
                    variant = TangemButton.Variant.Material,
                    size = TangemButton.Size.X11,
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_exchange_mini_24),
                    onClick = { isShowDropdownMenu = true },
                )
                OrganizeDropDownMenu(
                    organizeMenuUM = organizeTokensUM.organizeMenuUM,
                    showDropdownMenu = isShowDropdownMenu,
                    onDropdownDismiss = { isShowDropdownMenu = false },
                )
            }
        },
        contentColumn = {
            TangemNavigationText(
                text = stringResourceSafe(R.string.organize_tokens_title),
                role = TangemNavigationText.Role.Title,
            )
        },
        endButton = {
            TangemButton.Close(
                modifier = Modifier.testTag(OrganizeTokensScreenTestTags.CANCEL_BUTTON),
                isEnabled = organizeTokensUM.cancelButton.isEnabled,
                onClick = organizeTokensUM.cancelButton.onClick,
            )
        },
    )
}

@Composable
private fun TokenList(
    organizeTokensUM: OrganizeTokensUM,
    dragAndDropIntents: DragAndDropIntents,
    headerHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val tokenList = organizeTokensUM.tokenList
    val listState = rememberLazyListState()
    val draggableState = rememberTangemDraggableState(
        listState = listState,
        onMove = dragAndDropIntents::onItemMoved,
        onDragStart = { key ->
            tokenList.firstOrNull { it.id == key }?.let(dragAndDropIntents::onItemDraggingStart)
        },
        onDragEnd = dragAndDropIntents::onItemDraggingEnd,
    )
    val draggingKey = draggableState.draggingKey
    val draggingItem = remember(draggingKey, tokenList) { tokenList.firstOrNull { it.id == draggingKey } }
    val blockColor = TangemTheme.colors3.bg.secondary

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSourceTangem()
                .testTag(OrganizeTokensScreenTestTags.TOKENS_LAZY_LIST),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = headerHeight + 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            itemsIndexed(
                items = tokenList,
                key = { _, item -> item.id },
            ) { index, item ->
                OrganizeItem(
                    item = item,
                    draggableState = draggableState,
                    isValidDropTarget = isValidDropTarget(
                        item = item,
                        dragging = draggingItem,
                        isGrouped = organizeTokensUM.isGrouped,
                        isAccountsMode = organizeTokensUM.isAccountsMode,
                    ),
                    isBalanceHidden = organizeTokensUM.isBalanceHidden,
                    modifier = Modifier
                        .padding(getBlockGap(item.roundingModeUM))
                        .conditional(item.id != draggingKey) {
                            background(color = blockColor, shape = getBlockShape(item.roundingModeUM))
                        }
                        .semantics { lazyListItemPosition = index },
                )
            }
        }
        HeaderBlur(modifier = Modifier.height(headerHeight))
    }
}

@Composable
private fun HeaderBlur(modifier: Modifier = Modifier) {
    val fallbackColor = TangemTheme.colors3.bg.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .hazeEffectTangem(
                style = HazeStyle(
                    backgroundColor = Color.Transparent,
                    tints = listOf(HazeTint(Color.Transparent)),
                    blurRadius = 16.dp,
                    noiseFactor = 0f,
                ),
            ) {
                fallbackTint = HazeTint(fallbackColor)
                progressive = HazeProgressive.verticalGradient(
                    startIntensity = 1f,
                    endIntensity = 0f,
                    preferPerformance = true,
                )
            },
    )
}

@Composable
private fun LazyItemScope.OrganizeItem(
    item: OrganizeRowItemUM,
    draggableState: TangemDraggableState,
    isValidDropTarget: Boolean,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is OrganizeRowItemUM.Token -> TangemDraggable(
            state = draggableState,
            key = item.id,
            enabled = isValidDropTarget,
            modifier = modifier,
        ) {
            val tokenRowState = item.tokenRowState
            TangemTokenRow.Organize(
                icon = tokenRowState.icon,
                title = tokenRowState.title,
                ticker = tokenRowState.ticker,
                fiatBalance = tokenRowState.fiatBalance,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.testTag(OrganizeTokensScreenTestTags.TOKEN_LIST_ITEM),
            )
        }
        is OrganizeRowItemUM.Network -> TangemDraggable(
            state = draggableState,
            key = item.id,
            enabled = isValidDropTarget,
            modifier = modifier,
        ) {
            TangemHeaderRow(
                headerRowUM = item.headerRowUM,
                modifier = Modifier.testTag(OrganizeTokensScreenTestTags.GROUP_TITLE_ITEM),
            )
        }
        is OrganizeRowItemUM.Portfolio -> TangemHeaderRow(
            headerRowUM = item.headerRowUM,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
        )
        is OrganizeRowItemUM.Placeholder -> TangemDraggable(
            state = draggableState,
            key = item.id,
            enabled = isValidDropTarget,
            modifier = modifier,
        ) {
            Box(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getBlockGap(roundingMode: RoundingModeUM): PaddingValues {
    if (!roundingMode.isShowGap) return PaddingValues(all = 0.dp)

    return when (roundingMode) {
        is RoundingModeUM.None -> PaddingValues(all = 0.dp)
        is RoundingModeUM.All -> PaddingValues(vertical = BlockHalfGap)
        is RoundingModeUM.Top -> PaddingValues(top = BlockHalfGap)
        is RoundingModeUM.Bottom -> PaddingValues(bottom = BlockHalfGap)
    }
}

private fun getBlockShape(roundingMode: RoundingModeUM): Shape = when (roundingMode) {
    is RoundingModeUM.None -> RectangleShape
    is RoundingModeUM.All -> RoundedCornerShape(BlockRadius)
    is RoundingModeUM.Top -> RoundedCornerShape(topStart = BlockRadius, topEnd = BlockRadius)
    is RoundingModeUM.Bottom -> RoundedCornerShape(bottomStart = BlockRadius, bottomEnd = BlockRadius)
}

private val BlockHalfGap = 8.dp
private val ModalDragIndicatorHeight = 16.dp
private val HeaderBottomPadding = 16.dp
private val BlockRadius = 24.dp

@Composable
private fun ApplyButton(applyButton: TangemButtonUM) {
    TangemFooter(
        windowInsets = WindowInsets(0),
        fadeEnabled = false,
        blurBackground = false,
    ) {
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OrganizeTokensScreenTestTags.APPLY_BUTTON),
            onClick = applyButton.onClick,
            text = applyButton.text,
            size = TangemButton.Size.X12,
            isEnabled = applyButton.isEnabled,
        )
    }
}

@Composable
private fun Modifier.fullHeightIntrinsics(): Modifier {
    val windowHeight = LocalWindowInfo.current.containerSize.height
    return this then FullHeightIntrinsicsElement(windowHeight)
}

private data class FullHeightIntrinsicsElement(
    val height: Int,
) : ModifierNodeElement<FullHeightIntrinsicsNode>() {

    override fun create() = FullHeightIntrinsicsNode(height)

    override fun update(node: FullHeightIntrinsicsNode) {
        node.height = height
    }
}

private class FullHeightIntrinsicsNode(var height: Int) : LayoutModifierNode, Modifier.Node() {

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = height

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int = height

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OrganizeTokensModalContent_Preview() {
    TangemThemePreviewRedesign {
        OrganizeTokensModalContent(
            organizeTokensUM = OrganizeTokensPreview.defaultState,
            dragAndDropIntents = object : DragAndDropIntents {
                override fun onItemDragged(from: ItemPosition, to: ItemPosition) {}

                override fun onItemMoved(from: Int, to: Int) {}

                override fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean = false

                override fun onItemDraggingStartLegacy(item: DraggableItem) {}

                override fun onItemDraggingStart(item: OrganizeRowItemUM) {}

                override fun onItemDraggingEnd() {}
            },
            onDismiss = {},
        )
    }
}
// endregion