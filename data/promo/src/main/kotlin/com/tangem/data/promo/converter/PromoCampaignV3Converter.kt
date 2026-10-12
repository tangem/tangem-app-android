package com.tangem.data.promo.converter

import com.tangem.datasource.api.promotion.models.PromotionsV3Response.*
import com.tangem.domain.promo.models.*
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Campaigns are configured on the backend without an app release, so anything this app version can't
 * represent is dropped at the smallest possible scope — a block, item, row or token — instead of failing
 * the whole response. A campaign itself is dropped only when its identity-level fields are unusable.
 */
internal object PromoCampaignV3Converter : Converter<PromotionDto, PromoCampaign?> {

    override fun convert(value: PromotionDto): PromoCampaign? {
        return PromoCampaign(
            id = value.id ?: return null,
            name = value.name,
            isAlreadyEnrolled = value.isAlreadyEnrolled,
            payout = PromoPayout(
                isNeeded = value.properties.isPayoutNeeded,
                tokens = value.properties.payoutTokens.orEmpty().mapNotNull(::convertToken),
            ),
            matchMode = convertMatchMode(value.properties.matchMode) ?: return null,
            content = convertContent(value.content),
        )
    }

    private fun convertMatchMode(matchMode: MatchMode): PromoMatchMode? = when (matchMode) {
        MatchMode.STRICT -> PromoMatchMode.Strict
        MatchMode.LINKED_WALLET -> PromoMatchMode.LinkedWallet
        MatchMode.UNKNOWN -> null
    }

    private fun convertToken(token: PayoutToken): PromoPayoutToken? {
        return PromoPayoutToken(
            tokenId = token.tokenId,
            tokenAddress = token.tokenAddress,
            tokenSymbol = token.tokenSymbol ?: return null,
            tokenName = token.tokenName ?: return null,
            networkId = token.networkId,
            decimals = token.decimals,
        )
    }

    private fun convertContent(content: Content) = PromoCampaignContent(
        name = content.campaignName,
        summary = content.campaignDescription,
        description = content.publicDescription.mapNotNull(::convertBlock),
        imageUrl = content.publicImage,
        tncLink = content.tncLink.convert(),
        actionButtonName = content.actionButtonName,
    )

    private fun Link.convert() = PromoLink(text = text, url = url)

    private fun convertBlock(block: DescriptionBlock): PromoDescriptionBlock? = when (block.type) {
        BlockType.TEXT -> block.text?.let { text ->
            PromoDescriptionBlock.Text(title = block.title, text = text, link = block.link?.convert())
        }
        BlockType.HIGHLIGHT -> block.text?.let { text ->
            PromoDescriptionBlock.Highlight(title = block.title, text = text)
        }
        BlockType.BLOCK -> block.items.orEmpty().mapNotNull(::convertItem)
            .takeIf { it.isNotEmpty() }
            ?.let { PromoDescriptionBlock.Items(title = block.title, items = it) }
        BlockType.TABLE -> convertTable(block)
        BlockType.UNKNOWN -> null
    }

    private fun convertItem(item: BlockItem): PromoDescriptionBlock.Item? {
        val value = when (item.valueType) {
            ValueType.TEXT -> PromoValue.Text(item.value)
            else -> convertAmount(
                valueType = item.valueType,
                value = item.value.toBigDecimalOrNull() ?: return null,
                currency = item.currency,
            )
        } ?: return null

        return PromoDescriptionBlock.Item(label = item.label, value = value)
    }

    private fun convertTable(block: DescriptionBlock): PromoDescriptionBlock.Table? {
        val columnTypes = block.columnTypes ?: return null
        val rows = block.rows.orEmpty().mapNotNull { row -> convertRow(row, columnTypes) }
        if (rows.isEmpty()) return null

        return PromoDescriptionBlock.Table(
            title = block.title,
            headers = block.headers.orEmpty(),
            rows = rows,
        )
    }

    /** A row with any unrepresentable cell is dropped whole, so the remaining cells never shift columns. */
    private fun convertRow(row: List<TableCell>, columnTypes: List<ValueType>): List<PromoDescriptionBlock.Cell>? {
        if (row.size != columnTypes.size) return null

        return row.zip(columnTypes) { cell, columnType ->
            convertCell(cell, columnType) ?: return null
        }
    }

    private fun convertCell(cell: TableCell, columnType: ValueType): PromoDescriptionBlock.Cell? {
        if (columnType == ValueType.TEXT) {
            return cell.text?.let(PromoDescriptionBlock.Cell::Text)
        }

        val value = convertAmount(
            valueType = columnType,
            value = cell.value ?: return null,
            currency = cell.currency,
        ) ?: return null

        return PromoDescriptionBlock.Cell.Value(value)
    }

    private fun convertAmount(valueType: ValueType, value: BigDecimal, currency: String?): PromoValue? {
        return when (valueType) {
            ValueType.NUMBER -> PromoValue.Number(value)
            ValueType.PERCENT -> PromoValue.Percent(value)
            ValueType.MONEY -> currency?.let { PromoValue.Money(value = value, currency = it) }
            ValueType.TEXT,
            ValueType.UNKNOWN,
            -> null
        }
    }
}