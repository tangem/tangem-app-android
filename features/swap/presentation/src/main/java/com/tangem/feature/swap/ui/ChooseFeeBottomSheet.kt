package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.domain.models.ui.FeeType
import com.tangem.feature.swap.models.states.ChooseFeeBottomSheetConfig
import com.tangem.feature.swap.models.states.FeeItemState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.toImmutableList

@Composable
fun ChooseFeeBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: ChooseFeeBottomSheetConfig ->
        ChooseFeeBottomSheetContent(content = content)
    }
}

@Composable
private fun ChooseFeeBottomSheetContent(content: ChooseFeeBottomSheetConfig) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors.background.tertiary)
            .padding(bottom = TangemTheme.dimens.spacing8),
    ) {
        Text(
            text = stringResource(R.string.common_fee_selector_title),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing10)
                .align(Alignment.CenterHorizontally),
        )
        Column(
            modifier = Modifier
                .padding(TangemTheme.dimens.spacing16)
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        ) {
            FeeItemsBlock(content)
        }
        FooterBlock(
            readMore = content.readMore,
            readMoreUrl = content.readMoreUrl,
            onReadMoreClick = content.onReadMoreClick,
        )
    }
}

@Composable
private fun FooterBlock(readMore: TextReference, readMoreUrl: String, onReadMoreClick: (String) -> Unit) {
    val linkText = readMore.resolveReference()
    val fullString = stringResource(R.string.common_fee_selector_footer, linkText)
    val linkTextPosition = fullString.length - linkText.length
    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = TangemTheme.colors.text.tertiary)) {
            append(fullString.substring(0, linkTextPosition))
        }
        withStyle(SpanStyle(color = TangemTheme.colors.text.accent)) {
            append(fullString.substring(linkTextPosition, fullString.length))
        }
    }

    val click = { i: Int ->
        val readMoreStyle = requireNotNull(annotatedString.spanStyles.getOrNull(1))
        if (i in readMoreStyle.start..readMoreStyle.end) {
            onReadMoreClick(readMoreUrl)
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = Modifier
            .padding(
                vertical = TangemTheme.dimens.spacing8,
                horizontal = TangemTheme.dimens.spacing16,
            ),
        style = TangemTheme.typography.caption2.copy(textAlign = TextAlign.Start),
        onClick = click,
    )
}

@Composable
private fun FeeItemsBlock(content: ChooseFeeBottomSheetConfig) {
    content.feeItems.forEachIndexed { index, feeItem ->
        val isSelected = feeItem.feeType == content.selectedFee
        val showDivider = content.feeItems.lastIndex != index
        val symbol = " ${feeItem.symbolCrypto}"
        val preDotText = "${feeItem.amountCrypto}$symbol"
        val postDot = feeItem.amountFiatFormatted
        val ellipsizeOffset = symbol.length
        when (feeItem.feeType) {
            FeeType.NORMAL -> {
                SelectorRowItem(
                    titleRes = R.string.common_fee_selector_option_market,
                    iconRes = R.drawable.ic_bird_24,
                    preDot = TextReference.Str(preDotText),
                    postDot = TextReference.Str(postDot),
                    ellipsizeOffset = ellipsizeOffset,
                    isSelected = isSelected,
                    onSelect = { content.onSelectFeeType(feeItem.feeType) },
                    showDivider = showDivider,
                )
            }
            FeeType.PRIORITY -> {
                SelectorRowItem(
                    titleRes = R.string.common_fee_selector_option_fast,
                    iconRes = R.drawable.ic_hare_24,
                    preDot = TextReference.Str(preDotText),
                    postDot = TextReference.Str(postDot),
                    ellipsizeOffset = ellipsizeOffset,
                    isSelected = isSelected,
                    onSelect = { content.onSelectFeeType(feeItem.feeType) },
                    showDivider = showDivider,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChooseFeeBottomSheetContent_Preview() {
    val feeItems = listOf(
        FeeItemState.Content(
            feeType = FeeType.NORMAL,
            title = stringReference("Fee"),
            amountCrypto = "1000",
            symbolCrypto = "MATIC",
            amountFiatFormatted = "(10$)",
            isClickable = false,
            onClick = {},
        ),
        FeeItemState.Content(
            feeType = FeeType.PRIORITY,
            title = stringReference("Fee"),
            amountCrypto = "2000",
            symbolCrypto = "MATIC",
            amountFiatFormatted = "(10$)",
            isClickable = false,
            onClick = {},
        ),
    ).toImmutableList()
    Column {
        TangemThemePreview(isDark = true) {
            ChooseFeeBottomSheetContent(
                ChooseFeeBottomSheetConfig(
                    selectedFee = FeeType.NORMAL,
                    onSelectFeeType = {},
                    feeItems = feeItems,
                    readMore = stringReference("Read more"),
                    readMoreUrl = "",
                    onReadMoreClick = {},
                ),
            )
        }

        SpacerH24()

        TangemThemePreview(isDark = false) {
            ChooseFeeBottomSheetContent(
                ChooseFeeBottomSheetConfig(
                    selectedFee = FeeType.NORMAL,
                    onSelectFeeType = {},
                    feeItems = feeItems,
                    readMore = stringReference("Read more"),
                    readMoreUrl = "",
                    onReadMoreClick = {},
                ),
            )
        }
    }
}
