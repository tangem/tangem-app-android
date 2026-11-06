package com.tangem.features.earn.impl.picker.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.impl.fake.fakeSummary
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.picker.EarnPickerTexts
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM.ContentUM
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class EarnPickerConverterTest {

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val vault = createFakeEarnProduct(FakeEarnProductPreset.Vault)
    private val converter = EarnPickerConverter(
        registry = EarnProductsRegistry(products = setOf(lending, vault), rateSources = emptySet()),
        cta = EarnPickerCta(),
    )

    private val ready = fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.None)
    private val lendingSummary = fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.None)
        .copy(rateText = stringReference("lending rate"))
    private val vaultSummary = fakeSummary(FakeEarnProductPreset.Vault, FakeEarnPosition.None)
        .copy(rateText = stringReference("vault rate"))

    private val calls = mutableListOf<Call>()
    private val intents = object : EarnPickerClickIntents {
        override fun onProductClick(id: EarnProductId) {
            calls += Call.Product(id)
        }

        override fun onActionClick(product: EarnProduct, action: EarnAction) {
            calls += Call.Action(product, action)
        }

        override fun onBlockedClick(block: EarnBlock) {
            calls += Call.Blocked(block)
        }

        override fun onAddFundsClick() {
            calls += Call.AddFunds
        }

        override fun onRetryClick(product: EarnProduct) {
            calls += Call.Retry(product)
        }

        override fun onOverviewRetryClick() {
            calls += Call.OverviewRetry
        }

        override fun onInfoClick(block: EarnTextBlock) {
            calls += Call.Info(block)
        }

        override fun onLinkClick(url: String) {
            calls += Call.Link(url)
        }

        override fun onCloseClick() {
            calls += Call.Close
        }
    }

    @BeforeEach
    fun resetCalls() {
        calls.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Products {

        @Test
        fun `GIVEN two summaries WHEN convert THEN cards are in summary order with registry titles and rates`() {
            // Arrange
            val summaries = listOf(vaultSummary, lendingSummary)

            // Act
            val actual = convert(summaries = summaries, selectedId = lending.id).cardInfos()

            // Assert
            val expected = listOf(
                CardInfo(id = "vault", title = vault.title, rate = vaultSummary.rateText, isSelected = false),
                CardInfo(id = "lending", title = lending.title, rate = lendingSummary.rateText, isSelected = true),
            )
            assertThat(actual).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `GIVEN a card WHEN its onClick THEN intents get its product id`() {
            // Arrange
            val cards = convert().products

            // Act
            cards.last().onClick()

            // Assert
            assertThat(calls).containsExactly(Call.Product(vault.id))
        }

        @Test
        fun `GIVEN one summary WHEN convert THEN there are no cards`() {
            // Act
            val actual = convert(summaries = listOf(lendingSummary))

            // Assert
            assertThat(actual.products).isEmpty()
        }

        @Test
        fun `GIVEN no summaries WHEN convert THEN there are no cards and the page is still built`() {
            // Act
            val actual = convert(summaries = emptyList())

            // Assert
            assertThat(actual.products).isEmpty()
            assertThat(actual.content).isInstanceOf(ContentUM.Ready::class.java)
        }

        @Test
        fun `GIVEN a summary of a product missing in the registry WHEN convert THEN it has no card`() {
            // Arrange
            val unknown = lendingSummary.copy(productId = EarnProductId("unknown"))

            // Act
            val actual = convert(summaries = listOf(lendingSummary, unknown, vaultSummary)).cardInfos()

            // Assert
            assertThat(actual.map { it.id }).containsExactly("lending", "vault").inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sections {

        @ParameterizedTest
        @ProvideTestModels
        fun sections(model: SectionsModel) {
            // Act
            val actual = convert(state = model.state).readyContent().sections

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
        }

        private fun provideTestModels() = listOf(
            SectionsModel(
                state = ready,
                expected = listOf(EarnPickerTab.Calculator, EarnPickerTab.Benefits, EarnPickerTab.Details),
            ),
            SectionsModel(
                state = ready.copy(projection = null),
                expected = listOf(EarnPickerTab.Benefits, EarnPickerTab.Details),
            ),
            SectionsModel(
                state = ready.copy(benefits = emptyList()),
                expected = listOf(EarnPickerTab.Calculator, EarnPickerTab.Details),
            ),
            SectionsModel(
                state = ready.copy(details = emptyList()),
                expected = listOf(EarnPickerTab.Calculator, EarnPickerTab.Benefits),
            ),
        )

        @Test
        fun `GIVEN Ready without projection, benefits and details WHEN convert THEN there are no sections`() {
            // Arrange
            val state = ready.copy(projection = null, benefits = emptyList(), details = emptyList(), incentiveRate = null)

            // Act
            val actual = convert(state = state).content

            // Assert
            val expected = ContentUM.Ready(
                description = ready.description,
                incentive = null,
                sections = persistentListOf(),
                benefits = persistentListOf(),
                details = persistentListOf(),
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Incentive {

        @ParameterizedTest
        @ProvideTestModels
        fun incentive(model: IncentiveModel) {
            // Arrange
            val state = ready.copy(incentiveRate = model.rate)

            // Act
            val actual = convert(state = state).readyContent().incentive

            // Assert
            assertThat(actual != null).isEqualTo(model.hasBadge)
            actual?.let { assertThat(it.iconEnd).isNotNull() }
        }

        private fun provideTestModels() = listOf(
            IncentiveModel(rate = null, hasBadge = false),
            IncentiveModel(rate = EarnProductRate(BigDecimal.ZERO, EarnRateType.APY), hasBadge = false),
            IncentiveModel(rate = EarnProductRate(BigDecimal("0.014"), EarnRateType.APY), hasBadge = true),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Content {

        @Test
        fun `GIVEN Loading WHEN convert THEN content is Loading`() {
            // Act
            val actual = convert(state = EarnProductState.Loading).content

            // Assert
            assertThat(actual).isEqualTo(ContentUM.Loading)
        }

        @Test
        fun `GIVEN Unavailable WHEN convert THEN its onRetryClick calls intents with the product`() {
            // Arrange
            val content = convert(state = EarnProductState.Unavailable).content as ContentUM.Unavailable

            // Act
            content.onRetryClick()

            // Assert
            assertThat(calls).containsExactly(Call.Retry(lending))
        }

        @Test
        fun `GIVEN details rows WHEN convert THEN info and link rows report their clicks and a text row has none`() {
            // Arrange
            val info = EarnTextBlock(stringReference("title"), stringReference("description"))
            val withInfo = EarnRow(stringReference("with"), EarnValue.Text(stringReference("1")), info)
            val link = EarnRow(stringReference("link"), EarnValue.Link(stringReference("Aave"), "https://aave.com"))
            val plain = EarnRow(stringReference("plain"), EarnValue.Text(stringReference("2")))
            val state = ready.copy(details = listOf(withInfo, link, plain))

            // Act
            val details = convert(state = state).readyContent().details
            details[0].onInfoClick?.invoke()
            details[1].onValueClick?.invoke()

            // Assert
            assertThat(details[2].onInfoClick).isNull()
            assertThat(details[2].onValueClick).isNull()
            assertThat(calls).containsExactly(Call.Info(info), Call.Link("https://aave.com")).inOrder()
        }

        @Test
        fun `GIVEN benefits with an icon WHEN convert THEN benefits carry the icon, title and description`() {
            // Arrange
            val icon = TangemIconUM.Icon(Icons.ic_lightning_20)
            val benefit = EarnTextBlock(stringReference("No lock-ups"), stringReference("Anytime"), icon)
            val state = ready.copy(benefits = listOf(benefit, benefit.copy(icon = null)))

            // Act
            val actual = convert(state = state).readyContent().benefits

            // Assert
            val expected = listOf(
                EarnBenefitUM(icon = icon, title = benefit.title, description = benefit.description),
                EarnBenefitUM(icon = null, title = benefit.title, description = benefit.description),
            )
            assertThat(actual).containsExactlyElementsIn(expected).inOrder()
        }

        @Test
        fun `GIVEN Ready and enough ETH WHEN convert THEN texts and the button come from the product state`() {
            // Act
            val actual = convert(ethBalance = BigDecimal.ONE)
            actual.cta.onClick()
            actual.onClose()

            // Assert
            assertThat(actual.title).isEqualTo(EarnPickerTexts.title)
            assertThat(actual.subtitle).isEqualTo(EarnPickerTexts.subtitle)
            assertThat(actual.barTitle).isEqualTo(lending.title)
            assertThat(actual.readyContent().description).isEqualTo(ready.description)
            assertThat(actual.cta.text).isEqualTo(ready.primary.label)
            assertThat(actual.ctaHint).isNull()
            assertThat(calls).containsExactly(Call.Action(lending, ready.primary.action), Call.Close).inOrder()
        }

        @Test
        fun `GIVEN balance below the minimum WHEN cta clicked THEN intents get onAddFundsClick and the hint is shown`() {
            // Act
            val actual = convert(ethBalance = BigDecimal.ZERO)
            actual.cta.onClick()

            // Assert
            assertThat(actual.ctaHint).isEqualTo(EarnPickerTexts.minDepositHint(ready.limits.minDeposit))
            assertThat(calls).containsExactly(Call.AddFunds)
        }

        @Test
        fun `GIVEN blocked primary action WHEN cta clicked THEN intents get onBlockedClick with the block`() {
            // Arrange
            val block = EarnBlock(reason = stringReference("blocked"))
            val state = ready.copy(primary = ready.primary.copy(block = block))

            // Act
            convert(state = state).cta.onClick()

            // Assert
            assertThat(calls).containsExactly(Call.Blocked(block))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Placeholders {

        @Test
        fun `GIVEN initial WHEN called THEN Loading content, the fixed texts, no cards and a disabled button`() {
            // Act
            val actual = converter.initial(intents)
            actual.onClose()

            // Assert
            assertThat(actual.title).isEqualTo(EarnPickerTexts.title)
            assertThat(actual.subtitle).isEqualTo(EarnPickerTexts.subtitle)
            assertThat(actual.barTitle).isEqualTo(EarnPickerTexts.title)
            assertThat(actual.products).isEmpty()
            assertThat(actual.content).isEqualTo(ContentUM.Loading)
            assertThat(actual.cta.text).isEqualTo(EarnPickerTexts.startEarning)
            assertThat(actual.cta.isEnabled).isFalse()
            assertThat(actual.ctaHint).isNull()
            assertThat(calls).containsExactly(Call.Close)
        }

        @Test
        fun `GIVEN unavailable WHEN Retry clicked THEN the overview retry intent is called and the button is disabled`() {
            // Arrange
            val actual = converter.unavailable(intents)

            // Act
            (actual.content as ContentUM.Unavailable).onRetryClick()

            // Assert
            assertThat(actual.products).isEmpty()
            assertThat(actual.cta.text).isEqualTo(EarnPickerTexts.startEarning)
            assertThat(actual.cta.isEnabled).isFalse()
            assertThat(calls).containsExactly(Call.OverviewRetry)
        }
    }

    private fun convert(
        state: EarnProductState = ready,
        summaries: List<EarnProductSummary> = listOf(lendingSummary, vaultSummary),
        selectedId: EarnProductId = lending.id,
        ethBalance: BigDecimal? = BigDecimal.ONE,
    ): EarnPickerUM = converter.convert(
        product = lending,
        state = state,
        summaries = summaries,
        selectedId = selectedId,
        ethBalance = ethBalance,
        intents = intents,
    )

    private fun EarnPickerUM.readyContent(): ContentUM.Ready = content as ContentUM.Ready

    private fun EarnPickerUM.cardInfos(): List<CardInfo> = products.map {
        CardInfo(id = it.id, title = it.title, rate = it.rate, isSelected = it.isSelected)
    }

    internal data class SectionsModel(val state: EarnProductState.Ready, val expected: List<EarnPickerTab>)

    internal data class IncentiveModel(val rate: EarnProductRate?, val hasBadge: Boolean)

    private data class CardInfo(val id: String, val title: TextReference, val rate: TextReference, val isSelected: Boolean)

    private sealed interface Call {
        data class Product(val id: EarnProductId) : Call
        data class Action(val product: EarnProduct, val action: EarnAction) : Call
        data class Blocked(val block: EarnBlock) : Call
        data object AddFunds : Call
        data class Retry(val product: EarnProduct) : Call
        data object OverviewRetry : Call
        data class Info(val block: EarnTextBlock) : Call
        data class Link(val url: String) : Call
        data object Close : Call
    }
}