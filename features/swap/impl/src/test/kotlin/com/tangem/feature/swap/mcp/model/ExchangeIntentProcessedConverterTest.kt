package com.tangem.feature.swap.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.feature.swap.mcp.ExchangeIntentAmountConverter
import com.tangem.feature.swap.mcp.createCoin
import com.tangem.feature.swap.mcp.createExchangeIntent
import com.tangem.feature.swap.mcp.createNetwork
import com.tangem.feature.swap.mcp.createToken
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

@ResourceLock(Resources.LOCALE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExchangeIntentProcessedConverterTest {

    private val converter = ExchangeIntentProcessedConverter(
        swapTextConverter = ExchangeIntentSwapTextConverter(ExchangeIntentAmountConverter()),
    )

    private val usdt = createToken()
    private val pol = createCoin(network = createNetwork(rawId = "polygon-pos", name = "Polygon"), symbol = "POL")
    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUpLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN both currencies WHEN convert THEN the subtitle names the swap`(model: SubtitleModel) {
        // Arrange
        val intent = createExchangeIntent(amountType = model.amountType, amount = model.amount)

        // Act
        val actual = convert(intent = intent)

        // Assert
        assertThat(actual.subtitle).isEqualTo(stringReference(model.expectedSubtitle))
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        SubtitleModel(SwapAmountType.From, "42000000", "Swap 42.00 USDT to POL"),
        SubtitleModel(SwapAmountType.To, "2500000000000000000", "Swap USDT to 2.50 POL"),
    )

    @Test
    fun `GIVEN missing currency WHEN convert THEN there is no subtitle`() {
        // Act
        val actual = convert(from = null)

        // Assert
        assertThat(actual.subtitle).isNull()
    }

    @Test
    fun `GIVEN malformed amount WHEN convert THEN there is no subtitle`() {
        // Act
        val actual = convert(intent = createExchangeIntent(amount = "many"))

        // Assert
        assertThat(actual.subtitle).isNull()
    }

    @Test
    fun `GIVEN processed intent WHEN convert THEN the sheet is an info status closed by Got it`() {
        // Arrange
        var closed = false

        // Act
        val actual = convert(onClose = { closed = true })
        actual.primaryButton.onClick()

        // Assert
        assertThat(actual.variant).isEqualTo(TangemModalStatus.Variant.Info)
        assertThat(actual.title).isEqualTo(stringReference("This intent has already been processed"))
        assertThat(actual.primaryButton.text).isEqualTo(stringReference("Got it"))
        assertThat(closed).isTrue()
    }

    private fun convert(
        intent: ExchangeIntent = createExchangeIntent(),
        from: CryptoCurrency? = usdt,
        onClose: () -> Unit = {},
    ): ExchangeIntentUM.Status = converter.convert(
        ExchangeIntentProcessedConverter.Input(intent = intent, from = from, to = pol, onClose = onClose),
    )

    data class SubtitleModel(val amountType: SwapAmountType, val amount: String, val expectedSubtitle: String)
}