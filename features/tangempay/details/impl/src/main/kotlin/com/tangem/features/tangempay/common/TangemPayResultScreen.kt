@file:Suppress("MagicNumber")

package com.tangem.features.tangempay.common

import android.content.res.Configuration
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.haze.hazeForegroundEffectTangem
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_24
import com.tangem.core.ui.res.generated.icons.ic_error_28
import dev.chrisbanes.haze.HazeStyle

private val StatusIconSize = 40.dp
private val FallbackCornerRadius = 24.dp

// DS3 palette Green.70 / Green.40 and Red.70 / Red.40 — the generated palette is internal to :core:ui.
private val AmbientSuccessFarColor = Color(0xFF1E6110)
private val AmbientSuccessNearColor = Color(0xFF2DAE3B)
private val AmbientErrorFarColor = Color(0xFF9E2729)
private val AmbientErrorNearColor = Color(0xFFFF5E66)

private val AmbientFarSpec = AmbientBlobSpec(
    diameter = 415.dp,
    centerX = 122.5.dp,
    centerY = 106.5.dp,
    isCenterFromEnd = false,
    alpha = 0.1f,
    blurRadius = 221.dp,
)

private val AmbientNearSpec = AmbientBlobSpec(
    diameter = 346.dp,
    centerX = (-26).dp,
    centerY = 22.dp,
    isCenterFromEnd = true,
    alpha = 0.4f,
    blurRadius = 165.dp,
)

@Composable
internal fun TangemPayResultScreen(
    variant: TangemPayResultScreen.Variant,
    title: TextReference,
    subtitle: TextReference,
    buttonText: TextReference,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: TextReference? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        ResultAmbient(variant = variant, modifier = Modifier.matchParentSize())
        TangemGlowRing(
            modifier = Modifier.matchParentSize(),
            variant = variant.glowVariant,
            cornerRadius = rememberScreenCornerRadius(),
        )
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                ResultHeader(variant = variant, title = title, subtitle = subtitle)
            }
            ResultFooter(buttonText = buttonText, onButtonClick = onButtonClick, footnote = footnote)
        }
    }
}

@Composable
private fun ResultHeader(
    variant: TangemPayResultScreen.Variant,
    title: TextReference,
    subtitle: TextReference,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(StatusIconSize),
            imageVector = variant.icon,
            tint = variant.iconTint(),
            contentDescription = null,
        )
        SpacerH(20.dp)
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        SpacerH(8.dp)
        Text(
            text = subtitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultFooter(
    buttonText: TextReference,
    onButtonClick: () -> Unit,
    footnote: TextReference?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        if (footnote != null) {
            Text(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                text = footnote.resolveAnnotatedReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
            SpacerH(12.dp)
        }
        TangemButton(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = buttonText,
            onClick = onButtonClick,
        )
    }
}

@Composable
private fun ResultAmbient(variant: TangemPayResultScreen.Variant, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AmbientBlob(
            spec = AmbientFarSpec,
            color = variant.ambientFarColor,
            modifier = Modifier.matchParentSize(),
        )
        AmbientBlob(
            spec = AmbientNearSpec,
            color = variant.ambientNearColor,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun AmbientBlob(spec: AmbientBlobSpec, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .hazeForegroundEffectTangem(style = HazeStyle(blurRadius = spec.blurRadius, tint = null))
            .drawBehind {
                val centerX = spec.centerX.toPx().let { if (spec.isCenterFromEnd) size.width - it else it }
                drawCircle(
                    color = color,
                    radius = spec.diameter.toPx() / 2f,
                    center = Offset(x = centerX, y = spec.centerY.toPx()),
                    alpha = spec.alpha,
                )
            },
    )
}

@Composable
private fun rememberScreenCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    return remember(view, density, configuration) {
        val radiusPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        } else {
            0
        }
        if (radiusPx > 0) with(density) { radiusPx.toDp() } else FallbackCornerRadius
    }
}

private data class AmbientBlobSpec(
    val diameter: Dp,
    val centerX: Dp,
    val centerY: Dp,
    val isCenterFromEnd: Boolean,
    val alpha: Float,
    val blurRadius: Dp,
)

internal object TangemPayResultScreen {

    enum class Variant(
        val icon: ImageVector,
        val glowVariant: TangemGlowRing.Variant,
        val ambientFarColor: Color,
        val ambientNearColor: Color,
    ) {
        Success(
            icon = Icons.ic_checkmark_24,
            glowVariant = TangemGlowRing.Variant.Success,
            ambientFarColor = AmbientSuccessFarColor,
            ambientNearColor = AmbientSuccessNearColor,
        ),
        Error(
            icon = Icons.ic_error_28,
            glowVariant = TangemGlowRing.Variant.Error,
            ambientFarColor = AmbientErrorFarColor,
            ambientNearColor = AmbientErrorNearColor,
        ),
    }
}

@Composable
@ReadOnlyComposable
private fun TangemPayResultScreen.Variant.iconTint(): Color = when (this) {
    TangemPayResultScreen.Variant.Success -> TangemTheme.colors3.icon.status.success
    TangemPayResultScreen.Variant.Error -> TangemTheme.colors3.icon.status.error
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800, name = "Light")
@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark",
)
@Composable
private fun TangemPayResultScreenPreview(@PreviewParameter(ResultProvider::class) model: ResultPreview) {
    TangemThemePreviewRedesign {
        TangemPayResultScreen(
            variant = model.variant,
            title = stringReference(model.title),
            subtitle = stringReference(model.subtitle),
            footnote = model.footnote?.let(::stringReference),
            buttonText = stringReference(model.buttonText),
            onButtonClick = {},
        )
    }
}

private data class ResultPreview(
    val variant: TangemPayResultScreen.Variant,
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val footnote: String? = null,
)

private class ResultProvider : CollectionPreviewParameterProvider<ResultPreview>(
    collection = listOf(
        ResultPreview(
            variant = TangemPayResultScreen.Variant.Success,
            title = "Card ordered",
            subtitle = "It will take up to 20 business days for delivery",
            footnote = "Within 3 business days you will receive an email from logistic company " +
                "on panampalmer@gmail.com",
            buttonText = "Show card",
        ),
        ResultPreview(
            variant = TangemPayResultScreen.Variant.Error,
            title = "You have exceeded your attempts for activation",
            subtitle = "Please contact support",
            buttonText = "Contact support",
        ),
    ),
)