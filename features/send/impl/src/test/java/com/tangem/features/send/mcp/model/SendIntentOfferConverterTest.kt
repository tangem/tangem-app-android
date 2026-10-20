package com.tangem.features.send.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.features.send.mcp.createCoin
import com.tangem.features.send.mcp.createSendIntent
import com.tangem.features.send.mcp.createToken
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
internal class SendIntentOfferConverterTest {

    private val converter = SendIntentOfferConverter()

    private val usdt = createToken()
    private val eth = createCoin()
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

    @Test
    fun `GIVEN token intent WHEN convert THEN offer shows token amount with network badge`() {
        // Arrange
        val onProceed: () -> Unit = {}
        val expected = SendIntentUM.Offer(
            token = TangemTokenIcon.UiState.Token(
                TangemTokenIcon.State(url = usdt.iconUrl, topIcon = usdt.networkIconResId),
            ),
            title = stringReference("mcp.claude.com Agent wants to send 42.00 USDT"),
            agent = SendIntentAgentUM.Verified(iconUrl = claude.iconUrl, label = stringReference("mcp.claude.com")),
            description = stringReference(DEFAULT_DESCRIPTION),
            primaryButton = SendIntentButtonUM(text = stringReference("Proceed"), onClick = onProceed),
        )

        // Act
        val actual = convert(onProceed = onProceed)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN coin intent WHEN convert THEN offer shows coin amount without network badge`() {
        // Arrange
        val onProceed: () -> Unit = {}
        val expected = SendIntentUM.Offer(
            token = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = eth.iconUrl, topIcon = null)),
            title = stringReference("mcp.claude.com Agent wants to send 1.50 ETH"),
            agent = SendIntentAgentUM.Verified(iconUrl = claude.iconUrl, label = stringReference("mcp.claude.com")),
            description = stringReference(DEFAULT_DESCRIPTION),
            primaryButton = SendIntentButtonUM(text = stringReference("Proceed"), onClick = onProceed),
        )

        // Act
        val actual = convert(
            intent = createSendIntent(amount = BigDecimal("1.5")),
            currency = eth,
            onProceed = onProceed,
        )

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AgentIdentity {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN agent metadata WHEN convert THEN title prefers origin over name and card keeps verified identity`(
            model: AgentModel,
        ) {
            // Arrange
            val expectedTitle = stringReference("${model.expectedIdentity} Agent wants to send 42.00 USDT")

            // Act
            val actual = convert(agent = model.agent)

            // Assert
            assertThat(actual?.title).isEqualTo(expectedTitle)
            assertThat(actual?.agent).isEqualTo(model.expectedAgent)
        }

        private fun provideTestModels() = listOf(
            AgentModel(
                agent = claude,
                expectedIdentity = "mcp.claude.com",
                expectedAgent = SendIntentAgentUM.Verified(claude.iconUrl, stringReference("mcp.claude.com")),
            ),
            AgentModel(
                agent = claude.copy(name = null, iconUrl = null),
                expectedIdentity = "mcp.claude.com",
                expectedAgent = SendIntentAgentUM.Verified(null, stringReference("mcp.claude.com")),
            ),
            AgentModel(
                agent = claude.copy(origin = null),
                expectedIdentity = "Claude",
                expectedAgent = SendIntentAgentUM.Unknown(claude.iconUrl, stringReference("Unknown")),
            ),
            AgentModel(
                agent = claude.copy(origin = null, name = null),
                expectedIdentity = "Unknown",
                expectedAgent = SendIntentAgentUM.Unknown(claude.iconUrl, stringReference("Unknown")),
            ),
            AgentModel(
                agent = McpAgent.unknown(clientId = "client"),
                expectedIdentity = "Unknown",
                expectedAgent = SendIntentAgentUM.Unknown(
                    iconUrl = McpAgent.unknown(clientId = "client").iconUrl,
                    label = stringReference("Unknown"),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN intent with agent comment WHEN convert THEN description stays the default text`() {
        // Act
        val actual = convert(intent = createSendIntent(comment = "Paying for lunch"))

        // Assert
        assertThat(actual?.description).isEqualTo(stringReference(DEFAULT_DESCRIPTION))
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AmountFormatting {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN excess precision or dust amount WHEN convert THEN title rounds up or shows lower bound`(
            model: AmountFormatModel,
        ) {
            // Arrange
            Locale.setDefault(model.locale)
            val intent = createSendIntent(amount = BigDecimal(model.amount))

            // Act
            val actual = convert(intent = intent, currency = model.currency)

            // Assert
            assertThat(actual?.title).isEqualTo(stringReference("mcp.claude.com Agent wants to send ${model.expected}"))
        }

        private fun provideTestModels() = listOf(
            AmountFormatModel(Locale.US, usdt, "1.0000001", "1.000001 USDT"),
            AmountFormatModel(Locale.US, usdt, "0.0000001", "<0.000001 USDT"),
            AmountFormatModel(Locale.US, eth, "0.000000011", "0.00000002 ETH"),
            AmountFormatModel(Locale.US, eth, "0.00000001", "0.00000001 ETH"),
            AmountFormatModel(Locale.US, eth, "0.000000001", "<0.00000001 ETH"),
            AmountFormatModel(Locale.GERMANY, eth, "0.000000011", "0,00000002 ETH"),
            AmountFormatModel(Locale.GERMANY, usdt, "42", "42,00 USDT"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NonPositiveAmounts {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN non-positive amount WHEN convert THEN there is no offer`(amount: BigDecimal) {
            // Act
            val actual = convert(intent = createSendIntent(amount = amount))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf("0", "0.000", "-0", "-5").map(::BigDecimal)
    }

    @Test
    fun `GIVEN offer WHEN its button is clicked THEN proceed is requested`() {
        // Arrange
        var proceeded = false
        val offer = requireNotNull(convert(onProceed = { proceeded = true }))

        // Act
        offer.primaryButton.onClick()

        // Assert
        assertThat(proceeded).isTrue()
    }

    private fun convert(
        intent: SendIntent = createSendIntent(),
        currency: CryptoCurrency = usdt,
        agent: McpAgent = claude,
        onProceed: () -> Unit = {},
    ): SendIntentUM.Offer? = converter.convert(
        SendIntentOfferConverter.Input(intent = intent, currency = currency, agent = agent, onProceed = onProceed),
    )

    data class AgentModel(
        val agent: McpAgent,
        val expectedIdentity: String,
        val expectedAgent: SendIntentAgentUM,
    )

    data class AmountFormatModel(
        val locale: Locale,
        val currency: CryptoCurrency,
        val amount: String,
        val expected: String,
    )

    private companion object {
        const val DEFAULT_DESCRIPTION =
            "The agent is asking to send from your account. You’ll review the details next."
    }
}