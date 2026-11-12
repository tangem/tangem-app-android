package com.tangem.features.earn.impl.sheet.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.overview.TestAction
import com.tangem.features.earn.impl.overview.createSummary
import com.tangem.features.earn.impl.sheet.EarnYieldSheetTexts
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

internal class EarnYieldSheetConverterTest {

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val staking = createFakeEarnProduct(FakeEarnProductPreset.Staking)
    private val vault = createFakeEarnProduct(FakeEarnProductPreset.Vault)
    private val converter = EarnYieldSheetConverter(
        registry = EarnProductsRegistry(products = setOf(lending, staking, vault), rateSources = emptySet()),
    )

    private val currency = MockCryptoCurrencyFactory().ethereum
    private val appCurrency = AppCurrency.Default
    private val intents: EarnYieldSheetClickIntents = mockk(relaxed = true)

    private val lendingSummary = createSummary(productId = lending.id)
    private val stakingSummary = createSummary(productId = staking.id)
    private val vaultSummary = createSummary(productId = vault.id)

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(intents)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Nested
    inner class Grouping {

        @Test
        fun `GIVEN summaries with and without position WHEN convert THEN active group first and the rest under inactive in input order`() {
            // Arrange
            val summaries = listOf(
                stakingSummary,
                lendingSummary.copy(position = EarnPositionSummary(BigDecimal("0.5031"))),
                vaultSummary,
            )

            // Act
            val actual = convert(summaries)

            // Assert
            assertThat(actual.active.map { it.id.value }).containsExactly("lending")
            assertThat(actual.inactive.map { it.id.value }).containsExactly("staking", "vault").inOrder()
            assertThat(actual.title).isEqualTo(EarnYieldSheetTexts.title)
            assertThat(actual.inactiveTitle).isEqualTo(EarnYieldSheetTexts.inactiveTitle)
        }

        @Test
        fun `GIVEN summary of an unregistered product WHEN convert THEN no row is produced`() {
            // Arrange
            val unknown = createSummary(
                productId = EarnProductId("unknown"),
                position = EarnPositionSummary(BigDecimal.ONE),
            )

            // Act
            val actual = convert(listOf(unknown, vaultSummary))

            // Assert
            assertThat(actual.active).isEmpty()
            assertThat(actual.inactive.map { it.id.value }).containsExactly("vault")
        }

        @Test
        fun `GIVEN empty summaries WHEN convert THEN both groups are empty`() {
            // Act
            val actual = convert(emptyList())

            // Assert
            assertThat(actual.active).isEmpty()
            assertThat(actual.inactive).isEmpty()
        }

        @Test
        fun `GIVEN no summaries WHEN empty THEN no rows and the titles are set`() {
            // Act
            val actual = converter.empty(intents)

            // Assert
            assertThat(actual.active).isEmpty()
            assertThat(actual.inactive).isEmpty()
            assertThat(actual.title).isEqualTo(EarnYieldSheetTexts.title)
            assertThat(actual.inactiveTitle).isEqualTo(EarnYieldSheetTexts.inactiveTitle)
        }
    }

