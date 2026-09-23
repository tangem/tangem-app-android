package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatPolymarketPercent
import com.tangem.features.polymarket.impl.common.formatPolymarketShares
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
import com.tangem.features.polymarket.impl.placeprediction.entity.availableOrZero
import com.tangem.features.polymarket.impl.placeprediction.model.SharesCalculator
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_minus_24
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24

@Composable
internal fun PredictionSharesBlock(
    selected: BigDecimal,
    position: SellPositionUM,
    proceeds: SharesProceedsUM,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
        Column {
            ProceedsSection(selected = selected, position = position, proceeds = proceeds)
            CalculatorRow(selected = selected, position = position, onAmountChange = onAmountChange)
        }
    }
}

private fun SellPositionUM.availableLabel(): String = when (this) {
    is SellPositionUM.Held -> available.formatPolymarketShares()
    SellPositionUM.Loading,
    SellPositionUM.NotHeld,
    SellPositionUM.Unreadable,
    -> DASH_SIGN
}

@Composable
private fun ProceedsSection(selected: BigDecimal, position: SellPositionUM, proceeds: SharesProceedsUM) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.prediction_close_you_will_receive),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        ProceedsFigure(proceeds = proceeds)
        Text(
            modifier = Modifier.padding(top = CAPTION_GAP),
            text = stringResourceSafe(
                R.string.prediction_close_selling_of,
                selected.formatPolymarketShares(),
                position.availableLabel(),
            ),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProceedsFigure(proceeds: SharesProceedsUM) {
    val figureStyle = TangemTheme.typography3.display.medium

    AnimatedContent(
        modifier = Modifier.padding(top = CAPTION_GAP),
        targetState = proceeds,
        transitionSpec = { fadeIn() togetherWith fadeOut() using null },
        contentAlignment = Alignment.Center,
        label = "proceedsFigure",
    ) { state ->
        when (state) {
            is SharesProceedsUM.Loading -> TangemShimmer(
                modifier = Modifier.fillMaxWidth(fraction = SHIMMER_WIDTH_FRACTION),
                style = figureStyle,
                textAlign = TextAlign.Center,
            )
            is SharesProceedsUM.Content -> Text(
                text = state.expected.formatPolymarketMoney(),
                style = figureStyle,
                color = TangemTheme.colors3.text.status.success,
                textAlign = TextAlign.Center,
            )
            is SharesProceedsUM.Unavailable -> Text(
                text = DASH_SIGN,
                style = figureStyle,
                color = TangemTheme.colors3.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalculatorRow(selected: BigDecimal, position: SellPositionUM, onAmountChange: (String) -> Unit) {
    val available = position.availableOrZero
    val hasShares = position is SellPositionUM.Held && available > BigDecimal.ZERO
    val isSteppable = SharesCalculator.isSteppable(available)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SharesFraction.entries.forEach { fraction ->
            TangemButton(
                modifier = Modifier.weight(1f),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = fraction.label(),
                isEnabled = hasShares,
                onClick = {
                    onAmountChange(SharesCalculator.fraction(available, fraction.percent).toPlainString())
                },
            )
        }
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X8,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_sign_minus_24),
            isEnabled = isSteppable && selected > SharesCalculator.minimum(available),
            onClick = { onAmountChange(SharesCalculator.decreased(selected, available).toPlainString()) },
        )
        TangemButton(
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X8,
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_sign_plus_24),
            isEnabled = isSteppable && selected < available,
            onClick = { onAmountChange(SharesCalculator.increased(selected, available).toPlainString()) },
        )
    }
}

private enum class SharesFraction(val percent: Int) {
    Quarter(percent = 25),
    Half(percent = 50),
    ThreeQuarters(percent = 75),
    All(percent = 100),
}

private fun SharesFraction.label(): TextReference = when (this) {
    SharesFraction.All -> resourceReference(R.string.send_max_amount)
    else -> stringReference(percent.formatPolymarketPercent())
}

private val CAPTION_GAP = 12.dp
private val BUTTON_GAP = 8.dp
private const val SHIMMER_WIDTH_FRACTION = 0.5f

@Preview(name = "Light", widthDp = 360)
@Preview(name = "Dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 320, fontScale = 1.5f)
@Composable
private fun PredictionSharesBlockPreview(
    @PreviewParameter(PredictionSharesBlockPreviewProvider::class) state: PredictionSharesBlockPreviewState,
) {
    TangemThemePreviewRedesign {
        PredictionSharesBlock(
            selected = state.selected,
            position = state.position,
            proceeds = state.proceeds,
            onAmountChange = {},
        )
    }
}

private data class PredictionSharesBlockPreviewState(
    val selected: BigDecimal,
    val position: SellPositionUM,
    val proceeds: SharesProceedsUM,
)

private class PredictionSharesBlockPreviewProvider : PreviewParameterProvider<PredictionSharesBlockPreviewState> {

    override val values = sequenceOf(
        PredictionSharesBlockPreviewState(
            selected = BigDecimal("0.00"),
            position = SellPositionUM.Held(available = BigDecimal("51.23")),
            proceeds = SharesProceedsUM.Unavailable,
        ),
        PredictionSharesBlockPreviewState(
            selected = BigDecimal("12.80"),
            position = SellPositionUM.Held(available = BigDecimal("51.23")),
            proceeds = SharesProceedsUM.Loading,
        ),
        PredictionSharesBlockPreviewState(
            selected = BigDecimal("51.23"),
            position = SellPositionUM.Held(available = BigDecimal("51.23")),
            proceeds = SharesProceedsUM.Content(expected = BigDecimal("43.54")),
        ),
        PredictionSharesBlockPreviewState(
            selected = BigDecimal("0.50"),
            position = SellPositionUM.Held(available = BigDecimal("0.50")),
            proceeds = SharesProceedsUM.Unavailable,
        ),
        PredictionSharesBlockPreviewState(
            selected = BigDecimal("0.00"),
            position = SellPositionUM.Loading,
            proceeds = SharesProceedsUM.Unavailable,
        ),
        PredictionSharesBlockPreviewState(
            selected = BigDecimal("0.00"),
            position = SellPositionUM.Unreadable,
            proceeds = SharesProceedsUM.Unavailable,
        ),
    )
}