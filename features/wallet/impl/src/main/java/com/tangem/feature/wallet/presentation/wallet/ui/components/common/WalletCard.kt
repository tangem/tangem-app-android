package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import android.content.res.Configuration
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.*
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemDimens
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletDropDownItems
import com.tangem.feature.wallet.presentation.wallet.ui.components.fastForEach
import kotlinx.collections.immutable.ImmutableList

private const val HALF_OF_ITEM_WIDTH = 0.5

/**
 * Wallet card
 *
 * @param state    state
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongMethod")
@Composable
internal fun WalletCard(state: WalletCardState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
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
            .testTag(MainScreenTestTags.TOTAL_BALANCE_CONTAINER)
            .then(
                if (state is WalletCardState.LockedContent || state.dropDownItems.isEmpty()) {
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.primary)
                .padding(horizontal = TangemTheme.dimens.spacing12),
        ) {
            CardContainer(
                state = state,
                isBalanceHidden = isBalanceHidden,
                itemSize = itemSize,
            )
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
        dropDownItems = state.dropDownItems,
    )
}

@Suppress("DestructuringDeclarationWithTooManyEntries")
@Composable
private fun CardContainer(state: WalletCardState, isBalanceHidden: Boolean, itemSize: IntSize) {
    var balanceWidth by remember { mutableIntStateOf(value = Int.MIN_VALUE) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            TitleText(
                text = state.title,
                modifier = Modifier,
            )
            Balance(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .onSizeChanged { balanceWidth = it.width }
                    .padding(vertical = TangemTheme.dimens.spacing8),
            )

            val additionalText by remember(state.additionalInfo, isBalanceHidden) {
                mutableStateOf(
                    state.additionalInfo?.content?.orMaskWithStars(
                        maskWithStars = state.additionalInfo?.hideable == true && isBalanceHidden,
                    ),
                )
            }
            AdditionalInfo(
                text = additionalText,
                modifier = Modifier.conditional(
                    state.imageResId == null,
                ) { fillMaxWidth() },
            )
        }

        // If balance has a large width then image must be hidden
        val hasSpaceForImage by remember(key1 = balanceWidth, key2 = itemSize.width) {
            mutableStateOf(value = balanceWidth < itemSize.width * HALF_OF_ITEM_WIDTH)
        }

        if (hasSpaceForImage) {
            Image(
                id = state.imageResId,
                modifier = Modifier.wrapContentWidth(),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ManageWalletContextMenu(
    isMenuVisible: Boolean,
    pressOffset: DpOffset,
    itemHeight: Dp,
    onDismissRequest: () -> Unit,
    dropDownItems: ImmutableList<WalletDropDownItems>,
) {
    DropdownMenu(
        expanded = isMenuVisible,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        offset = pressOffset.copy(y = pressOffset.y - itemHeight),
    ) {
        dropDownItems.fastForEach { item ->
            MenuItem(
                text = item.text,
                imageVector = ImageVector.vectorResource(id = item.icon),
                onClick = {
                    onDismissRequest()
                    item.onClick()
                },
            )
        }
    }
}

@Composable
private fun MenuItem(text: TextReference, imageVector: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = text.resolveReference(), style = TangemTheme.typography.subtitle2) },
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .testTag(MainScreenTestTags.TOTAL_BALANCE_MENU_ITEM),
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
        modifier = modifier.testTag(MainScreenTestTags.CARD_TITLE),
        color = TangemTheme.colors.text.tertiary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.button,
    )
}

@Composable
private fun Balance(state: WalletCardState, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = (state as? WalletCardState.Content)?.balance?.orMaskWithStars(isBalanceHidden).orEmpty(),
        label = "Update the balance",
        modifier = modifier.testTag(MainScreenTestTags.WALLET_BALANCE),
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 90)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = 90))
        },
    ) { balance ->
        when (state) {
            is WalletCardState.Content -> {
                Text(
                    text = balance,
                    modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32),
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 16.sp,
                        maxFontSize = TangemTheme.typography.h2.fontSize,
                    ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TangemTheme.typography.h2
                        .applyBladeBrush(
                            isEnabled = state.isBalanceFlickering,
                            textColor = TangemTheme.colors.text.primary1,
                        ),
                )
            }
            is WalletCardState.Error -> NonContentBalanceText(
                text = WalletCardState.EMPTY_BALANCE_TEXT.orMaskWithStars(isBalanceHidden),
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
        modifier = Modifier.testTag(MainScreenTestTags.DEVICES_COUNT),
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
            modifier = Modifier
                .width(width = TangemTheme.dimens.size120)
                .testTag(MainScreenTestTags.CARD_IMAGE),
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
        WalletPreviewData.walletCardContentState.copy(
            balance = "0.00",
        ),
        WalletPreviewData.walletCardContentState.copy(
            title = "Title",
            additionalInfo = WalletAdditionalInfo(
                hideable = false,
                content = TextReference.Str("3 cards"),
            ),
        ),
        WalletPreviewData.walletCardContentState.copy(
            isBalanceFlickering = true,
        ),
        WalletPreviewData.walletCardLoadingState,
        WalletPreviewData.walletCardErrorState,
    ),
)

// endregion Preview