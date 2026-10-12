package com.tangem.features.promobanners.impl.campaigns.v2.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerW4
import com.tangem.core.ui.components.account.*
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.TokenItem
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.entity.*

private const val CAMPAIGN_IMAGE_ALPHA = 0.5f

/**
 * The campaign sheet body: [PromoCampaignBody] scrolls above a pinned [PromoCampaignFooter]. A plain `Box`, not a
 * scaffold: the hosting modal sizes itself from the content's intrinsic height, which a `SubcomposeLayout` cannot
 * report.
 */
@Composable
internal fun PromoCampaignContent(state: PromoCampaignUM, modifier: Modifier = Modifier) {
    var footerHeight by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        val bottomInset = with(LocalDensity.current) { footerHeight.toDp() } + 12.dp
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState()),
            content = {
                PromoCampaignBody(state = state)
                SpacerH(bottomInset)
            },
        )
        PromoCampaignFooter(
            state = state.footer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> footerHeight = size.height },
        )
    }
}

/**
 * Scrollable body of the campaign sheet. The footer is not part of it — see [PromoCampaignFooter].
 *
 * [Figma](https://www.figma.com/design/bLsAa8zSmjVTrr0RvHzwfg/%F0%9F%92%BB-Common-things?node-id=4718-168957&m=dev)
 */
@Composable
private fun PromoCampaignBody(state: PromoCampaignUM, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        PromoCampaignImage(imageUrl = state.imageUrl)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 256.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PromoCampaignTitle(title = state.title, subtitle = state.subtitle)

            state.tokenSelector?.let { PromoTokenSelector(it) }

            state.blocks.forEach { block ->
                when (block) {
                    is PromoBlockUM.Text -> PromoTextBlock(block)
                    is PromoBlockUM.Tiles -> PromoTilesBlock(block)
                    is PromoBlockUM.Table -> PromoTableBlock(block)
                    is PromoBlockUM.Highlight -> PromoHighlightBlock(block)
                }
            }
        }
    }
}

@Composable
private fun PromoCampaignImage(imageUrl: String) {
    val background = TangemTheme.colors3.bg.primary
    val fallback = ColorPainter(TangemTheme.colors3.bg.secondary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(388.dp),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            placeholder = fallback,
            error = fallback,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(136.dp)
                .background(
                    Brush.verticalGradient(0f to background.copy(alpha = 0f), CAMPAIGN_IMAGE_ALPHA to background),
                ),
        )
    }
}

@Composable
private fun PromoCampaignTitle(title: TextReference, subtitle: TextReference?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        if (subtitle != null) {
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun PromoTokenSelector(state: PromoTokenSelectorUM) {
    if (state.selectedToken != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (state.selectedAccount != null) {
                    stringResourceSafe(R.string.promo_campaign_selected_cashback_account)
                } else {
                    stringResourceSafe(R.string.promo_campaign_selected_token)
                },
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            val itemModifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(TangemTheme.colors3.bg.opaque.primary)

            PromoSelectedTokenItem(
                selectedToken = state.selectedToken,
                selectedAccount = state.selectedAccount,
                modifier = itemModifier.clickable(onClick = state.onClick),
            )
        }
    }
}

@Composable
private fun PromoSelectedTokenItem(
    selectedToken: TokenItemState,
    selectedAccount: PromoSelectedAccountUM?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (selectedAccount != null) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (val icon = selectedAccount.iconState) {
                    is CurrencyIconState.PaymentAccount -> PaymentAccountIcon(size = icon.size)
                    is CurrencyIconState.PredictionAccount -> PredictionAccountResIcon(size = icon.size)
                    is CurrencyIconState.CryptoPortfolio.Icon -> AccountResIcon(
                        imageVector = icon.imageVector(),
                        color = icon.color,
                        size = AccountIconSize.ExtraSmall,
                    )
                    is CurrencyIconState.CryptoPortfolio.Letter -> AccountCharIcon(
                        char = icon.char.resolveReference().first(),
                        color = icon.color,
                        size = AccountIconSize.ExtraSmall,
                    )
                    is CurrencyIconState.CoinIcon,
                    is CurrencyIconState.CustomTokenIcon,
                    is CurrencyIconState.Empty,
                    is CurrencyIconState.FiatIcon,
                    CurrencyIconState.Loading,
                    CurrencyIconState.Locked,
                    is CurrencyIconState.TokenIcon,
                    -> Unit
                }

                SpacerW4()

                Text(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .alignByBaseline(),
                    text = selectedAccount.name.resolveReference(),
                    color = TangemTheme.colors.text.primary1,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TangemTheme.typography.caption1,
                )
            }

            SpacerH8()
        }

        TokenItem(
            state = selectedToken,
            isBalanceHidden = false,
            itemPaddingValues = PaddingValues(horizontal = 16.dp),
        )
    }
}

/** Pinned bottom of the campaign sheet; meant to be placed inside a modal, hence the modal insets and no blur. */
@Composable
internal fun PromoCampaignFooter(state: PromoCampaignFooterUM, modifier: Modifier = Modifier) {
    val hasTerms = state.terms != null
    TangemFooter(
        modifier = modifier.fillMaxWidth(),
        windowInsets = TangemFooter.ModalWindowInsets,
        fadeVariant = if (hasTerms) {
            TangemFade.Variant.Hard
        } else {
            TangemFade.Variant.Soft
        },
        blurBackground = hasTerms,
        extraTop = if (hasTerms) {
            {
                Text(
                    text = buildAnnotatedString {
                        appendColored(
                            text = stringResourceSafe(R.string.promo_campaign_terms_agreement_android),
                            color = TangemTheme.colors3.text.secondary,
                        )
                        appendSpace()
                        withLink(
                            link = LinkAnnotation.Clickable(
                                tag = "CAMPAIGN_LINK",
                                linkInteractionListener = { state.terms.onClick() },
                            ),
                            block = {
                                appendColored(
                                    text = state.terms.text.resolveReference(),
                                    color = TangemTheme.colors3.text.primary,
                                )
                            },
                        )
                    },
                    style = TangemTheme.typography3.caption.medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            null
        },
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = state.buttonText,
            onClick = state.onButtonClick,
            isLoading = state.isLoading,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 402, heightDp = 1100)
@Preview(showBackground = true, widthDp = 402, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignBody() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PromoCampaignBody(state = PromoCampaignPreviewData.campaign)
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignBody_WithFooter() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PromoCampaignBody(
                state = PromoCampaignPreviewData.campaign.copy(blocks = PromoCampaignPreviewData.shortBlocks),
            )
            PromoCampaignFooter(
                state = PromoCampaignPreviewData.campaign.footer,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_PromoCampaignBody_WithSelectedToken() {
    TangemThemePreviewRedesign {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PromoCampaignBody(state = PromoCampaignPreviewData.campaignWithSelectedToken)
        }
    }
}
// endregion