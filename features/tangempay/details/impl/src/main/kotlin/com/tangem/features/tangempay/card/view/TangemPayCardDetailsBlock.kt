package com.tangem.features.tangempay.card.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintLayoutScope
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemeRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_16
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.isAwaitingActivation
import com.tangem.domain.models.pay.TangemPayCardType
import com.tangem.features.tangempay.details.impl.R
import kotlin.math.roundToInt

private const val TEXT_WIDTH_PADDING = 2
private const val FREEZE_ANIMATION_DURATION_MS = 600
private const val CARD_ASPECT_RATIO = 370f / 238f
private val CustomCardBlockColor = Color(0x1F828282)
private val CardBackgroundColor = Color(0xFF171A27)
private val CardSeparatorColor = Color(0x1AFFFFFF)
private val SeparatorThickness = 0.5.dp
private val DetailsFieldSpacing = 12.dp
private val CardContentPadding = 20.dp

@Suppress("MagicNumber")
@Composable
internal fun TangemPayCard(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    val isCardFlipped = !state.isHidden
    val animDuration = 600
    val zAxisDistance = 50f // distance between camera and Card

    // rotate Y-axis with animation
    val rotateCardY by animateFloatAsState(
        targetValue = if (isCardFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = animDuration, easing = EaseInOut),
        label = "",
    )

    // Determine which side to show based on the rotation angle
    val shouldShowDetails = rotateCardY > 90f

    CardBgWrapper(
        rotateCardY = rotateCardY,
        zAxisDistance = zAxisDistance,
        shouldShowDetails = shouldShowDetails,
        backgroundImageUrl = state.cardBackgroundImageUrl,
        modifier = modifier,
        front = { TangemPayCardDetailsHiddenBlock(state = state) },
        back = {
            TangemPayCardDetailsShownBlock(
                cardNumber = state.number,
                cardholderName = state.cardholderName,
                expiry = state.expiry,
                cvv = state.cvv,
                onCopyCardNumber = { state.onCopy(state.number, CardDataType.Number) },
                onCopyCardholderName = {
                    state.cardholderName?.let { state.onCopy(it, CardDataType.CardholderName) }
                },
                onCopyCvv = { state.onCopy(state.cvv, CardDataType.CVV) },
                onCopyExpiry = { state.onCopy(state.expiry, CardDataType.Expiry) },
                onHideDetails = state.onClick,
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            )
        },
    )
}

@Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
private fun TangemPayCardDetailsHiddenBlock(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        TangemPayCardBackground(
            modifier = Modifier
                .matchParentSize()
                .zIndex(0f),
            cardFrozenState = state.cardFrozenState,
            cardState = state.cardState,
            cardImageUrl = state.cardImageUrl,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
        ) {
            if (state.cardType != TangemPayCardType.PHYSICAL) {
                CardTopBlock()
            }

            if (state.isActionsAvailable) {
                ConstraintLayout(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = CardContentPadding)
                        .padding(bottom = CardContentPadding)
                        .fillMaxWidth(),
                ) {
                    val (displayNameRef, cardNumberRef, frozenIconRef, buttonRef) = createRefs()

                    if (state.displayNameState != null) {
                        CardDisplayName(
                            state = state.displayNameState,
                            modifier = Modifier.constrainAs(displayNameRef) {
                                start.linkTo(parent.start)
                                bottom.linkTo(cardNumberRef.top, margin = 2.dp)
                                width = Dimension.wrapContent
                            },
                        )
                    }
                    CardNumberBlock(
                        isRenaming = state.displayNameState is DisplayNameState.Editing,
                        numberShort = state.numberShort,
                        cardNumberRef = cardNumberRef,
                    )

                    when (state.cardFrozenState) {
                        TangemPayCardFrozenState.Frozen -> Unit
                        TangemPayCardFrozenState.Pending -> CircularProgressIndicator(
                            modifier = Modifier
                                .constrainAs(frozenIconRef) {
                                    start.linkTo(cardNumberRef.end, margin = 4.dp)
                                    top.linkTo(cardNumberRef.top)
                                    bottom.linkTo(cardNumberRef.bottom)
                                }
                                .size(16.dp)
                                .testTag(TangemPayTestTags.CARD_FROZEN_BADGE),
                            color = TangemTheme.colors.text.constantWhite,
                            strokeWidth = 1.dp,
                        )
                        TangemPayCardFrozenState.Unfrozen -> Unit
                    }
                    AnimatedVisibility(
                        modifier = Modifier
                            .constrainAs(buttonRef) {
                                end.linkTo(parent.end)
                                bottom.linkTo(parent.bottom)
                            }
                            .testTag(TangemPayTestTags.CARD_DETAILS_SHOW_BUTTON),
                        visible = state.isLoading || state.shouldShowCardDetailsButtonOnCard,
                    ) {
                        TangemPayCardDetailsCustomButton(
                            text = stringResourceSafe(id = R.string.tangempay_card_details_show_details),
                            onClick = state.onClick,
                            showProgress = state.isLoading,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TangemPayCardBackground(
    cardState: TangemPayCardState,
    cardFrozenState: TangemPayCardFrozenState,
    cardImageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val isFrozen = cardFrozenState == TangemPayCardFrozenState.Frozen && !cardState.isAwaitingActivation
    val freezeProgress by animateFloatAsState(
        targetValue = if (isFrozen) 1f else 0f,
        animationSpec = tween(
            durationMillis = FREEZE_ANIMATION_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "freezeProgress",
    )

    Box(modifier = modifier.fillMaxSize()) {
        AsyncImage(
            modifier = Modifier.matchParentSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(cardImageUrl.takeIf { cardState == TangemPayCardState.Active })
                .crossfade(true)
                .build(),
            placeholder = painterResource(R.drawable.img_tangem_pay_card_placeholder),
            error = painterResource(R.drawable.img_tangem_pay_card_placeholder),
            fallback = painterResource(R.drawable.img_tangem_pay_card_placeholder),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        if (isFrozen || freezeProgress > 0f) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = freezeProgress },
                painter = painterResource(R.drawable.img_tangem_pay_visa_frozen),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Suppress("MagicNumber", "LongParameterList")
@Composable
private fun CardBgWrapper(
    rotateCardY: Float,
    zAxisDistance: Float,
    shouldShowDetails: Boolean,
    backgroundImageUrl: String?,
    modifier: Modifier = Modifier,
    back: @Composable () -> Unit,
    front: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT_RATIO)
            .graphicsLayer {
                rotationY = rotateCardY
                cameraDistance = zAxisDistance
            }
            .clip(RoundedCornerShape(TangemTheme.dimens2.x5))
            .border(
                width = 1.dp,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
                color = TangemTheme.colors3.border.primary,
            )
            .background(CardBackgroundColor),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(if (shouldShowDetails) 0f else 1f)
                .graphicsLayer { alpha = if (shouldShowDetails) 0f else 1f },
        ) { front() }
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(if (shouldShowDetails) 1f else 0f)
                .graphicsLayer { alpha = if (shouldShowDetails) 1f else 0f },
        ) {
            AsyncImage(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { rotationY = 180f },
                model = ImageRequest.Builder(LocalContext.current)
                    .data(backgroundImageUrl)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.img_tangem_pay_details_placeholder),
                error = painterResource(R.drawable.img_tangem_pay_details_placeholder),
                fallback = painterResource(R.drawable.img_tangem_pay_details_placeholder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
            back()
        }
    }
}

@Composable
private fun CardTopBlock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(CardContentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_digital_card),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.staticDark.primary,
        )
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x5),
            imageVector = ImageVector.vectorResource(R.drawable.ic_cloud_fill_16),
            tint = TangemTheme.colors3.icon.staticDark,
            contentDescription = null,
        )
    }
}

@Composable
private fun ConstraintLayoutScope.CardNumberBlock(
    isRenaming: Boolean,
    numberShort: String,
    cardNumberRef: ConstrainedLayoutReference,
    modifier: Modifier = Modifier,
) {
    Text(
        text = numberShort,
        style = TangemTheme.typography3.body.medium,
        color = if (isRenaming) {
            TangemTheme.colors3.text.staticDark.secondary
        } else {
            TangemTheme.colors3.text.staticDark.primary
        },
        modifier = modifier
            .constrainAs(cardNumberRef) {
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
            }
            .testTag(TangemPayTestTags.CARD_NUMBER_SHORT),
    )
}

@Composable
private fun CardDisplayName(state: DisplayNameState, modifier: Modifier = Modifier) {
    when (state) {
        is DisplayNameState.Display -> DisplayOnlyCardDisplayName(modifier = modifier, state = state)
        is DisplayNameState.Editing -> EditingCardDisplayName(modifier = modifier, state = state)
    }
}

