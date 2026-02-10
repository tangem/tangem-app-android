package com.tangem.features.details.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.preview.PreviewUserWalletListComponent
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.entity.WalletReorderUM
import com.tangem.features.details.impl.R
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun UserWalletListBlock(state: UserWalletListUM, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val reorderableListState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to -> state.walletReorderUM.onMove(from.index, to.index) },
    )

    val listHeight = WALLET_ITEM_HEIGHT * state.userWallets.size + ADD_WALLET_BUTTON_HEIGHT

    BlockCard(
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .heightIn(max = listHeight),
        ) {
            items(
                items = state.userWallets,
                key = { it.id },
            ) { walletState ->
                WalletItem(
                    model = walletState,
                    reorderableListState = reorderableListState,
                    walletReorderUM = state.walletReorderUM,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(key = "add_wallet_button") {
                AddWalletButton(
                    text = state.addNewWalletText,
                    isInProgress = state.isWalletSavingInProgress,
                    onClick = state.onAddNewWalletClick,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun LazyItemScope.WalletItem(
    model: UserWalletItemUM,
    reorderableListState: ReorderableLazyListState,
    walletReorderUM: WalletReorderUM,
    modifier: Modifier = Modifier,
) {
    ReorderableItem(
        state = reorderableListState,
        key = model.id,
        modifier = modifier,
    ) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 9.dp else 0.dp)
        val scale by animateFloatAsState(if (isDragging) 1.04f else 1f)
        val shapeRadius by animateDpAsState(if (isDragging) 16.dp else 0.dp)

        Surface(
            shape = RoundedCornerShape(shapeRadius),
            shadowElevation = elevation,
            modifier = Modifier.scale(scale),
        ) {
            UserWalletItem(
                state = model,
                modifier = Modifier
                    .longPressDraggableHandle(
                        enabled = walletReorderUM.isDragEnabled,
                        onDragStopped = walletReorderUM.onDragStopped,
                    )
                    .background(color = TangemTheme.colors.background.primary),
            )
        }
    }
}

@Composable
private fun AddWalletButton(
    text: TextReference,
    isInProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BlockCard(
        modifier = modifier,
        onClick = onClick,
        enabled = !isInProgress,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            AnimatedContent(
                modifier = Modifier.size(TangemTheme.dimens.size36),
                targetState = isInProgress,
                label = "Add wallet progress",
            ) { isInProgress ->
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(TangemTheme.dimens.size24)
                            .padding(4.dp),
                        color = TangemTheme.colors.icon.accent,
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TangemTheme.colors.icon.accent.copy(alpha = 0.1f)),
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            tint = TangemTheme.colors.icon.accent,
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus_24),
                            contentDescription = null,
                        )
                    }
                }
            }

            Text(
                text = text.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.accent,
            )
        }
    }
}

private val WALLET_ITEM_HEIGHT = 72.dp
private val ADD_WALLET_BUTTON_HEIGHT = 60.dp

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_UserWalletListBlock(
    @PreviewParameter(UserWalletListComponentPreviewProvider::class) component: UserWalletListComponent,
) {
    TangemThemePreview {
        component.Content(Modifier)
    }
}

private class UserWalletListComponentPreviewProvider : PreviewParameterProvider<UserWalletListComponent> {
    override val values: Sequence<UserWalletListComponent>
        get() = sequenceOf(
            PreviewUserWalletListComponent(),
        )
}
// endregion Preview