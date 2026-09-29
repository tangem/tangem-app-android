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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

@Suppress("MagicNumber")
@Composable
internal fun TangemPayAddToGoogleWalletButton(onClick: () -> Unit, isLoading: Boolean, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val contentAlpha by animateFloatAsState(targetValue = if (isLoading) 0f else 1f, label = "contentAlpha")
    val loaderAlpha by animateFloatAsState(targetValue = if (isLoading) 1f else 0f, label = "loaderAlpha")

    CompositionLocalProvider(LocalRippleConfiguration provides googleWalletButtonRipple()) {
        Box(
            modifier = modifier
                .heightIn(min = 48.dp)
                .border(
                    border = if (isFocused) {
                        BorderStroke(width = 2.dp, color = TangemTheme.colors3.interaction.focusRing.brand)
                    } else {
                        BorderStroke(width = 1.dp, color = GoogleWalletButtonOutlineColor)
                    },
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .background(GoogleWalletButtonColor)
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ButtonContent(
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
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
private fun ButtonContent(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(imageVector = GoogleWalletLogo, contentDescription = null, modifier = Modifier.size(20.dp))
        CompositionLocalProvider(LocalDensity provides cappedFontScaleDensity()) {
            Text(
                text = stringResourceSafe(R.string.tangempay_card_details_add_to_wallet_button_text),
                color = TangemTheme.colors3.text.staticDark.primary,
                style = TangemTheme.typography3.body.medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = TangemTheme.typography3.caption.medium.fontSize,
                    maxFontSize = TangemTheme.typography3.body.medium.fontSize,
                ),
            )
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

@Composable
private fun cappedFontScaleDensity(): Density {
    val density = LocalDensity.current
    return remember(density.density, density.fontScale) {
        if (density.fontScale <= MAX_LABEL_FONT_SCALE) {
            density
        } else {
            Density(density = density.density, fontScale = MAX_LABEL_FONT_SCALE)
        }
    }
}

private const val MAX_LABEL_FONT_SCALE = 1.3f
private val GoogleWalletButtonColor = Color(0xFF1F1F1F)
private val GoogleWalletButtonOutlineColor = Color(0xFF747775)

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