@Composable
private fun DisplayOnlyCardDisplayName(state: DisplayNameState.Display, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .testTag(TangemPayTestTags.CARD_NAME_EDIT_BUTTON)
            .conditional(
                condition = state.isEditingEnabled,
                modifier = { clickable(onClick = state.onClick) },
            ),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.displayName,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.staticDark.secondary,
            maxLines = 1,
        )
        if (state.isEditingEnabled) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_edit_card_20),
                contentDescription = null,
                modifier = Modifier.size(TangemTheme.dimens2.x5),
                tint = TangemTheme.colors3.icon.staticDark,
            )
        }
    }
}

@Composable
private fun EditingCardDisplayName(state: DisplayNameState.Editing, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val placeholder = stringResourceSafe(R.string.tangempay_card_edit_name_placeholder)

    val textStyle = TangemTheme.typography3.body.medium.copy(color = TangemTheme.colors3.text.staticDark.primary)
    val textMeasurer = rememberTextMeasurer()
    val measuredText = state.editingValue.text.ifEmpty { placeholder }
    val textWidthDp = with(LocalDensity.current) {
        textMeasurer.measure(measuredText, textStyle).size.width.toDp() + TEXT_WIDTH_PADDING.dp
    }

    BasicTextField(
        value = state.editingValue,
        onValueChange = state.onValueChanged,
        modifier = modifier
            .testTag(TangemPayTestTags.CARD_NAME_TEXT_FIELD)
            .width(textWidthDp.coerceAtLeast(1.dp))
            .focusRequester(focusRequester),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(TangemTheme.colors3.text.staticDark.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = if (state.isSubmitEnabled) {
                { state.onSubmit() }
            } else {
                null
            },
        ),
        decorationBox = { innerTextField ->
            Box {
                if (state.editingValue.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = TangemTheme.colors3.text.staticDark.secondary),
                    )
                }
                innerTextField()
            }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun TangemPayCardDetailsShownBlock(
    cardNumber: String,
    cardholderName: String?,
    expiry: String,
    cvv: String,
    onCopyCardNumber: () -> Unit,
    onCopyCardholderName: () -> Unit,
    onCopyExpiry: () -> Unit,
    onCopyCvv: () -> Unit,
    onHideDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(CardContentPadding),
        verticalArrangement = CardDetailsArrangement,
    ) {
        CardDetailsField(
            modifier = Modifier.fillMaxWidth(),
            title = stringResourceSafe(R.string.tangempay_card_details_card_number),
            value = cardNumber,
            onCopy = onCopyCardNumber,
            valueTestTag = TangemPayTestTags.CARD_DETAILS_NUMBER_VALUE,
            copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_NUMBER,
        )
        if (cardholderName != null) {
            CardDetailsFieldSeparator()
            CardDetailsField(
                modifier = Modifier.fillMaxWidth(),
                title = stringResourceSafe(R.string.tangempay_card_details_name_on_card),
                value = cardholderName,
                onCopy = onCopyCardholderName,
                valueTestTag = TangemPayTestTags.CARD_DETAILS_CARDHOLDER_NAME_VALUE,
                copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_CARDHOLDER_NAME,
            )
        }
        CardDetailsFieldSeparator()
        Row(horizontalArrangement = Arrangement.spacedBy(DetailsFieldSpacing)) {
            CardDetailsField(
                title = stringResourceSafe(R.string.tangempay_card_details_expiry),
                value = expiry,
                onCopy = onCopyExpiry,
                isCompact = true,
                valueTestTag = TangemPayTestTags.CARD_DETAILS_EXPIRATION_VALUE,
                copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_EXPIRATION,
            )
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .width(SeparatorThickness)
                    .background(CardSeparatorColor),
            )
            CardDetailsField(
                title = stringResourceSafe(R.string.tangempay_card_details_cvc),
                value = cvv,
                onCopy = onCopyCvv,
                isCompact = true,
                valueTestTag = TangemPayTestTags.CARD_DETAILS_CVC_VALUE,
                copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_CVC,
            )
        }
        Row {
            SpacerWMax()
            // Must use dark theme locally for button cause card is dark
            CompositionLocalProvider(LocalIsInDarkTheme provides true) {
                TangemThemeRedesign {
                    TangemButton(
                        modifier = Modifier.testTag(TangemPayTestTags.CARD_DETAILS_HIDE_BUTTON),
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X8,
                        text = resourceReference(R.string.common_close),
                        onClick = onHideDetails,
                    )
                }
            }
        }
    }
}

private object CardDetailsArrangement : Arrangement.Vertical {

    override val spacing: Dp = DetailsFieldSpacing

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        if (sizes.isEmpty()) return
        val gapCount = sizes.lastIndex.coerceAtLeast(1)
        val freeSpace = totalSize - sizes.sum()
        val squeezedGap = (freeSpace.toFloat() / gapCount).coerceIn(0f, spacing.toPx())

