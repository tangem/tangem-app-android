package com.tangem.features.promobanners.impl.campaigns.v2.converters

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.promo.models.PromoCampaign
import com.tangem.domain.promo.models.PromoDescriptionBlock
import com.tangem.domain.promo.models.PromoLink
import com.tangem.domain.promo.models.PromoValue
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoBlockUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignFooterUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoLinkUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoTokenSelectorUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

/**
 * Every `*Key` of the campaign is already localized by the backend (the request carries the app language),
 * so it is shown as-is.
 */
internal class PromoCampaignUMConverter(
    private val onLinkClick: (url: String) -> Unit,
    private val onActionClick: () -> Unit,
    private val onChooseTokenClick: () -> Unit,
    private val locale: Locale = Locale.getDefault(),
) : Converter<PromoCampaign, PromoCampaignUM> {

    override fun convert(value: PromoCampaign): PromoCampaignUM {
        val content = value.content
        val firstTextBlock = content.description.firstOrNull { it is PromoDescriptionBlock.Text }

        return PromoCampaignUM(
            imageUrl = content.imageUrl,
            title = stringReference(content.nameKey),
            subtitle = null,
            blocks = content.description.map { block ->
                convertBlock(
                    block = block,
                    learnMoreLink = content.learnMoreLink.takeIf { block === firstTextBlock },
                )
            }.toImmutableList(),
            tokenSelector = if (value.payout.isNeeded) {
                PromoTokenSelectorUM(selectedToken = null, selectedAccount = null, onClick = onChooseTokenClick)
            } else {
                null
            },
            footer = convertFooter(campaign = value, isPayoutTokenPending = value.payout.isNeeded),
        )
    }

    /** Until a required payout token is chosen, the action is choosing it — no terms yet. */
    fun convertFooter(campaign: PromoCampaign, isPayoutTokenPending: Boolean): PromoCampaignFooterUM {
        return if (isPayoutTokenPending) {
            PromoCampaignFooterUM(
                terms = null,
                buttonText = resourceReference(R.string.promo_campaign_select_token),
                onButtonClick = onActionClick,
            )
        } else {
            PromoCampaignFooterUM(
                terms = convertLink(campaign.content.tncLink),
                buttonText = stringReference(campaign.content.actionButtonNameKey),
                onButtonClick = onActionClick,
            )
        }
    }

    /** The backend has no dedicated slot for [learnMoreLink]; the design shows it inline in the first text block. */
    private fun convertBlock(block: PromoDescriptionBlock, learnMoreLink: PromoLink?): PromoBlockUM = when (block) {
        is PromoDescriptionBlock.Text -> PromoBlockUM.Text(
            title = block.titleKey?.let(::stringReference),
            text = stringReference(block.textKey),
            learnMore = learnMoreLink?.let(::convertLink),
        )
        is PromoDescriptionBlock.Items -> PromoBlockUM.Tiles(
            title = block.titleKey?.let(::stringReference),
            tiles = block.items.map { item ->
                PromoBlockUM.Tile(label = stringReference(item.labelKey), value = formatValue(item.value))
            }.toImmutableList(),
        )
        is PromoDescriptionBlock.Table -> PromoBlockUM.Table(
            title = block.titleKey?.let(::stringReference),
            headers = block.headerKeys.map(::stringReference).toImmutableList(),
            rows = block.rows.map { row -> row.map(::convertCell).toImmutableList() }.toImmutableList(),
        )
        is PromoDescriptionBlock.Highlight -> convertHighlight(block)
    }

    /** Without a title the design shows the text alone as the banner's single line. */
    private fun convertHighlight(block: PromoDescriptionBlock.Highlight): PromoBlockUM.Highlight {
        val title = block.titleKey ?: return PromoBlockUM.Highlight(
            title = stringReference(block.textKey),
            description = null,
        )

        return PromoBlockUM.Highlight(title = stringReference(title), description = stringReference(block.textKey))
    }

    private fun convertCell(cell: PromoDescriptionBlock.Cell): TextReference = when (cell) {
        is PromoDescriptionBlock.Cell.Text -> stringReference(cell.textKey)
        is PromoDescriptionBlock.Cell.Value -> formatValue(cell.value)
    }

    private fun convertLink(link: PromoLink) = PromoLinkUM(
        text = stringReference(link.textKey),
        onClick = { onLinkClick(link.url) },
    )

    private fun formatValue(value: PromoValue): TextReference {
        val text = when (value) {
            is PromoValue.Text -> value.text
            is PromoValue.Number -> value.value.format { simple(decimals = MAX_NUMBER_DECIMALS, locale = locale) }
            is PromoValue.Percent -> formatPercent(value.value)
            is PromoValue.Money -> formatMoney(value)
        }
        return stringReference(text)
    }

    /** The backend sends the percent itself (`0.10` is 0.10%), while the formatter expects a fraction. */
    private fun formatPercent(value: BigDecimal): String {
        return value.movePointLeft(2).format { percent(locale = locale) }
    }

    /** Payout currencies may be token symbols (e.g. `USDT`) that are not ISO 4217 codes; those read like crypto. */
    private fun formatMoney(money: PromoValue.Money): String {
        val fiatCurrency = runCatching { Currency.getInstance(money.currency) }.getOrNull()
        if (fiatCurrency == null) {
            val amount = money.value.format { simple(decimals = MAX_NUMBER_DECIMALS, locale = locale) }
            return "$amount ${money.currency}"
        }

        return money.value.format {
            fiat(
                fiatCurrencyCode = fiatCurrency.currencyCode,
                fiatCurrencySymbol = fiatCurrency.getSymbol(locale),
                locale = locale,
            ).compact()
        }
    }

    private companion object {
        const val MAX_NUMBER_DECIMALS = 2
    }
}