package com.tangem.features.tangempay.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.DisplayNameState
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.model.CardDataType

private const val TEXT_WIDTH_PADDING = 2
private val CustomCardBlockColor = Color(0x1F828282)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(328f / 212f) // size of img_tangem_pay_visa
            .graphicsLayer {
                rotationY = rotateCardY
                cameraDistance = zAxisDistance
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color(red = 18, green = 21, blue = 31))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        TangemTheme.colors.text.constantWhite.copy(alpha = 0.1F),
                        TangemTheme.colors.text.constantWhite.copy(alpha = 0f),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        if (shouldShowDetails && state.isActive) {
            TangemPayCardDetailsShownBlock(
                cardNumber = state.number,
                expiry = state.expiry,
                cvv = state.cvv,
                onCopyCardNumber = { state.onCopy(state.number, CardDataType.Number) },
                onCopyCvv = { state.onCopy(state.cvv, CardDataType.CVV) },
                onCopyExpiry = { state.onCopy(state.expiry, CardDataType.Expiry) },
                onHideDetails = state.onClick,
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            )
        } else {
            TangemPayCardDetailsHiddenBlock(
                state = state,
            )
        }
    }
}

@Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
private fun TangemPayCardDetailsHiddenBlock(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        val imageResId = when (state.cardFrozenState) {
            is TangemPayCardFrozenState.Frozen -> R.drawable.img_tangem_pay_visa_frozen
            else -> R.drawable.img_tangem_pay_visa
        }
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = imageResId),
            contentDescription = null,
        )

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cloud_fill_16),
                tint = TangemTheme.colors.icon.constant,
                contentDescription = null,
            )
            SpacerW4()
            Text(
                text = stringResourceSafe(R.string.tangempay_digital_card),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.constantWhite,
            )
        }

        if (state.isActive) {
            ConstraintLayout(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(),
            ) {
                val (displayNameRef, cardNumberRef, frozenIconRef, buttonRef) = createRefs()

                if (state.displayNameState != null) {
                    CardDisplayName(
                        state = state.displayNameState,
                        modifier = Modifier.constrainAs(displayNameRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(cardNumberRef.top)
                            width = Dimension.wrapContent
                        },
                    )
                }

                Text(
                    text = state.numberShort,
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.constantWhite,
                    modifier = Modifier
                        .constrainAs(cardNumberRef) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                        }
                        .padding(bottom = 8.dp),
                )
                when (state.cardFrozenState) {
                    is TangemPayCardFrozenState.Frozen -> Icon(
                        modifier = Modifier
                            .constrainAs(frozenIconRef) {
                                start.linkTo(cardNumberRef.end, margin = 4.dp)
                                top.linkTo(cardNumberRef.top)
                                bottom.linkTo(cardNumberRef.bottom)
                            }
                            .padding(bottom = 8.dp)
                            .size(16.dp),
                        painter = painterResource(id = R.drawable.ic_snow_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.constant,
                    )
                    TangemPayCardFrozenState.Pending -> CircularProgressIndicator(
                        modifier = Modifier
                            .constrainAs(frozenIconRef) {
                                start.linkTo(cardNumberRef.end, margin = 4.dp)
                                top.linkTo(cardNumberRef.top)
                                bottom.linkTo(cardNumberRef.bottom)
                            }
                            .padding(bottom = 8.dp)
                            .size(16.dp),
                        color = TangemTheme.colors.text.constantWhite,
                        strokeWidth = 1.dp,
                    )
                    TangemPayCardFrozenState.Unfrozen -> Unit
                }

                TangemPayCardDetailsCustomButton(
                    modifier = Modifier
                        .constrainAs(buttonRef) {
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                        .testTag(TangemPayTestTags.CARD_DETAILS_SHOW_BUTTON),
                    text = stringResourceSafe(id = R.string.tangempay_card_details_show_details),
                    onClick = state.onClick,
                    showProgress = state.isLoading,
                )
            }
        }
    }
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
        modifier = modifier.conditional(
            condition = state.isEditingEnabled,
            modifier = { clickable(onClick = state.onClick) },
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.displayName,
            style = TangemTheme.typography.caption1.copy(color = TangemTheme.colors.text.constantWhite),
            maxLines = 1,
        )
        if (state.isEditingEnabled) {
            Icon(
                painter = painterResource(id = R.drawable.ic_edit_new_12),
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = TangemTheme.colors.text.constantWhite,
            )
        }
    }
}

