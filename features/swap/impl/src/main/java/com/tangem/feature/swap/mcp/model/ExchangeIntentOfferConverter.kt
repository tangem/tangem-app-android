package com.tangem.feature.swap.mcp.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.utils.converter.Converter
import javax.inject.Inject

/**
 * Builds the offer an agent's exchange intent is shown as: the two currencies the wallet swaps between, what the agent
 * asks for and who asks. The headline prefers the verified origin over the self-declared name.
 * Returns `null` when the intent amount is not a positive number.
 */
internal class ExchangeIntentOfferConverter @Inject constructor(
    private val swapTextConverter: ExchangeIntentSwapTextConverter,
) : Converter<ExchangeIntentOfferConverter.Input, ExchangeIntentUM.Offer?> {

    data class Input(
        val intent: ExchangeIntent,
        val from: CryptoCurrency,
        val to: CryptoCurrency,
        val agent: McpAgent,
        val onProceed: () -> Unit,
    )

    override fun convert(value: Input): ExchangeIntentUM.Offer? {
        val swap = swapTextConverter.convert(
            ExchangeIntentSwapTextConverter.Input(intent = value.intent, from = value.from, to = value.to),
        ) ?: return null
        val agentIdentity = value.agent.origin ?: value.agent.name ?: Texts.UNKNOWN_AGENT_NAME

        return ExchangeIntentUM.Offer(
            fromToken = value.from.toTokenIcon(),
            toToken = value.to.toTokenIcon(),
            title = Texts.offerTitle(agentIdentity = agentIdentity, swap = swap),
            agent = value.agent.toAgentUM(),
            description = Texts.OFFER_DESCRIPTION,
            primaryButton = ExchangeIntentButtonUM(text = Texts.PROCEED, onClick = value.onProceed),
        )
    }

    private fun McpAgent.toAgentUM(): ExchangeIntentAgentUM = origin
        ?.let { ExchangeIntentAgentUM.Verified(iconUrl = iconUrl, label = stringReference(it)) }
        ?: ExchangeIntentAgentUM.Unknown(iconUrl = iconUrl, label = Texts.UNKNOWN_AGENT)

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
            "The agent is asking to swap from your account. You’ll review the details next.",
        )
        val PROCEED = stringReference("Proceed")

        fun offerTitle(agentIdentity: String, swap: ExchangeIntentSwapTextConverter.SwapText): TextReference =
            stringReference("$agentIdentity Agent wants to swap ${swap.from} to ${swap.to}")
    }
}