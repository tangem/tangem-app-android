package com.tangem.data.promo.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.promotion.models.PromotionsV3Response.*
import com.tangem.domain.promo.models.*
import com.tangem.domain.promo.models.PromoDescriptionBlock.Cell
import com.tangem.domain.promo.models.PromoDescriptionBlock.Item
import com.tangem.test.core.ProvideTestModels
import kotlinx.datetime.Instant
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
            description = "GIVEN paused status WHEN convert THEN Paused",
            dto = createDto(status = Status.PAUSED),
            expected = createCampaign(status = PromoCampaignStatus.Paused),
        ),
        ConvertModel(
            description = "GIVEN unknown status WHEN convert THEN null",
            dto = createDto(status = Status.UNKNOWN),
            expected = null,
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
            description = "GIVEN unparsable start date WHEN convert THEN null",
            dto = createDto(startAt = "2026-06-01"),
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
            description = "GIVEN text and highlight blocks WHEN convert THEN mapped",
            dto = createDto(
                blocks = listOf(
                    createBlock(type = BlockType.TEXT, titleKey = "intro_title", textKey = "intro"),
                    createBlock(type = BlockType.HIGHLIGHT, titleKey = "deadline_title", textKey = "deadline"),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Text(titleKey = "intro_title", textKey = "intro"),
                    PromoDescriptionBlock.Highlight(titleKey = "deadline_title", textKey = "deadline"),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN block items WHEN convert THEN value typed by valueType",
            dto = createDto(
                blocks = listOf(
                    createBlock(
                        type = BlockType.BLOCK,
                        titleKey = "amounts",
                        items = listOf(
                            BlockItem(labelKey = "note", value = "weekly", valueType = ValueType.TEXT, currency = null),
                            BlockItem(labelKey = "count", value = "3", valueType = ValueType.NUMBER, currency = null),
                            BlockItem(
                                labelKey = "tier",
                                value = "0.10",
                                valueType = ValueType.PERCENT,
                                currency = null
                            ),
                            BlockItem(labelKey = "bonus", value = "5", valueType = ValueType.MONEY, currency = "USDT"),
                        ),
                    ),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Items(
                        titleKey = "amounts",
                        items = listOf(
                            Item(labelKey = "note", value = PromoValue.Text("weekly")),
                            Item(labelKey = "count", value = PromoValue.Number(BigDecimal("3"))),
                            Item(labelKey = "tier", value = PromoValue.Percent(BigDecimal("0.10"))),
                            Item(labelKey = "bonus", value = PromoValue.Money(BigDecimal("5"), currency = "USDT")),
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
                            BlockItem(labelKey = "nan", value = "abc", valueType = ValueType.NUMBER, currency = null),
                            BlockItem(
                                labelKey = "no_currency",
                                value = "5",
                                valueType = ValueType.MONEY,
                                currency = null
                            ),
                            BlockItem(
                                labelKey = "unknown",
                                value = "5",
                                valueType = ValueType.UNKNOWN,
                                currency = null
                            ),
                            BlockItem(labelKey = "ok", value = "7", valueType = ValueType.NUMBER, currency = null),
                        ),
                    ),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Items(
                        titleKey = null,
                        items = listOf(Item(labelKey = "ok", value = PromoValue.Number(BigDecimal("7")))),
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
                        titleKey = "payout",
                        headersKeys = listOf("condition", "value"),
                        columnTypes = listOf(ValueType.TEXT, ValueType.MONEY),
                        rows = listOf(
                            listOf(createCell(textKey = "gas"), createCell(value = "0.002", currency = "ETH")),
                            listOf(createCell(textKey = "no_currency"), createCell(value = "1")),
                            listOf(createCell(textKey = "short_row")),
                        ),
                    ),
                ),
            ),
            expected = createCampaign(
                blocks = listOf(
                    PromoDescriptionBlock.Table(
                        titleKey = "payout",
                        headerKeys = listOf("condition", "value"),
                        rows = listOf(
                            listOf(
                                Cell.Text(textKey = "gas"),
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
                    createBlock(type = BlockType.TABLE, rows = listOf(listOf(createCell(textKey = "x")))),
                ),
            ),
            expected = createCampaign(blocks = emptyList()),
        ),
        ConvertModel(
            description = "GIVEN missing id WHEN convert THEN null",
            dto = createDto(id = null),
            expected = null,
        ),
        ConvertModel(
            description = "GIVEN missing eligibility WHEN convert THEN null",
            dto = createDto(eligibility = null),
            expected = null,
        ),
        ConvertModel(
            description = "GIVEN unknown eligibility type WHEN convert THEN null",
            dto = createDto(eligibility = Eligibility(type = EligibilityType.UNKNOWN, conditions = null)),
            expected = null,
        ),
        ConvertModel(
            description = "GIVEN private eligibility without conditions WHEN convert THEN nothing to check",
            dto = createDto(eligibility = Eligibility(type = EligibilityType.PRIVATE, conditions = null)),
            expected = createCampaign(eligibility = PromoEligibility.Private(createConditions())),
        ),
        ConvertModel(
            description = "GIVEN private eligibility with conditions WHEN convert THEN mapped",
            dto = createDto(
                eligibility = Eligibility(
                    type = EligibilityType.PRIVATE,
                    conditions = Conditions(
                        walletIds = listOf("UID_001"),
                        walletType = EligibilityWalletType.CARD,
                        batchIds = listOf("AF99"),
                        countryExcluded = listOf("US"),
                        minAndroidVersion = "6.1.0",
                        maxAndroidVersion = "6.3.0",
                    ),
                ),
            ),
            expected = createCampaign(
                eligibility = PromoEligibility.Private(
                    PromoEligibilityConditions(
                        walletIds = listOf("UID_001"),
                        walletType = PromoWalletType.Card,
                        batchIds = listOf("AF99"),
                        countryExcluded = listOf("US"),
                        minAndroidVersion = "6.1.0",
                        maxAndroidVersion = "6.3.0",
                    ),
                ),
            ),
        ),
        ConvertModel(
            description = "GIVEN mobile wallet type WHEN convert THEN mapped to mobile",
            dto = createDto(
                eligibility = Eligibility(
                    type = EligibilityType.PRIVATE,
                    conditions = createDtoConditions(walletType = EligibilityWalletType.MOBILE),
                ),
            ),
            expected = createCampaign(
                eligibility = PromoEligibility.Private(createConditions().copy(walletType = PromoWalletType.Mobile)),
            ),
        ),
        ConvertModel(
            description = "GIVEN unknown wallet type WHEN convert THEN wallet type not checked",
            dto = createDto(
                eligibility = Eligibility(
                    type = EligibilityType.PRIVATE,
                    conditions = createDtoConditions(walletType = EligibilityWalletType.UNKNOWN),
                ),
            ),
            expected = createCampaign(eligibility = PromoEligibility.Private(createConditions())),
        ),
    )

    private fun createDto(
        status: Status = Status.ACTIVE,
        startAt: String = START,
        matchMode: MatchMode = MatchMode.STRICT,
        payoutTokens: List<PayoutToken>? = null,
        blocks: List<DescriptionBlock> = emptyList(),
        eligibility: Eligibility? = Eligibility(type = EligibilityType.PUBLIC, conditions = null),
        id: Int? = CAMPAIGN_ID,
    ) = PromotionDto(
        id = id,
        name = "example_campaign",
        status = status,
        startAt = startAt,
        endAt = END,
        isAlreadyEnrolled = true,
        eligibility = eligibility,
        properties = Properties(
            isPayoutNeeded = payoutTokens != null,
            payoutTokens = payoutTokens,
            matchMode = matchMode,
        ),
        content = Content(
            campaignNameKey = "title",
            publicDescription = blocks,
            publicImage = "https://cdn.tangem.com/c.png",
            learnMoreLink = Link(textKey = "learn_more", url = "https://tangem.com/c"),
            tncLink = Link(textKey = "terms", url = "https://tangem.com/c/terms"),
            actionButtonNameKey = "action",
        ),
    )

    private fun createCampaign(
        status: PromoCampaignStatus = PromoCampaignStatus.Active,
        matchMode: PromoMatchMode = PromoMatchMode.Strict,
        payoutTokens: List<PromoPayoutToken>? = null,
        blocks: List<PromoDescriptionBlock> = emptyList(),
        eligibility: PromoEligibility = PromoEligibility.Public,
    ) = PromoCampaign(
        id = CAMPAIGN_ID,
        name = "example_campaign",
        status = status,
        timeline = PromoTimeline(start = Instant.parse(START), end = Instant.parse(END)),
        isAlreadyEnrolled = true,
        eligibility = eligibility,
        payout = PromoPayout(isNeeded = payoutTokens != null, tokens = payoutTokens.orEmpty()),
        matchMode = matchMode,
        content = PromoCampaignContent(
            nameKey = "title",
            description = blocks,
            imageUrl = "https://cdn.tangem.com/c.png",
            learnMoreLink = PromoLink(textKey = "learn_more", url = "https://tangem.com/c"),
            tncLink = PromoLink(textKey = "terms", url = "https://tangem.com/c/terms"),
            actionButtonNameKey = "action",
        ),
    )

    private fun createDtoConditions(walletType: EligibilityWalletType? = null) = Conditions(
        walletIds = null,
        walletType = walletType,
        batchIds = null,
        countryExcluded = null,
        minAndroidVersion = null,
        maxAndroidVersion = null,
    )

    private fun createConditions() = PromoEligibilityConditions(
        walletIds = null,
        walletType = null,
        batchIds = null,
        countryExcluded = null,
        minAndroidVersion = null,
        maxAndroidVersion = null,
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

    private fun createCell(textKey: String? = null, value: String? = null, currency: String? = null) = TableCell(
        textKey = textKey,
        value = value?.let(::BigDecimal),
        currency = currency,
    )

    private companion object {
        const val START = "2026-06-01T00:00:00Z"
        const val END = "2026-08-31T23:59:59Z"
        const val CAMPAIGN_ID = 7
    }
}