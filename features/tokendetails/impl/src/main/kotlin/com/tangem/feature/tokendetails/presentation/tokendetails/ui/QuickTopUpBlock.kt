package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.res.R
import com.tangem.core.ui.ds.button.PrimaryInverseTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.messageEffectBackground
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.tokendetails.presentation.tokendetails.state.QuickTopUpBlockUM
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun QuickTopUpBlock(state: QuickTopUpBlockUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .messageEffectBackground(
                    messageEffect = TangemMessageEffect.Magic,
                    radius = TangemTheme.dimens2.x6,
                    contentColor = TangemTheme.colors2.surface.level3,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens2.x3),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x4),
        ) {
            val titleColor = TangemTheme.colors2.text.neutral.primary
            Text(
                text = combinedReference(
                    stringReference("${StringsSigns.LIGHTNING} "),
                    resourceReference(R.string.quick_top_up_title),
                ).resolveReference(),
                style = TangemTheme.typography2.bodySemibold16,
                modifier = Modifier.graphicsLayer {
                    colorFilter = ColorFilter.tint(titleColor, BlendMode.SrcIn)
                },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1_5),
            ) {
                state.amounts.fastForEach { amountUM ->
                    PrimaryInverseTangemButton(
                        text = amountUM.displayValue,
                        onClick = amountUM.onClick,
                        size = TangemButtonSize.X9,
                        shape = TangemButtonShape.Rounded,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
            modifier = Modifier.padding(TangemTheme.dimens2.x3),
        )
    }
}