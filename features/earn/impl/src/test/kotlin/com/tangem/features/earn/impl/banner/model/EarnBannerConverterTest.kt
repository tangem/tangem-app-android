package com.tangem.features.earn.impl.banner.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.banner.ui.state.EarnBannerUM
import com.tangem.features.earn.impl.overview.createRate
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnBannerConverterTest {

    private val converter = EarnBannerConverter()
    private val currency = MockCryptoCurrencyFactory().ethereum
    private val intents: EarnBannerClickIntents = mockk(relaxed = true)

    private var previousLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(intents)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `GIVEN null overview WHEN convert THEN Hidden`() {
        // Act
        val actual = converter.convert(
            overview = null,
            currency = currency,
            isBalanceHidden = false,
            intents = intents,
        )

        // Assert
        assertThat(actual).isEqualTo(EarnBannerUM.Hidden)
    }

    @Test
    fun `GIVEN overview without banner WHEN convert THEN Hidden`() {
        // Arrange
        val overview = EarnOverview(headline = null, banner = null)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)

        // Assert
        assertThat(actual).isEqualTo(EarnBannerUM.Hidden)
    }

    @Test
    fun `GIVEN no positions and a rate WHEN convert THEN Offer with title, rate description and Enable button`() {
        // Arrange
        val overview = overviewOf(activeCount = 0)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)

        // Assert
        assertThat(actual).isInstanceOf(EarnBannerUM.Offer::class.java)
        val offer = actual as EarnBannerUM.Offer
        assertThat(offer.title).isEqualTo(resourceReference(R.string.token_details_earn_title))
        assertThat(offer.description).isEqualTo(upToApyText("5.50%"))
        assertThat(offer.buttonText).isEqualTo(resourceReference(R.string.common_enable))
        offer.onEnableClick()
        verify(exactly = 1) { intents.onEnableClick(BEST_PRODUCT_ID) }
    }

    @Test
    fun `GIVEN no positions and no best product WHEN Enable is clicked THEN intent gets no preselected product`() {
        // Arrange
        val overview = overviewOf(rate = null, bestProductId = null, activeCount = 0)
        val offer = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)
            as EarnBannerUM.Offer

        // Act
        offer.onEnableClick()

        // Assert
        verify(exactly = 1) { intents.onEnableClick(null) }
    }

    @Test
    fun `GIVEN no positions and no rate WHEN convert THEN Offer without description`() {
        // Arrange
        val overview = overviewOf(rate = null, activeCount = 0)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)

        // Assert
        assertThat(actual).isInstanceOf(EarnBannerUM.Offer::class.java)
        val offer = actual as EarnBannerUM.Offer
        assertThat(offer.title).isEqualTo(resourceReference(R.string.token_details_earn_title))
        assertThat(offer.description).isNull()
        assertThat(offer.buttonText).isEqualTo(resourceReference(R.string.common_enable))
    }

    @Test
    fun `GIVEN positions and amount WHEN convert THEN Enabled with total and active count`() {
        // Arrange
        val totalPosition = BigDecimal("1.3254")
        val overview = overviewOf(activeCount = 1, totalPosition = totalPosition)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)

        // Assert
        assertThat(actual).isInstanceOf(EarnBannerUM.Enabled::class.java)
        val enabled = actual as EarnBannerUM.Enabled
        assertThat(enabled.title).isEqualTo(resourceReference(R.string.token_details_yield_enabled_title))
        assertThat(enabled.description)
            .isEqualTo(activeCountText(amount = totalPosition.format { crypto(currency) }, count = 1))
        enabled.onClick()
        verify(exactly = 1) { intents.onBannerClick() }
    }

    @Test
    fun `GIVEN positions and amount and hidden balance WHEN convert THEN Enabled masks the amount`() {
        // Arrange
        val overview = overviewOf(activeCount = 2, totalPosition = BigDecimal("1.3254"))

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = true, intents = intents)

        // Assert
        assertThat(actual).isInstanceOf(EarnBannerUM.Enabled::class.java)
        val enabled = actual as EarnBannerUM.Enabled
        assertThat(enabled.description).isEqualTo(activeCountText(amount = "***", count = 2))
    }

    @Test
    fun `GIVEN no positions and hidden balance WHEN convert THEN Offer keeps the rate`() {
        // Arrange
        val overview = overviewOf(activeCount = 0)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = true, intents = intents)

        // Assert
        assertThat(actual).isInstanceOf(EarnBannerUM.Offer::class.java)
        val offer = actual as EarnBannerUM.Offer
        assertThat(offer.description).isEqualTo(upToApyText("5.50%"))
    }

    @Test
    fun `GIVEN an APR rate WHEN convert THEN Offer shows the Up to APR text`() {
        // Arrange
        val overview = overviewOf(rate = createRate(fraction = "0.055", type = EarnRateType.APR), activeCount = 0)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)

        // Assert
        val offer = actual as EarnBannerUM.Offer
        assertThat(offer.description).isEqualTo(upToAprText("5.50%"))
    }

    @Test
    fun `GIVEN positions but unknown total WHEN convert THEN Enabled with only the active count`() {
        // Arrange
        val overview = overviewOf(activeCount = 2, totalPosition = null)

        // Act
        val actual = converter.convert(overview, currency, isBalanceHidden = false, intents = intents)

        // Assert
        assertThat(actual).isInstanceOf(EarnBannerUM.Enabled::class.java)
        val enabled = actual as EarnBannerUM.Enabled
        assertThat(enabled.title).isEqualTo(resourceReference(R.string.token_details_yield_enabled_title))
        assertThat(enabled.description).isEqualTo(activeCountText(amount = null, count = 2))
    }

    private fun overviewOf(
        rate: EarnProductRate? = createRate("0.055"),
        bestProductId: EarnProductId? = BEST_PRODUCT_ID,
        activeCount: Int,
        totalPosition: BigDecimal? = null,
    ) = EarnOverview(
        headline = null,
        banner = EarnBannerOverview(
            rate = rate,
            bestProductId = bestProductId,
            activeCount = activeCount,
            totalPosition = totalPosition,
        ),
    )

    private companion object {

        val BEST_PRODUCT_ID = EarnProductId("lending")
    }
}