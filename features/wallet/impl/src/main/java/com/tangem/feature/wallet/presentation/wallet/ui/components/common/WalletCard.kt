package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemDimens
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.utils.StringsSigns

private const val HALF_OF_ITEM_WIDTH = 0.5

/**
 * Wallet card
 *
 * @param state    state
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@Suppress("LongMethod")
@Composable
internal fun WalletCard(state: WalletCardState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    CardContainer(
        onDeleteClick = { state.onDeleteClick(state.id) },
        onRenameClick = { state.onRenameClick(state.id) },
        isLockedState = state is WalletCardState.LockedContent,
        modifier = modifier,
    ) { itemSize ->
        val (titleRef, balanceRef, additionalTextRef, imageRef) = createRefs()

        val contentVerticalMargin = TangemTheme.dimens.spacing12
        TitleText(
            text = state.title,
            modifier = Modifier.constrainAs(titleRef) {
                start.linkTo(parent.start)
                top.linkTo(anchor = parent.top, margin = contentVerticalMargin)
                end.linkTo(imageRef.start)
                width = Dimension.fillToConstraints
            },
        )

        var balanceWidth by remember { mutableIntStateOf(value = Int.MIN_VALUE) }
        Balance(
            state = state,
            isBalanceHidden = isBalanceHidden,
            modifier = Modifier
                .onSizeChanged { balanceWidth = it.width }
                .padding(vertical = TangemTheme.dimens.spacing8)
                .constrainAs(balanceRef) {
                    start.linkTo(parent.start)
                    top.linkTo(anchor = titleRef.bottom)
                    bottom.linkTo(anchor = additionalTextRef.top)
                },
        )

        val additionalText by remember(state.additionalInfo, isBalanceHidden) {
            mutableStateOf(
                if (state.additionalInfo?.hideable == true && isBalanceHidden) {
                    WalletCardState.HIDDEN_BALANCE_TEXT
                } else {
                    state.additionalInfo?.content
                },
            )
        }
        AdditionalInfo(
            text = additionalText,
            modifier = Modifier.constrainAs(additionalTextRef) {
                start.linkTo(parent.start)
                top.linkTo(balanceRef.bottom)
                bottom.linkTo(anchor = parent.bottom, margin = contentVerticalMargin)

                if (additionalText != null) {
                    width = if (state.imageResId != null) {
                        end.linkTo(imageRef.start)
                        Dimension.fillToConstraints
                    } else {
                        Dimension.wrapContent
                    }
                }
            },
        )

        // If balance has a large width then image must be hidden
        val hasSpaceForImage by remember(key1 = balanceWidth, key2 = itemSize.width) {
            mutableStateOf(value = balanceWidth < itemSize.width * HALF_OF_ITEM_WIDTH)
        }

        if (hasSpaceForImage) {
            Image(
                id = state.imageResId,
                modifier = Modifier.constrainAs(imageRef) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                },
            )
        }
    }
}

@Composable
private fun CardContainer(
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    isLockedState: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (ConstraintLayoutScope.(IntSize) -> Unit),
) {
    var isMenuVisible by rememberSaveable { mutableStateOf(value = false) }
    var pressOffset by remember { mutableStateOf(value = DpOffset.Zero) }
    var itemSize by remember { mutableStateOf(value = IntSize.Zero) }

    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size108)
            .onSizeChanged { itemSize = it }
            .then(
                if (isLockedState) {
                    Modifier
                } else {
                    Modifier
                        .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
                        .indication(interactionSource = interactionSource, indication = LocalIndication.current)
                        .pointerInput(true) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isMenuVisible = true
                                    pressOffset = DpOffset(x = it.x.toDp(), y = it.y.toDp())
                                },
                                onPress = {
                                    val press = PressInteraction.Press(it)
                                    interactionSource.emit(press)
                                    tryAwaitRelease()
                                    interactionSource.emit(PressInteraction.Release(press))
                                },
                            )
                        }
                },
            ),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing12),
        ) {
            content(itemSize)
        }
    }

    val itemHeight by remember(itemSize.height) {
        mutableStateOf(value = with(density) { itemSize.height.toDp() })
    }
    ManageWalletContextMenu(
        isMenuVisible = isMenuVisible,
        pressOffset = pressOffset,
        itemHeight = itemHeight,
        onDismissRequest = { isMenuVisible = false },
        onShowRenameWalletDialogClick = onRenameClick,
        onDeleteClick = onDeleteClick,
    )
}

@Suppress("LongParameterList")
@Composable
private fun ManageWalletContextMenu(
    isMenuVisible: Boolean,
    pressOffset: DpOffset,
    itemHeight: Dp,
    onDismissRequest: () -> Unit,
    onShowRenameWalletDialogClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    DropdownMenu(
        expanded = isMenuVisible,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        offset = pressOffset.copy(y = pressOffset.y - itemHeight),
    ) {
        MenuItem(
            textResId = R.string.common_rename,
            imageVector = Icons.Outlined.Edit,
            onClick = {
                onDismissRequest()
                onShowRenameWalletDialogClick()
            },
        )
        MenuItem(
            textResId = R.string.common_delete,
            imageVector = Icons.Outlined.Delete,
            onClick = {
                onDismissRequest()
                onDeleteClick()
            },
        )
    }
}

@Composable
private fun MenuItem(@StringRes textResId: Int, imageVector: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(id = textResId), style = TangemTheme.typography.subtitle2) },
        modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        trailingIcon = { Icon(imageVector = imageVector, contentDescription = null) },
        onClick = onClick,
        colors = MenuDefaults.itemColors(
            textColor = TangemTheme.colors.text.primary1,
            trailingIconColor = TangemTheme.colors.icon.primary1,
        ),
    )
}

@Composable
private fun TitleText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.button,
    )
}

@Composable
private fun Balance(state: WalletCardState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state,
        label = "Update the balance",
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) { walletCardState ->
        when (walletCardState) {
            is WalletCardState.Content -> {
                ResizableText(
                    text = if (isBalanceHidden) StringsSigns.STARS else walletCardState.balance,
                    fontSizeRange = FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                    modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32),
                    color = TangemTheme.colors.text.primary1,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TangemTheme.typography.h2,
                )
            }
            is WalletCardState.Error -> NonContentBalanceText(
                text = if (isBalanceHidden) WalletCardState.HIDDEN_BALANCE_TEXT else WalletCardState.EMPTY_BALANCE_TEXT,
            )
            is WalletCardState.Loading -> {
                RectangleShimmer(modifier = Modifier.nonContentBalanceSize(TangemTheme.dimens))
            }
            is WalletCardState.LockedContent -> {
                LockedContent(modifier = Modifier.nonContentBalanceSize(TangemTheme.dimens))
            }
        }
    }
}

@Composable
private fun NonContentBalanceText(text: TextReference) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors.text.primary1,
        style = TangemTheme.typography.h2,
    )
}

private fun Modifier.nonContentBalanceSize(dimens: TangemDimens): Modifier {
    return this
        .padding(vertical = dimens.spacing4)
        .size(width = dimens.size102, height = dimens.size24)
}

@Composable
private fun AdditionalInfo(text: TextReference?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = text,
        label = "Update the additional text",
        modifier = modifier,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) { animatedText ->
        if (animatedText != null) {
            AdditionalInfoText(text = animatedText)
        } else {
            RectangleShimmer(modifier = Modifier.nonContentAdditionalInfoSize(dimens = TangemTheme.dimens))
        }
    }
}

@Composable
private fun AdditionalInfoText(text: TextReference) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.caption2,
    )
}

private fun Modifier.nonContentAdditionalInfoSize(dimens: TangemDimens): Modifier {
    return this.size(width = dimens.size84, height = dimens.size16)
}

@Composable
private fun LockedContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.field.primary,
            shape = RoundedCornerShape(TangemTheme.dimens.radius6),
        ),
    )
}

@Composable
private fun Image(@DrawableRes id: Int?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = id != null, modifier = modifier) {
        val imageRes = id ?: return@AnimatedVisibility

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier.width(width = TangemTheme.dimens.size120),
            contentScale = ContentScale.FillWidth,
        )
    }
}

// region Preview

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WalletCard(
    @PreviewParameter(WalletCardStateProvider::class)
    state: WalletCardState,
) {
    TangemThemePreview {
        WalletCard(state = state, isBalanceHidden = false)
    }
}

private class WalletCardStateProvider : CollectionPreviewParameterProvider<WalletCardState>(
    collection = listOf(
        WalletPreviewData.walletCardContentState,
        WalletPreviewData.walletCardLoadingState,
        WalletPreviewData.walletCardErrorState,
    ),
)

// endregion Preview
