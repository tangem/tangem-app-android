package com.tangem.features.tangempay.ui

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM

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
        if (shouldShowDetails) {
            TangemPayCardDetailsShownBlock(
                cardNumber = state.number,
                expiry = state.expiry,
                cvv = state.cvv,
                onCopyCardNumber = { state.onCopy(state.number) },
                onCopyCvv = { state.onCopy(state.cvv) },
                onCopyExpiry = { state.onCopy(state.expiry) },
                onHideDetails = state.onClick,
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            )
        } else {
            TangemPayCardDetailsHiddenBlock(
                cardFrozenState = state.cardFrozenState,
                isLoading = state.isLoading,
                shortCardNumber = state.numberShort,
                onShowDetails = state.onClick,
            )
        }
    }
}

@Composable
private fun TangemPayCardDetailsHiddenBlock(
    shortCardNumber: String,
    cardFrozenState: TangemPayCardFrozenState,
    isLoading: Boolean,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val imageResId = when (cardFrozenState) {
            is TangemPayCardFrozenState.Frozen -> R.drawable.img_tangem_pay_visa_frozen
            else -> R.drawable.img_tangem_pay_visa
        }
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(id = imageResId),
            contentDescription = null,
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shortCardNumber,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.constantWhite,
            )
            when (cardFrozenState) {
                is TangemPayCardFrozenState.Frozen -> Icon(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                    painter = painterResource(id = R.drawable.ic_snow_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.constant,
                )
                TangemPayCardFrozenState.Pending -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp),
                    color = TangemTheme.colors.text.constantWhite,
                    strokeWidth = 1.dp,
                )
                TangemPayCardFrozenState.Unfrozen -> Unit
            }
            SpacerWMax()
            TangemPayCardDetailsCustomButton(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResourceSafe(id = R.string.tangempay_card_details_show_details),
                onClick = onShowDetails,
                showProgress = isLoading,
            )
        }
    }
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
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardDetailsTextContainer(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                title = stringResourceSafe(R.string.tangempay_card_details_expiry),
                text = expiry,
                onCopy = onCopyExpiry,
            )
            CardDetailsTextContainer(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                title = stringResourceSafe(R.string.tangempay_card_details_cvc),
                text = cvv,
                onCopy = onCopyCvv,
            )
        }
        Row {
            SpacerWMax()
            TangemPayCardDetailsCustomButton(
                modifier = Modifier.padding(end = 16.dp, bottom = 8.dp),
                text = stringResourceSafe(id = R.string.tangempay_card_details_hide_details),
                onClick = onHideDetails,
                showProgress = false,
            )
        }
    }
}

@Composable
private fun CardDetailsTextContainer(title: String, text: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
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
            )
        }
        IconButton(
            modifier = Modifier.size(TangemTheme.dimens.size32),
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
            onCopy = {},
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Frozen,
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = {},
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            expiry = "",
            cvv = "",
            buttonText = resourceReference(R.string.tangempay_card_details_show_details),
            onCopy = {},
            isHidden = true,
            cardFrozenState = TangemPayCardFrozenState.Pending,
        ),
        TangemPayCardDetailsUM(
            isLoading = false,
            onClick = {},
            buttonText = resourceReference(R.string.tangempay_card_details_hide_details),
            onCopy = {},
            isHidden = false,
            number = "1234 5678 9012 3456",
            numberShort = "*3456",
            expiry = "12/34",
            cvv = "123",
            cardFrozenState = TangemPayCardFrozenState.Unfrozen,
        ),
    ),
)