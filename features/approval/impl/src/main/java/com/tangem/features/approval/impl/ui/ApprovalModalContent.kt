package com.tangem.features.approval.impl.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.*
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.R
import com.tangem.core.ui.components.HoldToConfirmButton
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.contextmenu.TangemContextMenuCheckboxItem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val LEARN_MORE_TAG = "LEARN_MORE"

/** Centered sheet subtitle that ends with a "Learn more" link. */
@Composable
internal fun ApprovalSubtitle(text: TextReference, onLearnMoreClick: () -> Unit, modifier: Modifier = Modifier) {
    val subtitle = text.resolveReference()
    val learnMore = stringResourceSafe(R.string.common_learn_more)
    val textColor = TangemTheme.colors3.text.secondary
    val linkColor = TangemTheme.colors3.text.primary
    val annotated = remember(subtitle, learnMore, textColor, linkColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = textColor)) { append(subtitle.trimEnd()) }
            append(" ")
            withLink(LinkAnnotation.Clickable(tag = LEARN_MORE_TAG, linkInteractionListener = { onLearnMoreClick() })) {
                withStyle(SpanStyle(color = linkColor)) { append(learnMore) }
            }
        }
    }
    Text(
        text = annotated,
        style = TangemTheme.typography3.subheading.medium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
}

/** "Amount <symbol>" row whose value opens a menu to pick the [ApproveType]. */
@Composable
internal fun ApprovalAmountRow(
    currency: String,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var offset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    Box(modifier = modifier) {
        ApprovalRow(
            title = resourceReference(R.string.give_permission_rows_amount, wrappedList(currency)),
            onClick = { isMenuExpanded = true },
            modifier = Modifier.onGloballyPositioned { coords ->
                val position = coords.parentLayoutCoordinates?.boundsInParent()?.topCenter
                if (position != null) {
                    offset = with(density) {
                        DpOffset(
                            x = position.x.toDp(),
                            y = position.y.dp - coords.size.height.dp,
                        )
                    }
                }
            },
        ) {
            Text(
                text = approveType.text.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.tertiary,
                maxLines = 1,
            )
            SelectIcon()
        }
        TangemContextMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier = Modifier.align(Alignment.BottomEnd),
            offset = offset,
        ) {
            approveItems.forEach { item ->
                TangemContextMenuCheckboxItem(
                    width = Dp.Unspecified,
                    title = item.text,
                    isChecked = item == approveType,
                    onClick = {
                        isMenuExpanded = false
                        onChangeApproveType(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun ApprovalRow(
    title: TextReference,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors3.bg.primary)
            .clickable(onClick = onClick)
            .heightIn(min = 46.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) { value() }
    }
}

@Composable
private fun SelectIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_select_18_24),
        contentDescription = null,
        tint = TangemTheme.colors3.icon.tertiary,
        modifier = Modifier.size(width = 18.dp, height = 24.dp),
    )
}

/** The rows of an approval sheet: amount type, then an optional fee row and its note. */
@Composable
internal fun ApprovalRows(
    modifier: Modifier = Modifier,
    feeNote: TextReference? = null,
    feeRow: (@Composable (Modifier) -> Unit)? = null,
    amountRow: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        amountRow()
        if (feeRow != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                feeRow(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .intrinsicsFromLastMeasure(),
                )
                if (feeNote != null) {
                    Text(
                        text = feeNote.resolveReference(),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.tertiary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

/** Full-width confirm button of an approval sheet; hot wallets confirm by holding it. */
@Composable
internal fun ApprovalConfirmButton(
    text: TextReference,
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHoldToConfirm: Boolean = false,
    @DrawableRes walletInteractionIcon: Int? = null,
) {
    val buttonModifier = modifier
        .fillMaxWidth()
        .padding(16.dp)
    // DS3 has no hold-to-confirm button yet, so hot wallets keep the DS1 one.
    if (isHoldToConfirm) {
        HoldToConfirmButton(
            text = text.resolveReference(),
            onConfirm = onClick,
            enabled = isEnabled,
            isLoading = isLoading,
            modifier = buttonModifier,
        )
    } else {
        TangemButton(
            text = text,
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            isEnabled = isEnabled,
            isLoading = isLoading,
            iconEnd = walletInteractionIcon?.let { TangemIconUM.Icon(ImageVector.vectorResource(it)) },
            onClick = onClick,
            modifier = buttonModifier,
        )
    }
}

// region TEMPORARY WORKAROUND FOR FEE SELECTOR
private const val UNMEASURED = 0

/**
 * Answers intrinsic height queries with the height of the last measure and never forwards them to the child.
 *
 * The legacy fee block contains `EllipsisText`, a `SubcomposeLayout`, which throws when
 * [com.tangem.core.ui.ds2.modal.TangemModal] probes the content's `minIntrinsicHeight`. Before the first
 * measure the row reports 0; the modal's `reportMeasuredHeight` then re-measures the card with the real height.
 */
private fun Modifier.intrinsicsFromLastMeasure(): Modifier = this then IntrinsicsFromLastMeasureElement

private object IntrinsicsFromLastMeasureElement : ModifierNodeElement<IntrinsicsFromLastMeasureNode>() {

    override fun create() = IntrinsicsFromLastMeasureNode()

    override fun update(node: IntrinsicsFromLastMeasureNode) = Unit

    override fun hashCode(): Int = "intrinsicsFromLastMeasure".hashCode()

    override fun equals(other: Any?): Boolean = other === this
}

private class IntrinsicsFromLastMeasureNode : LayoutModifierNode, Modifier.Node() {

    private var measuredHeight by mutableIntStateOf(UNMEASURED)

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        if (measuredHeight != placeable.height) measuredHeight = placeable.height
        return layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measuredHeight

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int): Int =
        measuredHeight

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurable: IntrinsicMeasurable, height: Int): Int = 0
}

// endregion

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ApprovalModalContent_Preview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.secondary)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            ApprovalSubtitle(
                text = resourceReference(R.string.give_permission_swap_subtitle_v2, wrappedList("")),
                onLearnMoreClick = {},
            )
            ApprovalRows(
                amountRow = {
                    ApprovalAmountRow(
                        currency = "USDT",
                        approveType = ApproveType.UNLIMITED,
                        approveItems = ApproveType.entries.toImmutableList(),
                        onChangeApproveType = {},
                    )
                    ApprovalAmountRow(
                        currency = "USDT",
                        approveType = ApproveType.UNLIMITED,
                        approveItems = ApproveType.entries.toImmutableList(),
                        onChangeApproveType = {},
                    )
                },
            )
            ApprovalConfirmButton(
                text = resourceReference(R.string.common_confirm),
                isEnabled = true,
                isLoading = false,
                onClick = {},
            )
        }
    }
}