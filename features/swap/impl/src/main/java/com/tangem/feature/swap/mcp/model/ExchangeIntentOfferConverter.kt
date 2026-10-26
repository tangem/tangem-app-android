package com.tangem.feature.swap.mcp.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.feature.swap.presentation.R
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
        val agentIdentity = (value.agent.origin ?: value.agent.name)?.let(::stringReference)
            ?: resourceReference(R.string.mcp_intent_unknown_agent_name)

        return ExchangeIntentUM.Offer(
            fromToken = value.from.toTokenIcon(),
            toToken = value.to.toTokenIcon(),
            title = resourceReference(
                id = R.string.mcp_swap_intent_offer_title,
                formatArgs = wrappedList(agentIdentity, swap.from, swap.to),
            ),
            agent = value.agent.toAgentUM(),
            description = resourceReference(R.string.mcp_swap_intent_offer_description),
            primaryButton = ExchangeIntentButtonUM(
                text = resourceReference(R.string.mcp_intent_proceed),
                onClick = value.onProceed,
            ),
        )
    }

    private fun McpAgent.toAgentUM(): ExchangeIntentAgentUM = origin
        ?.let { ExchangeIntentAgentUM.Verified(iconUrl = iconUrl, label = stringReference(it)) }
        ?: ExchangeIntentAgentUM.Unknown(iconUrl = iconUrl, label = resourceReference(R.string.mcp_intent_host))

    private fun CryptoCurrency.toTokenIcon() = TangemTokenIcon.UiState.Token(
        TangemTokenIcon.State(
            url = iconUrl,
            topIcon = networkIconResId.takeIf { this is CryptoCurrency.Token },
        ),
    )
}