        var offset = 0f
        sizes.forEachIndexed { index, size ->
            outPositions[index] = offset.roundToInt()
            offset += size + squeezedGap
        }
        val lastIndex = sizes.lastIndex
        outPositions[lastIndex] = maxOf(outPositions[lastIndex], totalSize - sizes[lastIndex])
    }
}

@Suppress("LongParameterList")
@Composable
private fun CardDetailsField(
    title: String,
    value: String,
    valueTestTag: String,
    copyTestTag: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .testTag(copyTestTag)
            .clickable(
                onClick = onCopy,
                interactionSource = interactionSource,
                indication = null,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.staticDark.secondary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else DetailsFieldSpacing),
        ) {
            Text(
                modifier = Modifier
                    .then(if (isCompact) Modifier.defaultMinSize(minWidth = 56.dp) else Modifier.weight(1f))
                    .testTag(valueTestTag),
                text = value,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.staticDark.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.ic_copy_16,
                tint = TangemTheme.colors3.icon.staticDark,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun CardDetailsFieldSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SeparatorThickness)
            .background(CardSeparatorColor),
    )
}

@Composable
private fun TangemPayCardDetailsCustomButton(
    text: String,
    onClick: () -> Unit,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemButton(
        modifier = modifier,
        text = text,
        icon = TangemButtonIconPosition.None,
        onClick = onClick,
        colors = ButtonColors(
            containerColor = CustomCardBlockColor,
            contentColor = TangemTheme.colors.text.constantWhite,
            disabledContainerColor = TangemTheme.colors.text.constantWhite,
            disabledContentColor = TangemTheme.colors.text.constantWhite,
        ),
        enabled = true,
        showProgress = showProgress,
        size = TangemButtonSize.Small,
        textStyle = TangemTheme.typography.subtitle1,
    )
}

@Preview(widthDp = 400, heightDp = 700, showBackground = true)
@Composable
private fun TangemPayCardDetailsBlockPreview(
    @PreviewParameter(TangemPayCardDetailsUMProvider::class) state: TangemPayCardDetailsUM,
) {
    TangemThemePreview(isDark = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.primary),
        ) {
            TangemPayCard(
                state = state,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
            )
        }
    }
}

private class TangemPayCardDetailsUMProvider : CollectionPreviewParameterProvider<TangemPayCardDetailsUM>(
    collection = listOf(
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            cardholderName = "JOHNNY SILVERHAND",
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = { _, _ -> },
            cardImageUrl = null,
            cardBackgroundImageUrl = null,
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Frozen,
            displayNameState = DisplayNameState.Editing(
                displayName = "Tangem",
                editingValue = TextFieldValue(text = "movet", selection = TextRange("movet".length)),
                onValueChanged = {},
                isSubmitEnabled = true,
                onSubmit = {},
                onDismiss = {},
            ),
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            cardholderName = "JOHNNY SILVERHAND",
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = { _, _ -> },
            cardImageUrl = null,
            cardBackgroundImageUrl = null,
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
            displayNameState = DisplayNameState.Editing(
                displayName = "Tangem",
                editingValue = TextFieldValue(text = ""),
                onValueChanged = {},
                isSubmitEnabled = true,
                onSubmit = {},
                onDismiss = {},
            ),
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            cardholderName = "JOHNNY SILVERHAND",
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = { _, _ -> },
            cardImageUrl = null,
            cardBackgroundImageUrl = null,
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Pending,
            displayNameState = DisplayNameState.Display(
                displayName = "Tangem Pay Card",
                onClick = {},
                isEditingEnabled = true,
            ),
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            buttonText = resourceReference(R.string.tangempay_card_details_hide_details),
            onCopy = { _, _ -> },
            cardImageUrl = null,
            cardBackgroundImageUrl = null,
            isHidden = false,
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            cardholderName = "JOHNNY SILVERHAND",
            expiry = "12/34",
            cvv = "123",
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
            displayNameState = DisplayNameState.Display(
                displayName = "Tangem Pay Card",
                onClick = {},
                isEditingEnabled = false,
            ),
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            buttonText = resourceReference(R.string.tangempay_card_details_hide_details),
            onCopy = { _, _ -> },
            cardImageUrl = null,
            cardBackgroundImageUrl = null,
            isHidden = false,
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            cardholderName = null,
            expiry = "12/34",
            cvv = "123",
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
            displayNameState = DisplayNameState.Display(
                displayName = "Tangem Pay Card",
                onClick = {},
                isEditingEnabled = false,
            ),
        ),
    ),
)