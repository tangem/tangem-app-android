package com.tangem.datasource.api.promotion

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tangem.datasource.api.promotion.models.PromotionsV3Response
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.BlockItem
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.BlockType
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Conditions
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Eligibility
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.EligibilityType
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.EligibilityWalletType
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Content
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.DescriptionBlock
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Link
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.MatchMode
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.PayoutToken
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.PromotionDto
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Properties
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.Status
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
            {"promotions":[{"id":7,"name":"example_campaign","status":"active",
            "startAt":"2026-06-01T00:00:00Z","endAt":"2026-08-31T23:59:59Z","alreadyEnrolled":false,
            "eligibility":{"type":"private","conditions":{"walletIds":["UID_001"],"partnerIds":["partner_a"],
            "walletType":"card","batchIds":["AF99 0088"],"minIosVersion":"6.1.0","minAndroidVersion":"6.1.0",
            "maxAndroidVersion":"6.3.0","countryExcluded":["US","CN"]}},
            "properties":{"payoutNeeded":true,"matchMode":"linked_wallet","payoutTokens":[
              {"tokenAddress":"0xdAC","networkId":"ethereum","tokenId":"tether","tokenName":"Tether",
              "tokenSymbol":"USDT","decimals":6}]},
            "content":{"campaignNameKey":"campaign.example_campaign.title",
            "publicDescription":[
              {"type":"text","textKey":"campaign.example_campaign.intro"},
              {"type":"block","titleKey":"campaign.example_campaign.cashback_amounts.title","items":[
                {"labelKey":"campaign.example_campaign.cashback_amounts.tier1","value":0.10,"valueType":"percent"},
                {"labelKey":"campaign.example_campaign.cashback_amounts.bonus","value":5,"valueType":"money",
                "currency":"USDT"},
                {"labelKey":"campaign.example_campaign.cashback_amounts.note","value":"weekly","valueType":"text"}]},
              {"type":"table","titleKey":"campaign.example_campaign.payout.title",
              "headersKeys":["campaign.example_campaign.payout.condition","campaign.example_campaign.payout.value"],
              "columnTypes":["text","money"],
              "rows":[[{"textKey":"campaign.example_campaign.payout.gas"},{"value":0.002,"currency":"ETH"}]]},
              {"type":"highlight","titleKey":"campaign.example_campaign.deadline_title",
              "textKey":"campaign.example_campaign.deadline"}],
            "publicImage":"https://cdn.tangem.com/campaigns/example_campaign.png",
            "learnMoreLink":{"textKey":"campaign.example_campaign.learn_more","url":"https://tangem.com/c"},
            "tncLink":{"textKey":"campaign.example_campaign.terms","url":"https://tangem.com/c/terms"},
            "actionButtonNameKey":"campaign.example_campaign.action_button"}}]}
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = PromotionsV3Response(
            promotions = listOf(
                createPromotion(
                    alreadyEnrolled = false,
                    eligibility = Eligibility(
                        type = EligibilityType.PRIVATE,
                        conditions = Conditions(
                            walletIds = listOf("UID_001"),
                            walletType = EligibilityWalletType.CARD,
                            batchIds = listOf("AF99 0088"),
                            countryExcluded = listOf("US", "CN"),
                            minAndroidVersion = "6.1.0",
                            maxAndroidVersion = "6.3.0",
                        ),
                    ),
                    properties = Properties(
                        isPayoutNeeded = true,
                        payoutTokens = listOf(
                            PayoutToken(
                                tokenAddress = "0xdAC",
                                networkId = "ethereum",
                                tokenId = "tether",
                                tokenName = "Tether",
                                tokenSymbol = "USDT",
                                decimals = 6,
                            ),
                        ),
                        matchMode = MatchMode.LINKED_WALLET,
                    ),
                    publicDescription = listOf(
                        createBlock(type = BlockType.TEXT, textKey = "campaign.example_campaign.intro"),
                        createBlock(
                            type = BlockType.BLOCK,
                            titleKey = "campaign.example_campaign.cashback_amounts.title",
                            items = listOf(
                                BlockItem(
                                    labelKey = "campaign.example_campaign.cashback_amounts.tier1",
                                    value = "0.10",
                                    valueType = ValueType.PERCENT,
                                    currency = null,
                                ),
                                BlockItem(
                                    labelKey = "campaign.example_campaign.cashback_amounts.bonus",
                                    value = "5",
                                    valueType = ValueType.MONEY,
                                    currency = "USDT",
                                ),
                                BlockItem(
                                    labelKey = "campaign.example_campaign.cashback_amounts.note",
                                    value = "weekly",
                                    valueType = ValueType.TEXT,
                                    currency = null,
                                ),
                            ),
                        ),
                        createBlock(
                            type = BlockType.TABLE,
                            titleKey = "campaign.example_campaign.payout.title",
                            headersKeys = listOf(
                                "campaign.example_campaign.payout.condition",
                                "campaign.example_campaign.payout.value",
                            ),
                            columnTypes = listOf(ValueType.TEXT, ValueType.MONEY),
                            rows = listOf(
                                listOf(
                                    TableCell(
                                        textKey = "campaign.example_campaign.payout.gas",
                                        value = null,
                                        currency = null,
                                    ),
                                    TableCell(textKey = null, value = BigDecimal("0.002"), currency = "ETH"),
                                ),
                            ),
                        ),
                        createBlock(
                            type = BlockType.HIGHLIGHT,
                            titleKey = "campaign.example_campaign.deadline_title",
                            textKey = "campaign.example_campaign.deadline",
                        ),
                    ),
                    learnMoreLink = Link(
                        textKey = "campaign.example_campaign.learn_more",
                        url = "https://tangem.com/c",
                    ),
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unknown block type and no optional fields WHEN parsed THEN campaign still mapped`() {
        // Arrange
        val json = """
            {"promotions":[{"id":7,"name":"example_campaign","status":"active",
            "startAt":"2026-06-01T00:00:00Z","endAt":"2026-08-31T23:59:59Z","alreadyEnrolled":true,
            "eligibility":{"type":"public"},
            "properties":{"payoutNeeded":false,"matchMode":"strict"},
            "content":{"campaignNameKey":"campaign.example_campaign.title",
            "publicDescription":[{"type":"carousel","images":["https://x/1.png"]}],
            "publicImage":"https://cdn.tangem.com/campaigns/example_campaign.png",
            "tncLink":{"textKey":"campaign.example_campaign.terms","url":"https://tangem.com/c/terms"},
            "actionButtonNameKey":"campaign.example_campaign.action_button"}}]}
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = PromotionsV3Response(
            promotions = listOf(
                createPromotion(
                    alreadyEnrolled = true,
                    eligibility = Eligibility(type = EligibilityType.PUBLIC, conditions = null),
                    properties = Properties(isPayoutNeeded = false, payoutTokens = null, matchMode = MatchMode.STRICT),
                    publicDescription = listOf(createBlock(type = BlockType.UNKNOWN)),
                    learnMoreLink = null,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unknown status, match mode, eligibility and value types WHEN parsed THEN mapped to UNKNOWN`() {
        // Arrange
        val json = """
            {"promotions":[{"name":"example_campaign","status":"expired",
            "startAt":"2026-06-01T00:00:00Z","endAt":"2026-08-31T23:59:59Z","alreadyEnrolled":false,
            "eligibility":{"type":"segment","conditions":{"walletType":"tablet"}},
            "properties":{"payoutNeeded":false,"matchMode":"all_wallets"},
            "content":{"campaignNameKey":"campaign.example_campaign.title",
            "publicDescription":[
              {"type":"block","items":[{"labelKey":"campaign.example_campaign.ratio","value":2,"valueType":"ratio"}]},
              {"type":"table","columnTypes":["date"],"rows":[[{"value":1}]]}],
            "publicImage":"https://cdn.tangem.com/campaigns/example_campaign.png",
            "tncLink":{"textKey":"campaign.example_campaign.terms","url":"https://tangem.com/c/terms"},
            "actionButtonNameKey":"campaign.example_campaign.action_button"}}]}
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = PromotionsV3Response(
            promotions = listOf(
                createPromotion(
                    id = null,
                    status = Status.UNKNOWN,
                    alreadyEnrolled = false,
                    eligibility = Eligibility(
                        type = EligibilityType.UNKNOWN,
                        conditions = Conditions(
                            walletIds = null,
                            walletType = EligibilityWalletType.UNKNOWN,
                            batchIds = null,
                            countryExcluded = null,
                            minAndroidVersion = null,
                            maxAndroidVersion = null,
                        ),
                    ),
                    properties = Properties(isPayoutNeeded = false, payoutTokens = null, matchMode = MatchMode.UNKNOWN),
                    publicDescription = listOf(
                        createBlock(
                            type = BlockType.BLOCK,
                            items = listOf(
                                BlockItem(
                                    labelKey = "campaign.example_campaign.ratio",
                                    value = "2",
                                    valueType = ValueType.UNKNOWN,
                                    currency = null,
                                ),
                            ),
                        ),
                        createBlock(
                            type = BlockType.TABLE,
                            columnTypes = listOf(ValueType.UNKNOWN),
                            rows = listOf(listOf(TableCell(textKey = null, value = BigDecimal("1"), currency = null))),
                        ),
                    ),
                    learnMoreLink = null,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun createPromotion(
        alreadyEnrolled: Boolean,
        eligibility: Eligibility,
        properties: Properties,
        publicDescription: List<DescriptionBlock>,
        learnMoreLink: Link?,
        status: Status = Status.ACTIVE,
        id: Int? = 7,
    ) = PromotionDto(
        id = id,
        name = "example_campaign",
        status = status,
        startAt = "2026-06-01T00:00:00Z",
        endAt = "2026-08-31T23:59:59Z",
        isAlreadyEnrolled = alreadyEnrolled,
        eligibility = eligibility,
        properties = properties,
        content = Content(
            campaignNameKey = "campaign.example_campaign.title",
            publicDescription = publicDescription,
            publicImage = "https://cdn.tangem.com/campaigns/example_campaign.png",
            learnMoreLink = learnMoreLink,
            tncLink = Link(textKey = "campaign.example_campaign.terms", url = "https://tangem.com/c/terms"),
            actionButtonNameKey = "campaign.example_campaign.action_button",
        ),
    )

    private fun createBlock(
        type: BlockType,
        titleKey: String? = null,
        textKey: String? = null,
        items: List<BlockItem>? = null,
        headersKeys: List<String>? = null,
        columnTypes: List<ValueType>? = null,
        rows: List<List<TableCell>>? = null,
    ) = DescriptionBlock(
        type = type,
        titleKey = titleKey,
        textKey = textKey,
        items = items,
        headersKeys = headersKeys,
        columnTypes = columnTypes,
        rows = rows,
    )

    private class TestBigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }
}