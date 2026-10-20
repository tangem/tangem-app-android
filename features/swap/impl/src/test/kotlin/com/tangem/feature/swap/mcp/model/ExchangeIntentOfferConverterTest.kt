package com.tangem.feature.swap.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.feature.swap.mcp.ExchangeIntentAmountConverter
import com.tangem.feature.swap.mcp.ExchangeIntentSwapRouteConverter
import com.tangem.feature.swap.mcp.createCoin
import com.tangem.feature.swap.mcp.createExchangeIntent
import com.tangem.feature.swap.mcp.createNetwork
import com.tangem.feature.swap.mcp.createToken
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@ResourceLock(Resources.LOCALE)
internal class ExchangeIntentOfferConverterTest {

    private val converter = ExchangeIntentOfferConverter(amountConverter = ExchangeIntentAmountConverter())

    private val usdt = createToken()
    private val pol = createCoin(network = createNetwork(rawId = "polygon-pos", name = "Polygon"), symbol = "POL")
    private val claude = McpAgent(
        clientId = "client",
        origin = "mcp.claude.com",
        name = "Claude",
        iconUrl = "https://icons.tangem.com/claude.png",
    )
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Titles {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN amount side and locale WHEN convert THEN title formats the fixed side`(model: TitleModel) {
            // Arrange
            Locale.setDefault(model.locale)
            val intent = createExchangeIntent(amountType = model.amountType, amount = model.amount)

            // Act
            val actual = convert(intent = intent)

            // Assert
            assertThat(actual?.title).isEqualTo(stringReference(model.expectedTitle))
        }

        private fun provideTestModels() = listOf(
            TitleModel(
                Locale.US,
                SwapAmountType.From,
                "42000000",
                "mcp.claude.com Agent wants to swap 42.00\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.From,
                "42000000",
                "mcp.claude.com Agent wants to swap 42,00\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "2500000000000000000",
                "mcp.claude.com Agent wants to swap USDT to 2.50\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "2500000000000000000",
                "mcp.claude.com Agent wants to swap USDT to 2,50\u00a0POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.From,
                "1234567890",
                "mcp.claude.com Agent wants to swap 1,234.56789\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.From,
                "1234567890",
                "mcp.claude.com Agent wants to swap 1.234,56789\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.FRANCE,
                SwapAmountType.From,
                "1234567890",
                "mcp.claude.com Agent wants to swap 1\u202f234,56789\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "1234567890000000000000",
                "mcp.claude.com Agent wants to swap USDT to 1,234.56789\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "1234567890000000000000",
                "mcp.claude.com Agent wants to swap USDT to 1.234,56789\u00a0POL",
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AgentIdentity {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN agent metadata WHEN convert THEN headline prefers origin and card preserves verified identity`(
            model: AgentModel,
        ) {
            // Arrange
            val intent = createExchangeIntent(
                amountType = model.amountType,
                amount = if (model.amountType == SwapAmountType.From) "42000000" else "2500000000000000000",
            )
            val swap = when (model.amountType) {
                SwapAmountType.From -> "42.00\u00a0USDT to POL"
                SwapAmountType.To -> "USDT to 2.50\u00a0POL"
            }
            val expectedTitle = stringReference("${model.expectedIdentity} Agent wants to swap $swap")
            val expected = model.agent.origin
                ?.let { ExchangeIntentAgentUM.Verified(iconUrl = model.agent.iconUrl, label = stringReference(it)) }
                ?: ExchangeIntentAgentUM.Unknown(iconUrl = model.agent.iconUrl, label = stringReference("Unknown"))

            // Act
            val actual = convert(intent = intent, agent = model.agent)

            // Assert
            assertThat(actual?.title).isEqualTo(expectedTitle)
            assertThat(actual?.agent).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            AgentModel(claude, expectedIdentity = "mcp.claude.com"),
            AgentModel(claude.copy(iconUrl = null), expectedIdentity = "mcp.claude.com"),
            AgentModel(claude.copy(name = null), expectedIdentity = "mcp.claude.com"),
            AgentModel(
                claude.copy(name = "Tangem", origin = "agent.example.com"),
                expectedIdentity = "agent.example.com",
            ),
            AgentModel(claude.copy(origin = null), expectedIdentity = "Claude"),
            AgentModel(claude.copy(origin = null, iconUrl = null), expectedIdentity = "Claude"),
            AgentModel(claude.copy(origin = null, name = null), expectedIdentity = "Unknown"),
            AgentModel(McpAgent.unknown(clientId = "client"), expectedIdentity = "Unknown"),
        ).flatMap { model ->
            SwapAmountType.entries.map { side -> model.copy(amountType = side) }
        }
    }

    @Test
    fun `GIVEN unknown agent WHEN convert THEN title uses generic name without inventing a host`() {
        // Arrange
        val intent = createExchangeIntent(amountType = SwapAmountType.From, amount = "42000000")
        val expected = stringReference("Unknown Agent wants to swap 42.00\u00a0USDT to POL")

        // Act
        val actual = convert(intent = intent, agent = McpAgent.unknown(clientId = "client"))

        // Assert
        assertThat(actual?.title).isEqualTo(expected)
    }

