package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheet
import com.tangem.features.collectibles.impl.common.ui.CollectiblesBottomSheetPrimaryButton

/**
 * @param amount the payout, signed and formatted with its token
 * @param price what one more spin costs, formatted with its token
 * @param cardImageUrl the sold card's front; without it the success glyph stands in
 * @param cardTitle the sold card's name, a second title line when known
 */
@Suppress("LongParameterList")
@Composable
internal fun CollectiblesAutoBuybackSheet(
    amount: String,
    price: String,
    cardImageUrl: String?,
    cardTitle: String?,
    onOpenAgainClick: () -> Unit,
    onExplorePacksClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    CollectiblesBottomSheet(onDismiss = onDismiss) {
        AutoBuybackContent(
            amount = amount,
            price = price,
            cardImageUrl = cardImageUrl,
            cardTitle = cardTitle,
            onOpenAgainClick = onOpenAgainClick,
            onExplorePacksClick = onExplorePacksClick,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun AutoBuybackContent(
    amount: String,
    price: String,
    cardImageUrl: String?,
    cardTitle: String?,
    onOpenAgainClick: () -> Unit,
    onExplorePacksClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (cardImageUrl != null) 16.dp else 24.dp),
        ) {
            if (cardImageUrl != null) {
                AsyncImage(
                    model = cardImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = 97.dp, height = 165.dp)
                        .clip(RoundedCornerShape(5.dp)),
                )
            } else {
                TangemModalStatus.Avatar(variant = TangemModalStatus.Variant.Success)
            }

            AutoBuybackTexts(amount = amount, cardTitle = cardTitle)
        }
        TangemFooter(
            windowInsets = WindowInsets(0),
            fadeEnabled = false,
            blurBackground = false,
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            TangemButtonGroup {
                TangemButton(
                    variant = TangemButton.Variant.Secondary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.collectibles_low_inventory_explore_packs),
                    onClick = onExplorePacksClick,
                )
                CollectiblesBottomSheetPrimaryButton(
                    text = resourceReference(R.string.collectibles_auto_buyback_open_again_button, wrappedList(price)),
                    onClick = onOpenAgainClick,
                )
            }
        }
    }
}

@Composable
private fun AutoBuybackTexts(amount: String, cardTitle: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TitleText(text = stringResourceSafe(R.string.collectibles_auto_buyback_title))
        if (cardTitle != null) TitleText(text = cardTitle)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = messageText(amount),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun messageText(amount: String): AnnotatedString {
    val message = stringResourceSafe(R.string.collectibles_auto_buyback_message, amount)
    val amountStart = message.indexOf(amount)
    val amountColor = TangemTheme.colors3.text.primary

    return buildAnnotatedString {
        append(message)
        if (amountStart >= 0) {
            addStyle(SpanStyle(color = amountColor), start = amountStart, end = amountStart + amount.length)
        }
    }
}

@Composable
private fun TitleText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
        textAlign = TextAlign.Center,
    )
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesAutoBuybackSheet_Preview() {
    TangemThemePreviewRedesign {
        AutoBuybackContent(
            amount = "+35.60 USDC",
            price = "50 USDC",
            cardImageUrl = "https://example.com/card.png",
            cardTitle = "2000 Bowman #236",
            onOpenAgainClick = {},
            onExplorePacksClick = {},
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesAutoBuybackSheet_NoCard_Preview() {
    TangemThemePreviewRedesign {
        AutoBuybackContent(
            amount = "+35.60 USDC",
            price = "50 USDC",
            cardImageUrl = null,
            cardTitle = null,
            onOpenAgainClick = {},
            onExplorePacksClick = {},
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}