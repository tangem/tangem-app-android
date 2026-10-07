package com.tangem.data.promo.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.*
import com.tangem.domain.promo.models.*
import com.tangem.domain.promo.models.PromoDescriptionBlock.Cell
import com.tangem.domain.promo.models.PromoDescriptionBlock.Item
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoCampaignV3ConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = PromoCampaignV3Converter.convert(model.dto)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ConvertModel(
        val description: String,
        val dto: PromotionDto,
        val expected: PromoCampaign?,
    ) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            description = "GIVEN minimal campaign WHEN convert THEN mapped",
            dto = createDto(),
            expected = createCampaign(),
        ),
        ConvertModel(
            description = "GIVEN linked_wallet match mode WHEN convert THEN LinkedWallet",
            dto = createDto(matchMode = MatchMode.LINKED_WALLET),
            expected = createCampaign(matchMode = PromoMatchMode.LinkedWallet),
        ),
        ConvertModel(
            description = "GIVEN unknown match mode WHEN convert THEN null",
            dto = createDto(matchMode = MatchMode.UNKNOWN),
            expected = null,
        ),
        ConvertModel(
            description = "GIVEN payout tokens WHEN convert THEN token without symbol dropped",
            dto = createDto(
                payoutTokens = listOf(
                    createToken(tokenSymbol = "USDT"),
                    createToken(tokenSymbol = null),
                ),
            ),
            expected = createCampaign(
                payoutTokens = listOf(
                    PromoPayoutToken(
                        tokenId = "tether",
                        tokenAddress = "0xdAC",
                        tokenSymbol = "USDT",
                        tokenName = "Tether",
                        networkId = "ethereum",
                        decimals = 6,
                    ),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN text and highlight blocks WHEN convert THEN mapped with text link",
            dto = createDto(
                blocks = listOf(
                    createBlock(
                        type = BlockType.TEXT,
                        title = "intro_title",
                        text = "intro",
                        link = Link(text = "learn_more", url = "https://tangem.com/c"),
                    ),
                    createBlock(type = BlockType.HIGHLIGHT, title = "deadline_title", text = "deadline"),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Text(
                        title = "intro_title",
                        text = "intro",
                        link = PromoLink(text = "learn_more", url = "https://tangem.com/c"),
                    ),
                    PromoDescriptionBlock.Highlight(title = "deadline_title", text = "deadline"),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN block items WHEN convert THEN value typed by valueType",
            dto = createDto(
                blocks = listOf(
                    createBlock(
                        type = BlockType.BLOCK,
                        title = "amounts",
                        items = listOf(
                            BlockItem(label = "note", value = "weekly", valueType = ValueType.TEXT, currency = null),
                            BlockItem(label = "count", value = "3", valueType = ValueType.NUMBER, currency = null),
                            BlockItem(
                                label = "tier",
                                value = "0.10",
                                valueType = ValueType.PERCENT,
                                currency = null
                            ),
                            BlockItem(label = "bonus", value = "5", valueType = ValueType.MONEY, currency = "USDT"),
                        ),
                    ),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Items(
                        title = "amounts",
                        items = listOf(
                            Item(label = "note", value = PromoValue.Text("weekly")),
                            Item(label = "count", value = PromoValue.Number(BigDecimal("3"))),
                            Item(label = "tier", value = PromoValue.Percent(BigDecimal("0.10"))),
                            Item(label = "bonus", value = PromoValue.Money(BigDecimal("5"), currency = "USDT")),
                        ),
                    ),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN invalid block items WHEN convert THEN only valid items kept",
            dto = createDto(
                blocks = listOf(
                    createBlock(
                        type = BlockType.BLOCK,
                        items = listOf(
                            BlockItem(label = "nan", value = "abc", valueType = ValueType.NUMBER, currency = null),
                            BlockItem(
                                label = "no_currency",
                                value = "5",
                                valueType = ValueType.MONEY,
                                currency = null
                            ),
                            BlockItem(
                                label = "unknown",
                                value = "5",
                                valueType = ValueType.UNKNOWN,
                                currency = null
                            ),
                            BlockItem(label = "ok", value = "7", valueType = ValueType.NUMBER, currency = null),
                        ),
                    ),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Items(
                        title = null,
                        items = listOf(Item(label = "ok", value = PromoValue.Number(BigDecimal("7")))),
                    ),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN table WHEN convert THEN cells typed by columnTypes and invalid rows dropped",
            dto = createDto(
                blocks = listOf(
                    createBlock(
                        type = BlockType.TABLE,
                        title = "payout",
                        headers = listOf("condition", "value"),
                        columnTypes = listOf(ValueType.TEXT, ValueType.MONEY),
                        rows = listOf(
                            listOf(createCell(text = "gas"), createCell(value = "0.002", currency = "ETH")),
                            listOf(createCell(text = "no_currency"), createCell(value = "1")),
                            listOf(createCell(text = "short_row")),
                        ),
                    ),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Table(
                        title = "payout",
                        headers = listOf("condition", "value"),
                        rows = listOf(
                            listOf(
                                Cell.Text(text = "gas"),
                                Cell.Value(PromoValue.Money(BigDecimal("0.002"), currency = "ETH")),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN unknown or incomplete blocks WHEN convert THEN dropped",
            dto = createDto(
                blocks = listOf(
                    createBlock(type = BlockType.UNKNOWN),
                    createBlock(type = BlockType.TEXT),
                    createBlock(type = BlockType.BLOCK, items = emptyList()),
                    createBlock(type = BlockType.TABLE, rows = listOf(listOf(createCell(text = "x")))),
                ),
            ),
            expected = createCampaign(blocks = emptyList()),
        ),
        ConvertModel(
            description = "GIVEN no campaign description WHEN convert THEN summary null",
            dto = createDto(campaignDescription = null),
            expected = createCampaign(summary = null),
        ),
        ConvertModel(
            description = "GIVEN missing id WHEN convert THEN null",
            dto = createDto(id = null),
            expected = null,
        ),
    )

    private fun createDto(
        matchMode: MatchMode = MatchMode.STRICT,
        payoutTokens: List<PayoutToken>? = null,
        blocks: List<DescriptionBlock> = emptyList(),
        id: Int? = CAMPAIGN_ID,
        campaignDescription: String? = "summary",
    ) = PromotionDto(
        id = id,
        name = "example_campaign",
        isAlreadyEnrolled = true,
        properties = Properties(
            isPayoutNeeded = payoutTokens != null,
            payoutTokens = payoutTokens,
            matchMode = matchMode,
        ),
        content = Content(
            campaignName = "title",
            campaignDescription = campaignDescription,
            publicDescription = blocks,
            publicImage = "https://cdn.tangem.com/c.png",
            tncLink = Link(text = "terms", url = "https://tangem.com/c/terms"),
            actionButtonName = "action",
        ),
    )

    private fun createCampaign(
        matchMode: PromoMatchMode = PromoMatchMode.Strict,
        payoutTokens: List<PromoPayoutToken>? = null,
        blocks: List<PromoDescriptionBlock> = emptyList(),
        summary: String? = "summary",
    ) = PromoCampaign(
        id = CAMPAIGN_ID,
        name = "example_campaign",
        isAlreadyEnrolled = true,
        payout = PromoPayout(isNeeded = payoutTokens != null, tokens = payoutTokens.orEmpty()),
        matchMode = matchMode,
        content = PromoCampaignContent(
            name = "title",
            summary = summary,
            description = blocks,
            imageUrl = "https://cdn.tangem.com/c.png",
            tncLink = PromoLink(text = "terms", url = "https://tangem.com/c/terms"),
            actionButtonName = "action",
        ),
    )

    private fun createToken(tokenSymbol: String?) = PayoutToken(
        tokenAddress = "0xdAC",
        networkId = "ethereum",
        tokenId = "tether",
        tokenName = "Tether",
        tokenSymbol = tokenSymbol,
        decimals = 6,
    )

    private fun createBlock(
        type: BlockType,
        title: String? = null,
        text: String? = null,
        items: List<BlockItem>? = null,
        headers: List<String>? = null,
        columnTypes: List<ValueType>? = null,
        rows: List<List<TableCell>>? = null,
        link: Link? = null,
    ) = DescriptionBlock(
        type = type,
        title = title,
        text = text,
        items = items,
        headers = headers,
        columnTypes = columnTypes,
        rows = rows,
        link = link,
    )

    private fun createCell(text: String? = null, value: String? = null, currency: String? = null) = TableCell(
        text = text,
        value = value?.let(::BigDecimal),
        currency = currency,
    )

    private companion object {
        const val CAMPAIGN_ID = 7
    }
}