    @Test
    fun `GIVEN unverified name without origin WHEN convert THEN title keeps the name and card uses generic fallback`() {
        // Arrange
        val intent = createExchangeIntent(amountType = SwapAmountType.From, amount = "42000000")
        val expected = ExchangeIntentAgentUM.Unknown(iconUrl = claude.iconUrl, label = stringReference("Unknown"))

        // Act
        val actual = convert(intent = intent, agent = claude.copy(origin = null))

        // Assert
        assertThat(actual?.title).isEqualTo(stringReference("Claude Agent wants to swap 42.00\u00a0USDT to POL"))
        assertThat(actual?.agent).isEqualTo(expected)
    }

    @Test
    fun `GIVEN token and coin WHEN convert THEN only the token has the network badge`() {
        // Arrange
        val expectedFrom = TangemTokenIcon.UiState.Token(
            TangemTokenIcon.State(url = usdt.iconUrl, topIcon = usdt.networkIconResId),
        )
        val expectedTo = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = pol.iconUrl))

        // Act
        val actual = convert()

        // Assert
        assertThat(actual?.fromToken).isEqualTo(expectedFrom)
        assertThat(actual?.toToken).isEqualTo(expectedTo)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvalidAmounts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN invalid or non-positive amount WHEN convert THEN there is no offer`(model: AmountModel) {
            // Arrange
            val intent = createExchangeIntent(amountType = model.amountType, amount = model.amount)

            // Act
            val actual = convert(intent = intent)

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = SwapAmountType.entries.flatMap { side ->
            listOf("many", "0", "0.000", "-0", "-5").map { amount ->
                AmountModel(amountType = side, amount = amount)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PositiveAmounts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN positive boundary amount WHEN convert THEN title stays nonzero and offer is actionable`(
            model: TitleModel,
        ) {
            // Arrange
            Locale.setDefault(model.locale)
            val intent = createExchangeIntent(amountType = model.amountType, amount = model.amount)
            var proceeded = false

            // Act
            val actual = requireNotNull(
                convert(
                    intent = intent,
                    onProceed = { proceeded = true },
                ),
            )
            actual.primaryButton.onClick()

            // Assert
            assertThat(actual.title).isEqualTo(stringReference(model.expectedTitle))
            assertThat(proceeded).isTrue()
        }

        private fun provideTestModels() = listOf(
            TitleModel(
                Locale.US,
                SwapAmountType.From,
                "1",
                "mcp.claude.com Agent wants to swap 0.000001\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.From,
                "1",
                "mcp.claude.com Agent wants to swap 0,000001\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.From,
                "2",
                "mcp.claude.com Agent wants to swap 0.000002\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.From,
                "2",
                "mcp.claude.com Agent wants to swap 0,000002\u00a0USDT to POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "1",
                "mcp.claude.com Agent wants to swap USDT to <0.00000001\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "1",
                "mcp.claude.com Agent wants to swap USDT to <0,00000001\u00a0POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "9999999999",
                "mcp.claude.com Agent wants to swap USDT to <0.00000001\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "9999999999",
                "mcp.claude.com Agent wants to swap USDT to <0,00000001\u00a0POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "10000000000",
                "mcp.claude.com Agent wants to swap USDT to 0.00000001\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "10000000000",
                "mcp.claude.com Agent wants to swap USDT to 0,00000001\u00a0POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "10000000001",
                "mcp.claude.com Agent wants to swap USDT to 0.00000001\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "10000000001",
                "mcp.claude.com Agent wants to swap USDT to 0,00000001\u00a0POL",
            ),
            TitleModel(
                Locale.US,
                SwapAmountType.To,
                "19999999999",
                "mcp.claude.com Agent wants to swap USDT to 0.00000001\u00a0POL",
            ),
            TitleModel(
                Locale.GERMANY,
                SwapAmountType.To,
                "19999999999",
                "mcp.claude.com Agent wants to swap USDT to 0,00000001\u00a0POL",
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DirectionalRounding {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN excess precision WHEN converting offer and route THEN title bounds amount and route stays exact`(
            model: RoundingModel,
        ) {
            // Arrange
            Locale.setDefault(model.locale)
            val fixedCurrency = pol.copy(displayDecimals = model.displayDecimals)
            val baseIntent = createExchangeIntent(amountType = model.amountType, amount = model.amount)
            val intent = when (model.amountType) {
                SwapAmountType.From -> baseIntent.copy(from = baseIntent.to, to = baseIntent.from)
                SwapAmountType.To -> baseIntent
            }
            val from = if (model.amountType == SwapAmountType.From) fixedCurrency else usdt
            val to = if (model.amountType == SwapAmountType.To) fixedCurrency else usdt
            val localizedAmount = if (model.locale == Locale.GERMANY) {
                model.expectedAmount.replace('.', ',')
            } else {
                model.expectedAmount
            }
            val swap = when (model.amountType) {
                SwapAmountType.From -> "$localizedAmount\u00a0POL to USDT"
                SwapAmountType.To -> "USDT to $localizedAmount\u00a0POL"
            }
            var proceeded = false
            val expectedAmount = when (model.amountType) {
                SwapAmountType.From -> BigDecimal(model.amount).movePointLeft(18).stripTrailingZeros()
                SwapAmountType.To -> null
            }
            val routeConverter = ExchangeIntentSwapRouteConverter(ExchangeIntentAmountConverter())

            // Act
            val actual = requireNotNull(convert(intent = intent, from = from, to = to, onProceed = { proceeded = true }))
            val route = routeConverter.convert(
                ExchangeIntentSwapRouteConverter.Input(
                    intent = intent,
                    userWalletId = UserWalletId("011"),
                    from = from,
                    to = to,
                ),
            )
            actual.primaryButton.onClick()

            // Assert
            assertThat(actual.title).isEqualTo(stringReference("mcp.claude.com Agent wants to swap $swap"))
            assertThat(proceeded).isTrue()
            assertThat(route.fromAmount).isEqualTo(expectedAmount)
        }

        private fun provideTestModels() = listOf(
            RoundingBoundary(0, "1", "<1", "<1"),
            RoundingBoundary(0, "1000000000000000000", "1", "1"),
            RoundingBoundary(0, "1000000000000000001", "2", "1"),
            RoundingBoundary(1, "1", "<0.1", "<0.1"),
            RoundingBoundary(1, "100000000000000000", "0.1", "0.1"),
            RoundingBoundary(1, "100000000000000001", "0.2", "0.1"),
            RoundingBoundary(6, "1", "<0.000001", "<0.000001"),
            RoundingBoundary(6, "999999999999", "<0.000001", "<0.000001"),
            RoundingBoundary(6, "1000000000000", "0.000001", "0.000001"),
            RoundingBoundary(6, "1000000000001", "0.000002", "0.000001"),
            RoundingBoundary(6, "1000000900000000000", "1.000001", "1.00"),
            RoundingBoundary(6, "999999999999999999", "1.00", "0.999999"),
            RoundingBoundary(8, "1", "<0.00000001", "<0.00000001"),
            RoundingBoundary(8, "9999999999", "<0.00000001", "<0.00000001"),
            RoundingBoundary(8, "10000000000", "0.00000001", "0.00000001"),
            RoundingBoundary(8, "10000000001", "0.00000002", "0.00000001"),
            RoundingBoundary(8, "19999999999", "0.00000002", "0.00000001"),
            RoundingBoundary(8, "1000000000000000001", "1.00000001", "1.00"),
            RoundingBoundary(18, "1", "<0.00000001", "<0.00000001"),
            RoundingBoundary(18, "9999999999", "<0.00000001", "<0.00000001"),
            RoundingBoundary(18, "10000000000", "0.00000001", "0.00000001"),
            RoundingBoundary(18, "10000000001", "0.00000002", "0.00000001"),
            RoundingBoundary(18, "19999999999", "0.00000002", "0.00000001"),
            RoundingBoundary(18, "1000000000000000001", "1.00000001", "1.00"),
        ).flatMap { boundary ->
            listOf(Locale.US, Locale.GERMANY).flatMap { locale ->
                listOf(
                    RoundingModel(
                        locale = locale,
                        amountType = SwapAmountType.From,
                        amount = boundary.amount,
                        displayDecimals = boundary.displayDecimals,
                        expectedAmount = boundary.expectedFrom,
                    ),
                    RoundingModel(
                        locale = locale,
                        amountType = SwapAmountType.To,
                        amount = boundary.amount,
                        displayDecimals = boundary.displayDecimals,
                        expectedAmount = boundary.expectedTo,
                    ),
                )
            }
        }
    }

    @Test
    fun `GIVEN offer WHEN its button is clicked THEN proceed is requested`() {
        // Arrange
        var proceeded = false

        // Act
        val offer = requireNotNull(convert(onProceed = { proceeded = true }))
        offer.primaryButton.onClick()

        // Assert
        assertThat(proceeded).isTrue()
    }

    private fun convert(
        intent: ExchangeIntent = createExchangeIntent(),
        agent: McpAgent = claude,
        from: CryptoCurrency = usdt,
        to: CryptoCurrency = pol,
        onProceed: () -> Unit = {},
    ): ExchangeIntentUM? = converter.convert(
        ExchangeIntentOfferConverter.Input(intent = intent, from = from, to = to, agent = agent, onProceed = onProceed),
    )

    data class TitleModel(
        val locale: Locale,
        val amountType: SwapAmountType,
        val amount: String,
        val expectedTitle: String,
    )

    data class RoundingBoundary(
        val displayDecimals: Int,
        val amount: String,
        val expectedFrom: String,
        val expectedTo: String,
    )

    data class RoundingModel(
        val locale: Locale,
        val amountType: SwapAmountType,
        val amount: String,
        val displayDecimals: Int,
        val expectedAmount: String,
    )

    data class AgentModel(
        val agent: McpAgent,
        val expectedIdentity: String,
        val amountType: SwapAmountType = SwapAmountType.From,
    )

    data class AmountModel(val amountType: SwapAmountType, val amount: String)
}