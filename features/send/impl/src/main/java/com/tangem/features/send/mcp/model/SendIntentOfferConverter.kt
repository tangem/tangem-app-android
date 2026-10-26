package com.tangem.features.send.mcp.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 * Builds the offer an agent's send intent is shown as: the currency the wallet sends, what the agent asks for and who
 * asks. The headline prefers the verified origin over the self-declared name.
 * Returns `null` when the intent amount is not a positive number.
 */
internal class SendIntentOfferConverter @Inject constructor(
    private val amountTextConverter: SendIntentAmountTextConverter,
) : Converter<SendIntentOfferConverter.Input, SendIntentUM.Offer?> {

    data class Input(
        val intent: SendIntent,
        val currency: CryptoCurrency,
        val agent: McpAgent,
        val onProceed: () -> Unit,
    )

    override fun convert(value: Input): SendIntentUM.Offer? {
        val amountText = amountTextConverter.convert(
            SendIntentAmountTextConverter.Input(amount = value.intent.amount, currency = value.currency),
        ) ?: return null
        val agentIdentity = value.agent.origin ?: value.agent.name ?: Texts.UNKNOWN_AGENT_NAME

        return SendIntentUM.Offer(
            token = value.currency.toTokenIcon(),
            title = Texts.offerTitle(agentIdentity = agentIdentity, amount = amountText),
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