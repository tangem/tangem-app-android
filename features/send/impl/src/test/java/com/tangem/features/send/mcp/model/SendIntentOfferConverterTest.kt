package com.tangem.features.send.mcp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.features.send.impl.R
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

    private val converter = SendIntentOfferConverter(SendIntentAmountTextConverter())

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
            title = resourceReference(
                R.string.mcp_send_intent_offer_title,
                wrappedList(stringReference("mcp.claude.com"), "42.00\u00a0USDT"),
            ),
            agent = SendIntentAgentUM.Verified(iconUrl = claude.iconUrl, label = stringReference("mcp.claude.com")),
            description = resourceReference(R.string.mcp_send_intent_offer_description),
            primaryButton = SendIntentButtonUM(text = resourceReference(R.string.mcp_intent_proceed), onClick = onProceed),
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
            title = resourceReference(
                R.string.mcp_send_intent_offer_title,
                wrappedList(stringReference("mcp.claude.com"), "1.50\u00a0ETH"),
            ),
            agent = SendIntentAgentUM.Verified(iconUrl = claude.iconUrl, label = stringReference("mcp.claude.com")),
            description = resourceReference(R.string.mcp_send_intent_offer_description),
            primaryButton = SendIntentButtonUM(text = resourceReference(R.string.mcp_intent_proceed), onClick = onProceed),
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
            val expectedTitle = resourceReference(
                R.string.mcp_send_intent_offer_title,
                wrappedList(model.expectedIdentity, "42.00\u00a0USDT"),
            )

            // Act
            val actual = convert(agent = model.agent)

            // Assert
            assertThat(actual?.title).isEqualTo(expectedTitle)
            assertThat(actual?.agent).isEqualTo(model.expectedAgent)
        }

        private fun provideTestModels() = listOf(
            AgentModel(
                agent = claude,
                expectedIdentity = stringReference("mcp.claude.com"),
                expectedAgent = SendIntentAgentUM.Verified(claude.iconUrl, stringReference("mcp.claude.com")),
            ),
            AgentModel(
                agent = claude.copy(name = null, iconUrl = null),
                expectedIdentity = stringReference("mcp.claude.com"),
                expectedAgent = SendIntentAgentUM.Verified(null, stringReference("mcp.claude.com")),
            ),
            AgentModel(
                agent = claude.copy(origin = null),
                expectedIdentity = stringReference("Claude"),
                expectedAgent = SendIntentAgentUM.Unknown(claude.iconUrl, resourceReference(R.string.mcp_intent_host)),
            ),
            AgentModel(
                agent = claude.copy(origin = null, name = null),
                expectedIdentity = resourceReference(R.string.mcp_intent_unknown_agent_name),
                expectedAgent = SendIntentAgentUM.Unknown(claude.iconUrl, resourceReference(R.string.mcp_intent_host)),
            ),
            AgentModel(
                agent = McpAgent.unknown(clientId = "client"),
                expectedIdentity = resourceReference(R.string.mcp_intent_unknown_agent_name),
                expectedAgent = SendIntentAgentUM.Unknown(
                    iconUrl = McpAgent.unknown(clientId = "client").iconUrl,
                    label = resourceReference(R.string.mcp_intent_host),
                ),
            ),
            AgentModel(
                agent = claude.copy(origin = null, name = "AI"),
                expectedIdentity = stringReference("AI"),
                expectedAgent = SendIntentAgentUM.Unknown(claude.iconUrl, resourceReference(R.string.mcp_intent_host)),
            ),
        )
    }

    @Test
    fun `GIVEN intent with agent comment WHEN convert THEN description stays the default text`() {
        // Act
        val actual = convert(intent = createSendIntent(comment = "Paying for lunch"))

        // Assert
        assertThat(actual?.description).isEqualTo(resourceReference(R.string.mcp_send_intent_offer_description))
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
        val expectedIdentity: TextReference,
        val expectedAgent: SendIntentAgentUM,
    )
}