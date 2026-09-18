package com.tangem.features.tangempay.account

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_12
import com.tangem.core.ui.res.generated.icons.ic_cloud_12_filled
import com.tangem.core.ui.res.generated.icons.ic_snowflake_16
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.domain.models.pay.TangemPayCardType

private const val DEFAULT_CARD_BG = 0xFF1C1F29
private const val REISSUING_CARD_BG = 0xFF1E1E1E
private const val DISABLED_ALPHA = 0.5f

@Suppress("LongParameterList")
@Composable
internal fun TangemPayCardView(
    isIssueInProgress: Boolean,
    isEnabled: Boolean,
    lastDigits: String,
    imageUrl: String?,
    cardType: TangemPayCardType,
    onClick: () -> Unit,
    isFrozen: Boolean,
    modifier: Modifier = Modifier,
) {
    CardBackground(
        modifier = modifier
            .size(
                height = TangemTheme.dimens2.x10,
                width = TangemTheme.dimens2.x14,
            )
            .testTag(TangemPayTestTags.PAYMENT_ACCOUNT_CARD_BUTTON),
        isIssueInProgress = isIssueInProgress,
        isEnabled = isEnabled,
        imageUrl = imageUrl,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x1)
                .padding(top = TangemTheme.dimens2.x1)
                .height(TangemTheme.dimens2.x3),
        ) {
            val statusIcon = when {
                isFrozen -> Icons.ic_snowflake_16
                isIssueInProgress -> Icons.ic_clock_12
                cardType != TangemPayCardType.PHYSICAL -> Icons.ic_cloud_12_filled
                else -> null
            }
            statusIcon?.let { icon ->
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(TangemTheme.dimens2.x3),
                    imageVector = icon,
                    tint = TangemTheme.colors3.icon.staticDark,
                    contentDescription = null,
                )
            }
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(height = TangemTheme.dimens2.x2, width = 22.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_visa_logo),
                tint = TangemTheme.colors3.icon.staticDark,
                contentDescription = null,
            )
        }

        Text(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TangemTheme.dimens2.x1, bottom = TangemTheme.dimens2.x1),
            text = lastDigits,
            style = TangemTheme.typography3.caption.medium.copy(fontSize = 10.sp),
            color = TangemTheme.colors3.text.staticDark.primary,
        )
    }
}

@Composable
internal fun TangemPayAddCardView(onClick: () -> Unit, isEnabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
            .size(
                height = TangemTheme.dimens2.x10,
                width = TangemTheme.dimens2.x14,
            )
            .clip(RoundedCornerShape(6.dp))
            .background(TangemTheme.colors3.bg.opaque.primary)
            .clickableSingle(onClick = onClick, enabled = isEnabled),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_plus_default_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.secondary,
        )
    }
}

@Suppress("MagicNumber")
@Composable
private fun CardBackground(
    isIssueInProgress: Boolean,
    isEnabled: Boolean,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val bgColor = remember(isIssueInProgress) {
        if (isIssueInProgress) {
            Color(REISSUING_CARD_BG)
        } else {
            Color(DEFAULT_CARD_BG)
        }
    }
    Box(
        modifier = modifier
            .alpha(if (isEnabled) 1f else DISABLED_ALPHA)
            .clip(RoundedCornerShape(6.dp))
            .drawBehind {
                drawRect(bgColor)

                val w = size.width
                val h = size.height
                val radiusScaleRightCorner = h / 2f

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(
                                if (isIssueInProgress) 0xFFB0B4BC else 0xFF38587F,
                            ).copy(
                                if (isIssueInProgress) .1f else .41f,
                            ),
                            Color.Transparent,
                        ),
                        center = Offset(w / 2, h * .05f),
                        radius = radiusScaleRightCorner,
                        tileMode = TileMode.Clamp,
                    ),
                )
            }
            .border(
                width = 1.dp,
                color = TangemTheme.colors3.border.primary,
                shape = RoundedCornerShape(6.dp),
            )
            .clickableSingle(onClick = onClick, enabled = isEnabled),
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                modifier = Modifier.matchParentSize(),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                loading = {},
                error = {},
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
        content()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun CardBackgroundPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            CardBackground(
                modifier = Modifier.size(
                    height = TangemTheme.dimens2.x10,
                    width = TangemTheme.dimens2.x14,
                ),
                isIssueInProgress = false,
                onClick = {},
                content = {},
                isEnabled = true,
                imageUrl = null,
            )
            SpacerH(TangemTheme.dimens2.x4)
            CardBackground(
                modifier = Modifier.size(
                    height = TangemTheme.dimens2.x10,
                    width = TangemTheme.dimens2.x14,
                ),
                isIssueInProgress = true,
                imageUrl = null,
                onClick = {},
                content = {},
                isEnabled = true,
            )
            SpacerH(TangemTheme.dimens2.x4)
            TangemPayAddCardView(onClick = {}, isEnabled = true)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun TangemPayCardViewPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            PreviewCardView(cardType = TangemPayCardType.VIRTUAL, lastDigits = "1234")
            SpacerH(TangemTheme.dimens2.x4)
            PreviewCardView(cardType = TangemPayCardType.PHYSICAL, lastDigits = "5678")
            SpacerH(TangemTheme.dimens2.x4)
            PreviewCardView(cardType = TangemPayCardType.VIRTUAL, lastDigits = "", isIssueInProgress = true)
            SpacerH(TangemTheme.dimens2.x4)
            PreviewCardView(cardType = TangemPayCardType.VIRTUAL, lastDigits = "1234", isFrozen = true)
        }
    }
}

@Composable
private fun PreviewCardView(
    cardType: TangemPayCardType,
    lastDigits: String,
    isIssueInProgress: Boolean = false,
    isFrozen: Boolean = false,
) {
    TangemPayCardView(
        isIssueInProgress = isIssueInProgress,
        onClick = {},
        lastDigits = lastDigits,
        imageUrl = null,
        cardType = cardType,
        isEnabled = true,
        isFrozen = isFrozen,
    )
}