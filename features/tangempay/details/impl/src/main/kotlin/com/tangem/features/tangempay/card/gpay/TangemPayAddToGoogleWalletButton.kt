package com.tangem.features.tangempay.card.gpay

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R
import kotlin.math.roundToInt

@Composable
internal fun TangemPayAddToGoogleWalletButton(onClick: () -> Unit, isLoading: Boolean, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentAlpha by animateFloatAsState(targetValue = if (isLoading) 0f else 1f, label = "contentAlpha")
    val loaderAlpha by animateFloatAsState(targetValue = if (isLoading) 1f else 0f, label = "loaderAlpha")
    val context = LocalContext.current
    val density = LocalDensity.current
    val background = remember(context) {
        checkNotNull(context.getDrawable(R.drawable.img_google_wallet_button_background))
    }
    val focusRing = BorderStroke(width = 2.dp, color = TangemTheme.colors3.interaction.focusRing.brand)

    CompositionLocalProvider(LocalRippleConfiguration provides googleWalletButtonRipple()) {
        Box(
            modifier = modifier
                .height(with(density) { ButtonHeight.toDp() })
                .drawBehind {
                    background.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
                    drawIntoCanvas { canvas -> background.draw(canvas.nativeCanvas) }
                }
                .then(if (isFocused) Modifier.border(border = focusRing, shape = CircleShape) else Modifier)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = !isLoading,
                    role = Role.Button,
                    onClick = onClick,
                )
                .testTag(TangemPayTestTags.ADD_TO_GOOGLE_WALLET_BUTTON),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.img_add_to_google_wallet_button_content),
                contentDescription = stringResourceSafe(R.string.tangempay_card_details_add_to_wallet_button_text),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = with(density) { ContentHorizontalPadding.toDp() },
                        vertical = with(density) { ContentVerticalPadding.toDp() },
                    )
                    .alpha(contentAlpha),
            )
            if (loaderAlpha > 0f) {
                TangemLoader(
                    modifier = Modifier.alpha(loaderAlpha),
                    color = TangemTheme.colors3.icon.staticDark,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
@ReadOnlyComposable
private fun googleWalletButtonRipple(): RippleConfiguration = RippleConfiguration(
    color = TangemTheme.colors3.interaction.press.staticDark,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0f,
        focusedAlpha = 0f,
        hoveredAlpha = 0.05f,
        pressedAlpha = 0.1f,
    ),
)

private val ButtonHeight = 48.sp
private val ContentHorizontalPadding = 20.sp
private val ContentVerticalPadding = 10.sp

@Suppress("MagicNumber")
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(widthDp = 360, fontScale = 1.5f, locale = "de")
@Composable
private fun PreviewTangemPayAddToGoogleWalletButton() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemPayAddToGoogleWalletButton(onClick = {}, isLoading = false, modifier = Modifier.fillMaxWidth())
            TangemPayAddToGoogleWalletButton(onClick = {}, isLoading = true, modifier = Modifier.fillMaxWidth())
        }
    }
}