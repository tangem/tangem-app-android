package com.tangem.features.promobanners.impl.campaigns.v2.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.promo.models.PromoDescriptionBlock
import com.tangem.domain.promo.models.PromoDescriptionBlock.Cell
import com.tangem.domain.promo.models.PromoDescriptionBlock.Item
import com.tangem.domain.promo.models.PromoLink
import com.tangem.domain.promo.models.PromoValue
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.v2.PROMO_IMAGE_URL
import com.tangem.features.promobanners.impl.campaigns.v2.PROMO_TNC_URL
import com.tangem.features.promobanners.impl.campaigns.v2.createPromoCampaign
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoBlockUM
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoCampaignUMConverterTest {

    private val openedUrls = mutableListOf<String>()
    private var actionClicks = 0
    private var chooseTokenClicks = 0

    private val converter = PromoCampaignUMConverter(
        onLinkClick = { openedUrls += it },
        onActionClick = { actionClicks++ },
        onChooseTokenClick = { chooseTokenClicks++ },
        locale = Locale.US,
    )

    @BeforeEach
    fun reset() {
        openedUrls.clear()
        actionClicks = 0
        chooseTokenClicks = 0
    }

    @Test
    fun `GIVEN payout not needed WHEN convert THEN no token selector`() {
        // Act
        val actual = converter.convert(createPromoCampaign(isPayoutNeeded = false))

        // Assert
        assertThat(actual.tokenSelector).isNull()
    }

    @Test
    fun `GIVEN payout needed WHEN selector clicked THEN empty selector opens chooser`() {
        // Arrange
        val selector = converter.convert(createPromoCampaign(isPayoutNeeded = true)).tokenSelector

        // Act
        selector?.onClick?.invoke()

        // Assert
        assertThat(selector?.selectedToken).isNull()
        assertThat(selector?.selectedAccount).isNull()
        assertThat(chooseTokenClicks).isEqualTo(1)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun convertBlock(model: BlockModel) {
        // Act
        val actual = converter.convert(createPromoCampaign(blocks = listOf(model.block))).blocks

        // Assert
        assertThat(actual).containsExactly(model.expected)
    }

    @Test
    fun `GIVEN campaign content WHEN convert THEN header and footer mapped`() {
        // Act
        val actual = converter.convert(createPromoCampaign(blocks = emptyList()))

        // Assert
        assertThat(actual.imageUrl).isEqualTo(PROMO_IMAGE_URL)
        assertThat(actual.title).isEqualTo(stringReference("Whale cashback"))
        assertThat(actual.subtitle).isNull()
        assertThat(actual.footer.buttonText).isEqualTo(stringReference("Enroll"))
        assertThat(actual.footer.terms?.text).isEqualTo(stringReference("T&C"))
    }

    @Test
    fun `GIVEN payout needed WHEN convert THEN footer asks to select cashback token without terms`() {
        // Act
        val footer = converter.convert(createPromoCampaign(isPayoutNeeded = true)).footer

        // Assert
        assertThat(footer.terms).isNull()
        assertThat(footer.buttonText).isEqualTo(resourceReference(R.string.promo_campaign_select_token))
    }

    @Test
    fun `GIVEN footer WHEN terms and button clicked THEN tnc url opened and action invoked`() {
        // Arrange
        val footer = converter.convert(createPromoCampaign(blocks = emptyList())).footer

        // Act
        footer.terms?.onClick?.invoke()
        footer.onButtonClick()

        // Assert
        assertThat(openedUrls).containsExactly(PROMO_TNC_URL)
        assertThat(actionClicks).isEqualTo(1)
    }

    @Test
    fun `GIVEN two text blocks and learn more link WHEN convert THEN link attached to the first only`() {
        // Arrange
        val campaign = createPromoCampaign(
            blocks = listOf(
                PromoDescriptionBlock.Text(titleKey = null, textKey = "first"),
                PromoDescriptionBlock.Text(titleKey = null, textKey = "second"),
            ),
            learnMoreLink = PromoLink(textKey = "Learn more", url = LEARN_MORE_URL),
        )

        // Act
        val (first, second) = converter.convert(campaign).blocks.filterIsInstance<PromoBlockUM.Text>()
        first.learnMore?.onClick?.invoke()

        // Assert
        assertThat(first.learnMore?.text).isEqualTo(stringReference("Learn more"))
        assertThat(second.learnMore).isNull()
        assertThat(openedUrls).containsExactly(LEARN_MORE_URL)
    }

    @Test
    fun `GIVEN learn more link without text blocks WHEN convert THEN link dropped`() {
        // Arrange
        val campaign = createPromoCampaign(
            blocks = listOf(PromoDescriptionBlock.Highlight(titleKey = null, textKey = "note")),
            learnMoreLink = PromoLink(textKey = "Learn more", url = LEARN_MORE_URL),
        )

        // Act
        val actual = converter.convert(campaign).blocks

        // Assert
        assertThat(actual).containsExactly(PromoBlockUM.Highlight(title = stringReference("note"), description = null))
    }

    data class BlockModel(val block: PromoDescriptionBlock, val expected: PromoBlockUM)

    private fun provideTestModels() = listOf(
        BlockModel(
            block = PromoDescriptionBlock.Text(titleKey = "Limits", textKey = "Swaps are excluded"),
            expected = PromoBlockUM.Text(
                title = stringReference("Limits"),
                text = stringReference("Swaps are excluded"),
                learnMore = null,
            ),
        ),
        BlockModel(
            block = PromoDescriptionBlock.Highlight(titleKey = "You have \$30", textKey = "All tokens"),
            expected = PromoBlockUM.Highlight(
                title = stringReference("You have \$30"),
                description = stringReference("All tokens"),
            ),
        ),
        BlockModel(
            block = PromoDescriptionBlock.Highlight(titleKey = null, textKey = "You have \$30"),
            expected = PromoBlockUM.Highlight(title = stringReference("You have \$30"), description = null),
        ),
        BlockModel(
            block = PromoDescriptionBlock.Items(
                titleKey = "Cashback",
                items = listOf(
                    Item(labelKey = "From 10K", value = PromoValue.Percent(BigDecimal("0.10"))),
                    Item(labelKey = "Count", value = PromoValue.Number(BigDecimal("1234.5"))),
                    Item(labelKey = "Payout", value = PromoValue.Text("weekly")),
                    Item(labelKey = "Bonus", value = PromoValue.Money(BigDecimal("0.5"), currency = "USD")),
                    Item(labelKey = "Reward", value = PromoValue.Money(BigDecimal("0.5"), currency = "USDT")),
                ),
            ),
            expected = PromoBlockUM.Tiles(
                title = stringReference("Cashback"),
                tiles = persistentListOf(
                    tile(label = "From 10K", value = "0.10%"),
                    tile(label = "Count", value = "1,234.5"),
                    tile(label = "Payout", value = "weekly"),
                    tile(label = "Bonus", value = "\$0.50"),
                    tile(label = "Reward", value = "0.5 USDT"),
                ),
            ),
        ),
        BlockModel(
            block = PromoDescriptionBlock.Table(
                titleKey = null,
                headerKeys = listOf("Option", "Amount"),
                rows = listOf(
                    listOf(Cell.Text("Per swap"), Cell.Value(PromoValue.Number(BigDecimal("500")))),
                ),
            ),
            expected = PromoBlockUM.Table(
                title = null,
                headers = persistentListOf(stringReference("Option"), stringReference("Amount")),
                rows = persistentListOf(persistentListOf(stringReference("Per swap"), stringReference("500"))),
            ),
        ),
    )

    private fun tile(label: String, value: String) = PromoBlockUM.Tile(
        label = stringReference(label),
        value = stringReference(value),
    )

    private companion object {
        const val LEARN_MORE_URL = "https://tangem.com/learn"
    }
}