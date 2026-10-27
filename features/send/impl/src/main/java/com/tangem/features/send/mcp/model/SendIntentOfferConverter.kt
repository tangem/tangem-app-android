package com.tangem.features.send.mcp.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.features.send.impl.R
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
        val agentIdentity = (value.agent.origin ?: value.agent.name)?.let(::stringReference)
            ?: resourceReference(R.string.mcp_intent_unknown_agent_name)

        return SendIntentUM.Offer(
            token = value.currency.toTokenIcon(),
            title = resourceReference(
                id = R.string.mcp_send_intent_offer_title,
                formatArgs = wrappedList(agentIdentity, amountText),
            ),
            agent = value.agent.toAgentUM(),
            description = resourceReference(R.string.mcp_send_intent_offer_description),
            primaryButton = SendIntentButtonUM(
                text = resourceReference(R.string.mcp_intent_proceed),
                onClick = value.onProceed,
            ),
        )
    }

    private fun McpAgent.toAgentUM(): SendIntentAgentUM = origin
        ?.let { SendIntentAgentUM.Verified(iconUrl = iconUrl, label = stringReference(it)) }
        ?: SendIntentAgentUM.Unknown(iconUrl = iconUrl, label = resourceReference(R.string.mcp_intent_host))

    private fun CryptoCurrency.toTokenIcon() = TangemTokenIcon.UiState.Token(
        TangemTokenIcon.State(
            url = iconUrl,
            topIcon = networkIconResId.takeIf { this is CryptoCurrency.Token },
        ),
    )
}