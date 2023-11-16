package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.models.states.ChooseFeeBottomSheetConfig
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ChooseFeeBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: ChooseFeeBottomSheetConfig ->
        ChooseFeeBottomSheetContent(content = content)
    }
}

@Composable
private fun ChooseFeeBottomSheetContent(content: ChooseFeeBottomSheetConfig) {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.secondary),
    ) {
        Text(
            text = "Choose fee", // todo replace with strings
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10)
                .align(Alignment.CenterHorizontally),
        )
        Column(
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing16)
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        ) {
            FeeItemsBlock(content)
        }
        Text(
            text = "Network transaction fees are small charges paid to support network security, incentivize " +
                "validators," +
                " allocate resources, and determine transaction priority.", // todo replace with strings
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
            modifier = Modifier
                .padding(
                    vertical = TangemTheme.dimens.spacing8,
                    horizontal = TangemTheme.dimens.spacing16,
                )
                .align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun FeeItemsBlock(content: ChooseFeeBottomSheetConfig) {
    content.feeItems.forEach { feeItem ->
        val isSelected = feeItem.feeType == content.selectedFee
        val preEllipsizeText = feeItem.amountCrypto
        val postEllipsizeText = " ${feeItem.symbolCrypto} (${feeItem.amountFiat} ${feeItem.symbolFiat})"
        when (feeItem.feeType) {
            FeeType.NORMAL -> {
                SelectorRowItem(
                    titleRes = R.string.common_fee_selector_option_market,
                    iconRes = R.drawable.ic_bird_24,
                    preEllipsize = TextReference.Str(preEllipsizeText),
                    postEllipsize = TextReference.Str(postEllipsizeText),
                    isSelected = isSelected,
                    onSelect = { content.onSelectFeeType(feeItem.feeType) },
                )
            }
            FeeType.PRIORITY -> {
                SelectorRowItem(
                    titleRes = R.string.common_fee_selector_option_fast,
                    iconRes = R.drawable.ic_hare_24,
                    preEllipsize = TextReference.Str(preEllipsizeText),
                    postEllipsize = TextReference.Str(postEllipsizeText),
                    isSelected = isSelected,
                    onSelect = { content.onSelectFeeType(feeItem.feeType) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChooseFeeBottomSheetContent_Preview() {
    val feeItems = listOf(
        FeeItemState(
            feeType = FeeType.NORMAL,
            title = "Fee",
            amountCrypto = "1000",
            symbolCrypto = "MATIC",
            amountFiat = "10",
            symbolFiat = "$",
            onClick = {},
        ),
        FeeItemState(
            feeType = FeeType.PRIORITY,
            title = "Fee",
            amountCrypto = "2000",
            symbolCrypto = "MATIC",
            amountFiat = "20",
            symbolFiat = "$",
            onClick = {},
        ),
    ).toImmutableList()
    Column {
        TangemTheme(isDark = true) {
            ChooseFeeBottomSheetContent(
                ChooseFeeBottomSheetConfig(
                    selectedFee = FeeType.NORMAL,
                    onSelectFeeType = {},
                    feeItems = feeItems,
                ),
            )
        }

        SpacerH24()

        TangemTheme(isDark = false) {
            ChooseFeeBottomSheetContent(
                ChooseFeeBottomSheetConfig(
                    selectedFee = FeeType.NORMAL,
                    onSelectFeeType = {},
                    feeItems = feeItems,
                ),
            )
        }
    }
}