@Composable
private fun EditingCardDisplayName(state: DisplayNameState.Editing, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val placeholder = stringResourceSafe(R.string.tangempay_card_edit_name_placeholder)

    val textStyle = TangemTheme.typography.caption1.copy(color = TangemTheme.colors.text.constantWhite)
    val textMeasurer = rememberTextMeasurer()
    val measuredText = state.editingValue.text.ifEmpty { placeholder }
    val textWidthDp = with(LocalDensity.current) {
        textMeasurer.measure(measuredText, textStyle).size.width.toDp() + TEXT_WIDTH_PADDING.dp
    }

    BasicTextField(
        value = state.editingValue,
        onValueChange = state.onValueChanged,
        modifier = modifier
            .width(textWidthDp.coerceAtLeast(1.dp))
            .focusRequester(focusRequester),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(TangemTheme.colors.text.constantWhite),
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
                        style = textStyle.copy(color = TangemTheme.colors.text.tertiary),
                    )
                }
                innerTextField()
            }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Suppress("MagicNumber", "LongParameterList")
@Composable
private fun TangemPayCardDetailsShownBlock(
    cardNumber: String,
    expiry: String,
    cvv: String,
    onCopyCardNumber: () -> Unit,
    onCopyExpiry: () -> Unit,
    onCopyCvv: () -> Unit,
    onHideDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        CardDetailsTextContainer(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            title = stringResourceSafe(R.string.tangempay_card_details_card_number),
            text = cardNumber,
            onCopy = onCopyCardNumber,
            valueTestTag = TangemPayTestTags.CARD_DETAILS_NUMBER_VALUE,
            copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_NUMBER,
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardDetailsTextContainer(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                title = stringResourceSafe(R.string.tangempay_card_details_expiry),
                text = expiry,
                onCopy = onCopyExpiry,
                valueTestTag = TangemPayTestTags.CARD_DETAILS_EXPIRATION_VALUE,
                copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_EXPIRATION,
            )
            CardDetailsTextContainer(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                title = stringResourceSafe(R.string.tangempay_card_details_cvc),
                text = cvv,
                onCopy = onCopyCvv,
                valueTestTag = TangemPayTestTags.CARD_DETAILS_CVC_VALUE,
                copyTestTag = TangemPayTestTags.CARD_DETAILS_COPY_CVC,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Row {
            SpacerWMax()
            TangemPayCardDetailsCustomButton(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 8.dp)
                    .testTag(TangemPayTestTags.CARD_DETAILS_HIDE_BUTTON),
                text = stringResourceSafe(id = R.string.tangempay_card_details_hide_details),
                onClick = onHideDetails,
                showProgress = false,
            )
        }
    }
}

@Composable
private fun CardDetailsTextContainer(
    title: String,
    text: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    valueTestTag: String? = null,
    copyTestTag: String? = null,
) {
    Row(
        modifier = modifier
            .background(
                color = CustomCardBlockColor,
                shape = RoundedCornerShape(11.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            Text(
                text = text,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.constantWhite,
                modifier = if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier,
            )
        }
        IconButton(
            modifier = Modifier
                .size(TangemTheme.dimens.size32)
                .then(if (copyTestTag != null) Modifier.testTag(copyTestTag) else Modifier),
            onClick = onCopy,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_copy_new_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
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
private fun TangemPayCardDetailsBlockV2Preview(
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
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = { _, _ -> },
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
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = { _, _ -> },
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
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = { _, _ -> },
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
            isHidden = false,
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
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