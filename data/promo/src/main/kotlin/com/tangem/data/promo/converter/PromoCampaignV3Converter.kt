package com.tangem.data.promo.converter

import com.tangem.datasource.api.promotion.models.PromotionsV3Response.*
import com.tangem.domain.promo.models.*
import com.tangem.utils.converter.Converter
import kotlinx.datetime.Instant
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
            status = convertStatus(value.status) ?: return null,
            timeline = PromoTimeline(
                start = parseInstant(value.startAt) ?: return null,
                end = parseInstant(value.endAt) ?: return null,
            ),
            isAlreadyEnrolled = value.isAlreadyEnrolled,
            eligibility = convertEligibility(value.eligibility ?: return null) ?: return null,
            payout = PromoPayout(
                isNeeded = value.properties.isPayoutNeeded,
                tokens = value.properties.payoutTokens.orEmpty().mapNotNull(::convertToken),
            ),
            matchMode = convertMatchMode(value.properties.matchMode) ?: return null,
            content = convertContent(value.content),
        )
    }

    private fun parseInstant(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()

    private fun convertStatus(status: Status): PromoCampaignStatus? = when (status) {
        Status.DRAFT -> PromoCampaignStatus.Draft
        Status.ACTIVE -> PromoCampaignStatus.Active
        Status.PAUSED -> PromoCampaignStatus.Paused
        Status.ARCHIVED -> PromoCampaignStatus.Archived
        Status.UNKNOWN -> null
    }

    /** An audience rule this app version can't read must not show the campaign to everyone. */
    private fun convertEligibility(eligibility: Eligibility): PromoEligibility? = when (eligibility.type) {
        EligibilityType.PUBLIC -> PromoEligibility.Public
        EligibilityType.PRIVATE -> PromoEligibility.Private(convertConditions(eligibility.conditions))
        EligibilityType.UNKNOWN -> null
    }

    private fun convertConditions(conditions: Conditions?) = PromoEligibilityConditions(
        walletIds = conditions?.walletIds,
        walletType = conditions?.walletType?.let(::convertWalletType),
        batchIds = conditions?.batchIds,
        countryExcluded = conditions?.countryExcluded,
        minAndroidVersion = conditions?.minAndroidVersion,
        maxAndroidVersion = conditions?.maxAndroidVersion,
    )

    /**
     * A converted (hot, upgraded to a card) wallet is not distinguishable from a card on the app side, so that
     * condition is left to the backend, which receives the wallet type with the request.
     */
    private fun convertWalletType(walletType: EligibilityWalletType): PromoWalletType? = when (walletType) {
        EligibilityWalletType.MOBILE -> PromoWalletType.Mobile
        EligibilityWalletType.CARD -> PromoWalletType.Card
        EligibilityWalletType.UNKNOWN,
        -> null
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
        nameKey = content.campaignNameKey,
        description = content.publicDescription.mapNotNull(::convertBlock),
        imageUrl = content.publicImage,
        learnMoreLink = content.learnMoreLink?.let(::convertLink),
        tncLink = convertLink(content.tncLink),
        actionButtonNameKey = content.actionButtonNameKey,
    )

    private fun convertLink(link: Link) = PromoLink(textKey = link.textKey, url = link.url)

    private fun convertBlock(block: DescriptionBlock): PromoDescriptionBlock? = when (block.type) {
        BlockType.TEXT -> block.textKey?.let { textKey ->
            PromoDescriptionBlock.Text(titleKey = block.titleKey, textKey = textKey)
        }
        BlockType.HIGHLIGHT -> block.textKey?.let { textKey ->
            PromoDescriptionBlock.Highlight(titleKey = block.titleKey, textKey = textKey)
        }
        BlockType.BLOCK -> block.items.orEmpty().mapNotNull(::convertItem)
            .takeIf { it.isNotEmpty() }
            ?.let { PromoDescriptionBlock.Items(titleKey = block.titleKey, items = it) }
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

        return PromoDescriptionBlock.Item(labelKey = item.labelKey, value = value)
    }

    private fun convertTable(block: DescriptionBlock): PromoDescriptionBlock.Table? {
        val columnTypes = block.columnTypes ?: return null
        val rows = block.rows.orEmpty().mapNotNull { row -> convertRow(row, columnTypes) }
        if (rows.isEmpty()) return null

        return PromoDescriptionBlock.Table(
            titleKey = block.titleKey,
            headerKeys = block.headersKeys.orEmpty(),
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
            return cell.textKey?.let(PromoDescriptionBlock.Cell::Text)
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