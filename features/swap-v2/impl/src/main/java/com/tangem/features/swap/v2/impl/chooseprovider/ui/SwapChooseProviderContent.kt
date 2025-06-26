package com.tangem.features.swap.v2.impl.chooseprovider.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R

@Composable
fun SwapChooseProviderContent(expressProvider: ExpressProvider?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick,
        ),
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = TangemTheme.colors.stroke.primary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.padding(12.dp),
        ) {
            Icon(
                painter = rememberVectorPainter(
                    ImageVector.vectorResource(R.drawable.ic_exchange_horizontal_24),
                ),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
            )
            Text(
                text = stringResourceSafe(R.string.express_provider),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(start = 8.dp),
            )
            SpacerWMax()
            SubcomposeAsyncImage(
                modifier = modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TangemColorPalette.Light1),
                model = ImageRequest.Builder(context = LocalContext.current)
                    .data(expressProvider?.imageLarge)
                    .crossfade(enable = true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
            )
            Text(
                text = expressProvider?.name.orEmpty(), // todo provider error
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
                modifier = Modifier.padding(start = 6.dp),
            )
            Icon(
                painter = rememberVectorPainter(
                    ImageVector.vectorResource(R.drawable.ic_chevron_24),
                ),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapChooseProviderContent_Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier.background(TangemTheme.colors.background.tertiary),
        ) {
            SwapChooseProviderContent(
                expressProvider = ExpressProvider(
                    providerId = "changelly",
                    rateTypes = listOf(ExpressRateType.Fixed),
                    name = "Changelly",
                    type = ExpressProviderType.CEX,
                    imageLarge = "",
                    termsOfUse = "",
                    privacyPolicy = "",
                    isRecommended = false,
                    slippage = null,
                ),
                onClick = {},
            )
        }
    }
}
// endregion