package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.wallets.RenameWalletDialogContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemDimens
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState

/**
 * Wallet card
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun WalletCard(state: WalletCardState, modifier: Modifier = Modifier) {
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    CardContainer(
        name = state.title,
        onDeleteClick = { state.onDeleteClick(state.id) },
        onRenameClick = { state.onRenameClick(state.id, it) },
        modifier = modifier,
    ) {
        val (title, balance, additionalText, image) = createRefs()

        val contentVerticalMargin = TangemTheme.dimens.spacing12
        Title(
            state = state,
            modifier = Modifier.constrainAs(title) {
                start.linkTo(parent.start)
                top.linkTo(anchor = parent.top, margin = contentVerticalMargin)
                end.linkTo(image.start)
                width = Dimension.fillToConstraints
            },
        )

        val betweenContentMargin = TangemTheme.dimens.spacing8
        Balance(
            state = state,
            modifier = Modifier.constrainAs(balance) {
                start.linkTo(parent.start)
                top.linkTo(anchor = title.bottom, margin = betweenContentMargin)
                bottom.linkTo(anchor = additionalText.top, margin = betweenContentMargin)
            },
        )

        AdditionalInfo(
            state = state,
            modifier = Modifier.constrainAs(additionalText) {
                start.linkTo(parent.start)
                bottom.linkTo(anchor = parent.bottom, margin = contentVerticalMargin)
            },
        )

        val imageWidth = TangemTheme.dimens.size120
        Image(
            id = state.imageResId,
            modifier = Modifier.constrainAs(image) {
                centerVerticallyTo(parent)
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
                width = Dimension.value(imageWidth)
            },
        )
    }
}

@Composable
private fun CardContainer(
    name: String,
    onDeleteClick: () -> Unit,
    onRenameClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (ConstraintLayoutScope.() -> Unit),
) {
    var isMenuVisible by rememberSaveable { mutableStateOf(value = false) }
    var pressOffset by remember { mutableStateOf(value = DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(value = 0.dp) }

    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size108)
            .onSizeChanged { itemHeight = with(density) { it.height.toDp() } }
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
            },
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing14),
        ) {
            content()
        }
    }

    var isRenameWalletDialogVisible by rememberSaveable { mutableStateOf(value = false) }

    DropdownMenu(
        expanded = isMenuVisible,
        onDismissRequest = { isMenuVisible = false },
        modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        offset = pressOffset.copy(y = pressOffset.y - itemHeight),
    ) {
        MenuItem(
            textResId = R.string.common_rename,
            imageVector = Icons.Outlined.Edit,
            onClick = {
                isMenuVisible = false
                isRenameWalletDialogVisible = true
            },
        )
        MenuItem(textResId = R.string.common_delete, imageVector = Icons.Outlined.Delete, onClick = onDeleteClick)
    }

    if (isRenameWalletDialogVisible) {
        RenameWalletDialogContent(
            name = name,
            onConfirm = {
                onRenameClick(it)
                isRenameWalletDialogVisible = false
            },
            onDismiss = { isRenameWalletDialogVisible = false },
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
            trailingIconColor = TangemColorPalette.Dark6,
        ),
    )
}

@Composable
private fun Title(state: WalletCardState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4),
    ) {
        TitleText(title = state.title)

        AnimatedVisibility(visible = state is WalletCardState.HiddenContent, label = "Update the hidden icon") {
            Icon(
                modifier = Modifier.size(size = TangemTheme.dimens.size20),
                painter = painterResource(id = R.drawable.ic_eye_off_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Composable
private fun TitleText(title: String) {
    Text(
        text = title,
        color = TangemTheme.colors.text.tertiary,
        style = TangemTheme.typography.body2,
        maxLines = 1,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Balance(state: WalletCardState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state,
        label = "Update the balance",
        modifier = modifier,
    ) { walletCardState ->
        when (walletCardState) {
            is WalletCardState.Content -> {
                ResizableText(
                    text = walletCardState.balance,
                    fontSizeRange = FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                    modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.h2,
                )
            }
            is WalletCardState.HiddenContent -> NonContentBalanceText(text = WalletCardState.HIDDEN_BALANCE_TEXT)
            is WalletCardState.Error -> NonContentBalanceText(text = WalletCardState.EMPTY_BALANCE_TEXT)
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
    return size(width = dimens.size102, height = dimens.size32)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AdditionalInfo(state: WalletCardState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state.additionalInfo,
        label = "Update the additional text",
        modifier = modifier,
    ) { additionalInfo ->
        if (additionalInfo != null) {
            AdditionalInfoText(text = additionalInfo)
        } else {
            when (state) {
                is WalletCardState.Loading -> {
                    RectangleShimmer(modifier = Modifier.nonContentAdditionalInfoSize(TangemTheme.dimens))
                }
                is WalletCardState.LockedContent -> {
                    LockedContent(modifier = Modifier.nonContentAdditionalInfoSize(TangemTheme.dimens))
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AdditionalInfoText(text: TextReference) {
    Text(
        text = text.resolveReference(),
        color = TangemTheme.colors.text.disabled,
        style = TangemTheme.typography.caption,
    )
}

private fun Modifier.nonContentAdditionalInfoSize(dimens: TangemDimens): Modifier {
    return size(width = dimens.size84, height = dimens.size16)
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
        Image(
            painter = painterResource(id = requireNotNull(id)),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )
    }
}

// region Preview

@Preview
@Composable
private fun Preview_WalletCard_LightTheme(
    @PreviewParameter(WalletCardStateProvider::class)
    state: WalletCardState,
) {
    TangemTheme(isDark = false) {
        WalletCard(state = state)
    }
}

@Preview
@Composable
private fun Preview_WalletCard_DarkTheme(@PreviewParameter(WalletCardStateProvider::class) state: WalletCardState) {
    TangemTheme(isDark = true) {
        WalletCard(state)
    }
}

private class WalletCardStateProvider : CollectionPreviewParameterProvider<WalletCardState>(
    collection = listOf(
        WalletPreviewData.walletCardContentState,
        WalletPreviewData.walletCardLoadingState,
        WalletPreviewData.walletCardHiddenContentState,
        WalletPreviewData.walletCardErrorState,
    ),
)

// endregion Preview