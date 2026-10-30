package com.tangem.features.send.mcp.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Builds the offer an agent's send intent is shown as: the currency the wallet sends, what the agent asks for and who
 * asks. The headline prefers the verified origin over the self-declared name.
 * Returns `null` when the intent amount is not a positive number.
 */
internal class SendIntentOfferConverter @Inject constructor() :
    Converter<SendIntentOfferConverter.Input, SendIntentUM?> {

    data class Input(
        val intent: SendIntent,
        val currency: CryptoCurrency,
        val agent: McpAgent,
        val onProceed: () -> Unit,
    )

    override fun convert(value: Input): SendIntentUM? {
        val amount = value.intent.amount.takeIf { it.signum() > 0 } ?: return null
        val agentIdentity = value.agent.origin ?: value.agent.name ?: Texts.UNKNOWN_AGENT_NAME

        return SendIntentUM(
            token = value.currency.toTokenIcon(),
            title = Texts.offerTitle(agentIdentity = agentIdentity, amount = amount.toOfferAmount(value.currency)),
            agent = value.agent.toAgentUM(),
            description = Texts.OFFER_DESCRIPTION,
            primaryButton = SendIntentButtonUM(text = Texts.PROCEED, onClick = value.onProceed),
        )
    }

    private fun McpAgent.toAgentUM(): SendIntentAgentUM = origin
        ?.let { SendIntentAgentUM.Verified(iconUrl = iconUrl, label = stringReference(it)) }
        ?: SendIntentAgentUM.Unknown(iconUrl = iconUrl, label = Texts.UNKNOWN_AGENT)

    private fun CryptoCurrency.toTokenIcon() = TangemTokenIcon.UiState.Token(
        TangemTokenIcon.State(
            url = iconUrl,
            topIcon = networkIconResId.takeIf { this is CryptoCurrency.Token },
        ),
    )

    /** Rounds up, as the amount is spent; sub-display-unit values show a less-than bound. */
    private fun BigDecimal.toOfferAmount(currency: CryptoCurrency): String {
        val maxDecimals = currency.displayDecimals.coerceAtMost(MAX_DECIMALS)

        return format {
            crypto(currency, ignoreSymbolPosition = true).anyDecimals(
                maxDecimals = maxDecimals,
                minDecimals = maxDecimals.coerceAtMost(MIN_DECIMALS),
                roundingMode = RoundingMode.UP,
                canBeLower = true,
            )
        }
    }

    private companion object {
        const val MAX_DECIMALS = 8
        const val MIN_DECIMALS = 2
    }

    // TODO: [REDACTED_TASK_KEY] Replace English placeholders with Lokalise-backed resources.
    private object Texts {
        const val UNKNOWN_AGENT_NAME = "Unknown"

        val UNKNOWN_AGENT = stringReference(UNKNOWN_AGENT_NAME)
        val OFFER_DESCRIPTION = stringReference(
            "The agent is asking to send from your account. You’ll review the details next.",
        )
        val PROCEED = stringReference("Proceed")

        fun offerTitle(agentIdentity: String, amount: String): TextReference =
            stringReference("$agentIdentity Agent wants to send $amount")
    }
}