    @Nested
    inner class Amounts {

        @Test
        fun `GIVEN position and fiat rate WHEN convert THEN amount is crypto text and fiat is amount times rate`() {
            // Arrange
            val position = EarnPositionSummary(BigDecimal("0.5031"))

            // Act
            val actual = convert(
                summaries = listOf(lendingSummary.copy(position = position)),
                fiatRate = BigDecimal("4000"),
            ).active.single()

            // Assert
            assertThat(actual.isActive).isTrue()
            assertThat(actual.amount).isEqualTo(stringReference(BigDecimal("0.5031").format { crypto(currency) }))
            assertThat(actual.amountFiat).isEqualTo(
                stringReference(BigDecimal("2012.4").format { fiat(appCurrency.code, appCurrency.symbol) }),
            )
            assertThat(actual.caption).isNull()
        }

        @Test
        fun `GIVEN position without fiat rate WHEN convert THEN amount is set and fiat is null`() {
            // Arrange
            val position = EarnPositionSummary(BigDecimal("0.5031"))

            // Act
            val actual = convert(
                summaries = listOf(lendingSummary.copy(position = position)),
                fiatRate = null,
            ).active.single()

            // Assert
            assertThat(actual.amount).isEqualTo(stringReference(BigDecimal("0.5031").format { crypto(currency) }))
            assertThat(actual.amountFiat).isNull()
        }

        @Test
        fun `GIVEN position with unknown amount WHEN convert THEN row is active with null amount and null fiat`() {
            // Arrange
            val position = EarnPositionSummary(cryptoAmount = null)

            // Act
            val actual = convert(
                summaries = listOf(lendingSummary.copy(position = position)),
                fiatRate = BigDecimal("4000"),
            ).active.single()

            // Assert
            assertThat(actual.isActive).isTrue()
            assertThat(actual.amount).isNull()
            assertThat(actual.amountFiat).isNull()
            assertThat(actual.caption).isNull()
        }

        @Test
        fun `GIVEN no position WHEN convert THEN row is inactive with the start earning caption and null amounts`() {
            // Act
            val actual = convert(
                summaries = listOf(lendingSummary),
                fiatRate = BigDecimal("4000"),
            ).inactive.single()

            // Assert
            assertThat(actual.isActive).isFalse()
            assertThat(actual.caption).isEqualTo(EarnYieldSheetTexts.startEarning)
            assertThat(actual.amount).isNull()
            assertThat(actual.amountFiat).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Risk {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN risk WHEN convert THEN risk badge has the mapped status, X4 size and text`(model: RiskModel) {
            // Act
            val actual = convert(listOf(lendingSummary.copy(risk = model.risk))).inactive.single().risk

            // Assert
            val expected = model.risk?.let {
                TangemBadgeUM(
                    text = EarnYieldSheetTexts.risk(it),
                    status = requireNotNull(model.status),
                    size = TangemBadge.Size.X4,
                )
            }
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            RiskModel(risk = EarnRisk.Low, status = TangemBadge.Status.Success),
            RiskModel(risk = EarnRisk.Medium, status = TangemBadge.Status.Warning),
            RiskModel(risk = EarnRisk.High, status = TangemBadge.Status.Error),
            RiskModel(risk = null, status = null),
        )
    }

    @Nested
    inner class RowFields {

        @Test
        fun `GIVEN row fields WHEN built THEN title and icon come from the registered product and rate from the summary rateText`() {
            // Arrange
            val summary = lendingSummary.copy(rateText = stringReference("lending rate"))

            // Act
            val actual = convert(listOf(summary)).inactive.single()

            // Assert
            assertThat(actual.id).isEqualTo(lending.id)
            assertThat(actual.title).isEqualTo(lending.title)
            assertThat(actual.icon).isEqualTo(lending.provider.icon)
            assertThat(actual.rate).isEqualTo(summary.rateText)
        }
    }

    @Nested
    inner class Intents {

        @Test
        fun `GIVEN built rows WHEN row onClick and sheet onDismiss are invoked THEN intents receive productId with onClickAction and onDismissClick`() {
            // Arrange
            val ui = convert(listOf(lendingSummary))

            // Act
            ui.inactive.single().onClick()
            ui.onDismiss()

            // Assert
            verify(exactly = 1) { intents.onActionClick(EarnProductId("lending"), TestAction) }
            verify(exactly = 1) { intents.onDismissClick() }
        }
    }

    private fun convert(summaries: List<EarnProductSummary>, fiatRate: BigDecimal? = null) = converter.convert(
        summaries = summaries,
        currency = currency,
        fiatRate = fiatRate,
        appCurrency = appCurrency,
        intents = intents,
    )

    internal data class RiskModel(val risk: EarnRisk?, val status: TangemBadge.Status?)
}