package com.tangem.feature.wallet.child.tokenActions

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.contextmenu.CenteredContextMenuPositionProvider
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.internal.TangemTokenRowPreviewData
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenActionButtonUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TokenActionContent(
    tokenRowUM: TangemTokenRowUM,
    isBalanceHidden: Boolean,
    offset: DpOffset,
    actions: ImmutableList<TokenActionButtonUM>,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    var anchorShiftPx by remember { mutableIntStateOf(0) }
    val anchorShiftDp = with(density) { anchorShiftPx.toDp() }
    val animatedShift by animateDpAsState(
        targetValue = anchorShiftDp,
        animationSpec = tween(),
        label = "AnchorShift",
    )

    Box(modifier.fillMaxSize()) {
        Box(modifier = Modifier.offset(y = offset.y - animatedShift)) {
            TangemTokenRow(
                tokenRowUM = tokenRowUM,
                isBalanceHidden = isBalanceHidden,
                reorderableState = null,
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens2.x3)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TangemTheme.colors2.surface.level3),
            )
            TangemContextMenu(
                expanded = true,
                onDismissRequest = onDismiss,
                positionProvider = remember(density) {
                    CenteredContextMenuPositionProvider(
                        contentOffset = DpOffset(x = 0.dp, y = 12.dp),
                        density = density,
                        onAnchorShiftRequired = { shift ->
                            if (anchorShiftPx == 0 && shift > 0) {
                                anchorShiftPx = shift
                            }
                        },
                    )
                },
            ) {
                TokenActionContextMenuContent(
                    actions = actions,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun TokenActionContextMenuContent(actions: ImmutableList<TokenActionButtonUM>, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(min = 206.dp)
            .padding(
                vertical = TangemTheme.dimens2.x2_5,
                horizontal = TangemTheme.dimens2.x4,
            ),
    ) {
        actions.fastForEach { item ->
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                    modifier = Modifier
                        .clickable(
                            enabled = item.isEnabled,
                            onClick = {
                                item.onClick()
                                onDismiss()
                            },
                        )
                        .padding(
                            start = TangemTheme.dimens2.x1_5,
                            end = TangemTheme.dimens2.x2,
                            top = TangemTheme.dimens2.x2_5,
                            bottom = TangemTheme.dimens2.x2_5,
                        ),
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(item.iconResId),
                        contentDescription = null,
                        tint = if (item.isWarning) {
                            TangemTheme.colors2.graphic.status.warning
                        } else {
                            TangemTheme.colors2.graphic.neutral.primary
                        },
                        modifier = Modifier.size(TangemTheme.dimens2.x5),
                    )
                    Text(
                        text = item.text.resolveReference(),
                        style = TangemTheme.typography2.headingRegular17,
                        color = if (item.isWarning) {
                            TangemTheme.colors2.text.status.warning
                        } else {
                            TangemTheme.colors2.text.neutral.primary
                        },
                    )
                }
                if (item.hasDivider) {
                    Spacer(
                        modifier = Modifier
                            .padding(
                                vertical = TangemTheme.dimens2.x2_5,
                                horizontal = TangemTheme.dimens2.x2,
                            )
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TangemTheme.colors2.border.neutral.primary),
                    )
                }
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenActionContent_Preview() {
    TangemThemePreviewRedesign {
        TokenActionContent(
            tokenRowUM = TangemTokenRowPreviewData.tokenState,
            offset = DpOffset(
                x = 100.dp,
                y = 100.dp,
            ),
            actions = persistentListOf(
                TokenActionButtonUM(
                    id = "Send",
                    text = stringReference("Send"),
                    iconResId = R.drawable.ic_arrow_up_24,
                    isEnabled = true,
                    isWarning = false,
                    hasDivider = false,
                    onClick = {},
                ),
                TokenActionButtonUM(
                    id = "Receive",
                    text = stringReference("Receive"),
                    iconResId = R.drawable.ic_arrow_down_24,
                    isEnabled = true,
                    isWarning = false,
                    hasDivider = false,
                    onClick = {},
                ),
                TokenActionButtonUM(
                    id = "Swap",
                    text = stringReference("Swap"),
                    iconResId = R.drawable.ic_exchange_vertical_24,
                    isEnabled = true,
                    isWarning = false,
                    hasDivider = true,
                    onClick = {},
                ),
                TokenActionButtonUM(
                    id = "Remove",
                    text = stringReference("Remove"),
                    iconResId = R.drawable.ic_trash_24,
                    isEnabled = true,
                    isWarning = true,
                    hasDivider = false,
                    onClick = {},
                ),
            ),
            isBalanceHidden = false,
            onDismiss = {},
        )
    }
}
// endregion