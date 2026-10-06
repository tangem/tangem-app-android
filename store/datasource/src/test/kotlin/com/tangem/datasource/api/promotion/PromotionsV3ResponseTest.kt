package com.tangem.datasource.api.promotion

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.datasource.api.promotion.models.PromotionsV3Response
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.BlockItem
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.BlockType
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Content
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.DescriptionBlock
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Link
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.MatchMode
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.PayoutToken
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.PromotionDto
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Properties
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.TableCell
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.ValueType
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PromotionsV3ResponseTest {

    private val moshi = Moshi.Builder()
        .add(TestBigDecimalAdapter())
        .addPromotionEnumFallbackAdapters()
        .build()
    private val adapter = moshi.adapter(PromotionsV3Response::class.java)

    @Test
    fun `GIVEN full campaign json WHEN parsed THEN every block type mapped`() {
        // Arrange
        val json = """
            {"promotions":[{"id":1,"name":"example_campaign","alreadyEnrolled":false,
            "content":{"campaignName":"Летний стейкинг-бонус",
            "campaignDescription":"Кэшбек за стейкинг до конца лета",
            "publicDescription":[
              {"type":"text","text":"Застейкайте от 100 USDT и получите кэшбек",
              "link":{"text":"Learn more","url":"https://tangem.com/campaigns/example_campaign"}},
              {"type":"block","title":"Cashback and amounts","items":[
                {"label":"From ${'$'}10K","value":0.10,"valueType":"percent"},
                {"label":"From ${'$'}20K","value":0.20,"valueType":"percent"},
                {"label":"Weekly bonus","value":5,"valueType":"money","currency":"USDT"}]},
              {"type":"table","title":"Payout breakdown","headers":["Condition","Value"],
              "columnTypes":["text","money"],
              "rows":[[{"text":"Cashback"},{"value":5,"currency":"USDT"}],
                [{"text":"Gas refund"},{"value":0.002,"currency":"ETH"}]]},
              {"type":"highlight","title":"Deadline","text":"Акция действует только до 31 августа"}],
            "publicImage":"https://cdn.tangem.com/campaigns/example_campaign.png",
            "tncLink":{"text":"Terms and conditions","url":"https://tangem.com/campaigns/example_campaign/terms"},
            "actionButtonName":"Select cashback token"},
            "properties":{"payoutNeeded":true,"payoutTokens":[
              {"tokenAddress":"0xdac17f958d2ee523a2206206994597c13d831ec7","networkId":"ethereum",
              "tokenSymbol":"USDT","tokenName":"Tether USD","tokenId":"tether","decimals":6}],
            "matchMode":"linked_wallet"}}]}
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = PromotionsV3Response(
            promotions = listOf(
                createPromotion(
                    alreadyEnrolled = false,
                    properties = Properties(
                        isPayoutNeeded = true,
                        payoutTokens = listOf(
                            PayoutToken(
                                tokenAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7",
                                networkId = "ethereum",
                                tokenId = "tether",
                                tokenName = "Tether USD",
                                tokenSymbol = "USDT",
                                decimals = 6,
                            ),
                        ),
                        matchMode = MatchMode.LINKED_WALLET,
                    ),
                    publicDescription = listOf(
                        createBlock(
                            type = BlockType.TEXT,
                            text = "Застейкайте от 100 USDT и получите кэшбек",
                            link = Link(text = "Learn more", url = "https://tangem.com/campaigns/example_campaign"),
                        ),
                        createBlock(
                            type = BlockType.BLOCK,
                            title = "Cashback and amounts",
                            items = listOf(
                                BlockItem(
                                    label = "From \$10K",
                                    value = "0.10",
                                    valueType = ValueType.PERCENT,
                                    currency = null,
                                ),
                                BlockItem(
                                    label = "From \$20K",
                                    value = "0.20",
                                    valueType = ValueType.PERCENT,
                                    currency = null,
                                ),
                                BlockItem(
                                    label = "Weekly bonus",
                                    value = "5",
                                    valueType = ValueType.MONEY,
                                    currency = "USDT",
                                ),
                            ),
                        ),
                        createBlock(
                            type = BlockType.TABLE,
                            title = "Payout breakdown",
                            headers = listOf("Condition", "Value"),
                            columnTypes = listOf(ValueType.TEXT, ValueType.MONEY),
                            rows = listOf(
                                listOf(
                                    TableCell(text = "Cashback", value = null, currency = null),
                                    TableCell(text = null, value = BigDecimal("5"), currency = "USDT"),
                                ),
                                listOf(
                                    TableCell(text = "Gas refund", value = null, currency = null),
                                    TableCell(text = null, value = BigDecimal("0.002"), currency = "ETH"),
                                ),
                            ),
                        ),
                        createBlock(
                            type = BlockType.HIGHLIGHT,
                            title = "Deadline",
                            text = "Акция действует только до 31 августа",
                        ),
                    ),
                    campaignDescription = "Кэшбек за стейкинг до конца лета",
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unknown block type and no optional fields WHEN parsed THEN campaign mapped without description`() {
        // Arrange
        val json = """
            {"promotions":[{"id":1,"name":"example_campaign","alreadyEnrolled":true,
            "properties":{"payoutNeeded":false,"matchMode":"strict"},
            "content":{"campaignName":"Летний стейкинг-бонус",
            "publicDescription":[{"type":"carousel","images":["https://x/1.png"]}],
            "publicImage":"https://cdn.tangem.com/campaigns/example_campaign.png",
            "tncLink":{"text":"Terms and conditions","url":"https://tangem.com/campaigns/example_campaign/terms"},
            "actionButtonName":"Select cashback token"}}]}
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = PromotionsV3Response(
            promotions = listOf(
                createPromotion(
                    alreadyEnrolled = true,
                    properties = Properties(isPayoutNeeded = false, payoutTokens = null, matchMode = MatchMode.STRICT),
                    publicDescription = listOf(createBlock(type = BlockType.UNKNOWN)),
                    campaignDescription = null,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unknown match mode and value types WHEN parsed THEN mapped to UNKNOWN`() {
        // Arrange
        val json = """
            {"promotions":[{"name":"example_campaign","alreadyEnrolled":false,
            "properties":{"payoutNeeded":false,"matchMode":"all_wallets"},
            "content":{"campaignName":"Летний стейкинг-бонус",
            "publicDescription":[
              {"type":"block","items":[{"label":"Ratio","value":2,"valueType":"ratio"}]},
              {"type":"table","columnTypes":["date"],"rows":[[{"value":1}]]}],
            "publicImage":"https://cdn.tangem.com/campaigns/example_campaign.png",
            "tncLink":{"text":"Terms and conditions","url":"https://tangem.com/campaigns/example_campaign/terms"},
            "actionButtonName":"Select cashback token"}}]}
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = PromotionsV3Response(
            promotions = listOf(
                createPromotion(
                    id = null,
                    alreadyEnrolled = false,
                    properties = Properties(isPayoutNeeded = false, payoutTokens = null, matchMode = MatchMode.UNKNOWN),
                    publicDescription = listOf(
                        createBlock(
                            type = BlockType.BLOCK,
                            items = listOf(
                                BlockItem(label = "Ratio", value = "2", valueType = ValueType.UNKNOWN, currency = null),
                            ),
                        ),
                        createBlock(
                            type = BlockType.TABLE,
                            columnTypes = listOf(ValueType.UNKNOWN),
                            rows = listOf(listOf(TableCell(text = null, value = BigDecimal("1"), currency = null))),
                        ),
                    ),
                    campaignDescription = null,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun createPromotion(
        alreadyEnrolled: Boolean,
        properties: Properties,
        publicDescription: List<DescriptionBlock>,
        campaignDescription: String?,
        id: Int? = 1,
    ) = PromotionDto(
        id = id,
        name = "example_campaign",
        isAlreadyEnrolled = alreadyEnrolled,
        properties = properties,
        content = Content(
            campaignName = "Летний стейкинг-бонус",
            campaignDescription = campaignDescription,
            publicDescription = publicDescription,
            publicImage = "https://cdn.tangem.com/campaigns/example_campaign.png",
            tncLink = Link(
                text = "Terms and conditions",
                url = "https://tangem.com/campaigns/example_campaign/terms",
            ),
            actionButtonName = "Select cashback token",
        ),
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

    private class TestBigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }
}