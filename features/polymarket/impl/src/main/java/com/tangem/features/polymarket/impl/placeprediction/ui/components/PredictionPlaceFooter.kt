package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.polymarket.impl.common.PolymarketLegalLine
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.ui.preview.PreviewBuyOrder

@Composable
internal fun PredictionPlaceFooter(
    onPolymarketTermsClick: () -> Unit,
    onTangemTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    button: @Composable () -> Unit,
) {
    TangemFooter(
        modifier = modifier,
        windowInsets = windowInsets,
        extraTop = {
            PolymarketLegalLine(
                onPolymarketTermsClick = onPolymarketTermsClick,
                onTangemTermsClick = onTangemTermsClick,
            )
        },
    ) {
        button()
    }
}

@Composable
internal fun PredictionPlaceButton(
    order: OrderSideUM,
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconEnd: TangemIconUM? = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24),
) {
    TangemButton(
        modifier = modifier.fillMaxWidth(),
        size = TangemButton.Size.X12,
        isLoading = isLoading,
        iconEnd = iconEnd,
        text = resourceReference(
            when (order) {
                is OrderSideUM.Buy -> R.string.prediction_place_button
                is OrderSideUM.Sell -> R.string.prediction_close_button
            },
        ),
        isEnabled = isEnabled,
        onClick = onClick,
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 160)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 160, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PredictionPlaceFooterPreview() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PredictionPlaceFooter(
                windowInsets = WindowInsets(0),
                onPolymarketTermsClick = {},
                onTangemTermsClick = {},
            ) {
                PredictionPlaceButton(order = PreviewBuyOrder, isLoading = false, isEnabled = true, onClick = {})
            }
        }
    }
}