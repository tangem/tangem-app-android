package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.utils.StringsSigns
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.tokendetails.presentation.tokendetails.state.QuickTopUpBlockUM
import kotlinx.collections.immutable.persistentListOf

private val quickTopUpGradientBrush = Brush.linearGradient(
    colors = listOf(Color(0xFFEDE5F3), Color(0xFFD7EDD9)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
)

private val quickTopUpBorderBrush = Brush.sweepGradient(
    listOf(
        Color(0x0D000000),
        Color(0x26000000),
        Color(0x0D000000),
        Color(0x26000000),
    ),
)

@Composable
internal fun QuickTopUpBlock(state: QuickTopUpBlockUM, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(TangemTheme.dimens.radius20)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(brush = quickTopUpGradientBrush)
            .border(width = 1.dp, brush = quickTopUpBorderBrush, shape = shape),
    ) {
        Column(
            modifier = Modifier.padding(TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            val textColor = TangemTheme.colors.text.primary1
            Text(
                text = combinedReference(
                    stringReference("${StringsSigns.LIGHTNING} "),
                    resourceReference(R.string.quick_top_up_title),
                ).resolveReference(),
                style = TangemTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.graphicsLayer {
                    colorFilter = ColorFilter.tint(textColor, BlendMode.SrcIn)
                },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing6),
            ) {
                state.amounts.fastForEach { amountUM ->
                    Button(
                        onClick = amountUM.onClick,
                        shape = CircleShape,
                        contentPadding = PaddingValues(
                            horizontal = TangemTheme.dimens.spacing12,
                            vertical = TangemTheme.dimens.spacing0,
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TangemTheme.colors.background.primary,
                            contentColor = TangemTheme.colors.text.primary1,
                        ),
                        modifier = Modifier.heightIn(TangemTheme.dimens.size36),
                        elevation = null,
                    ) {
                        Text(
                            text = amountUM.displayValue.resolveReference(),
                            style = TangemTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun QuickTopUpBlock_Preview() {
    TangemThemePreviewRedesign {
        QuickTopUpBlock(
            state = QuickTopUpBlockUM(
                amounts = persistentListOf(
                    QuickTopUpBlockUM.QuickTopUpAmountUM(
                        displayValue = stringReference("\$50"),
                        onClick = {},
                    ),
                    QuickTopUpBlockUM.QuickTopUpAmountUM(
                        displayValue = stringReference("\$200"),
                        onClick = {},
                    ),
                    QuickTopUpBlockUM.QuickTopUpAmountUM(
                        displayValue = stringReference("\$700"),
                        onClick = {},
                    ),
                    QuickTopUpBlockUM.QuickTopUpAmountUM(
                        displayValue = resourceReference(R.string.quick_top_up_chip_other),
                        onClick = {},
                        isOther = true,
                    ),
                ),
            ),
            modifier = Modifier.padding(TangemTheme.dimens.spacing12),
        )
    }
}