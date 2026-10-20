package com.tangem.feature.swap.mcp.model

import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.feature.swap.mcp.ExchangeIntentAmountConverter
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Builds the offer an agent's exchange intent is shown as: the two currencies the wallet swaps between, what the agent
 * asks for and who asks. The headline prefers the verified origin over the self-declared name.
 * Returns `null` when the intent amount is not a positive number.
 */
internal class ExchangeIntentOfferConverter @Inject constructor(
    private val amountConverter: ExchangeIntentAmountConverter,
) : Converter<ExchangeIntentOfferConverter.Input, ExchangeIntentUM?> {

    data class Input(
        val intent: ExchangeIntent,
        val from: CryptoCurrency,
        val to: CryptoCurrency,
        val agent: McpAgent,
        val onProceed: () -> Unit,
    )

    override fun convert(value: Input): ExchangeIntentUM? {
        val swap = value.toSwapText() ?: return null
        val agentIdentity = value.agent.origin ?: value.agent.name ?: Texts.UNKNOWN_AGENT_NAME

        return ExchangeIntentUM(
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

    /** The fixed side rounds spend up and receive down; sub-display-unit values show a less-than bound. */
    private fun Input.toSwapText(): SwapText? {
        val amount = amountConverter.convert(intent)?.takeIf { it.signum() > 0 } ?: return null
        val fixedCurrency = if (intent.amountType == SwapAmountType.From) from else to
        val roundingMode = when (intent.amountType) {
            SwapAmountType.From -> RoundingMode.UP
            SwapAmountType.To -> RoundingMode.DOWN
        }
        val fixedAmount = amount.toOfferAmount(currency = fixedCurrency, roundingMode = roundingMode)

        return when (intent.amountType) {
            SwapAmountType.From -> SwapText(from = fixedAmount, to = to.symbol)
            SwapAmountType.To -> SwapText(from = from.symbol, to = fixedAmount)
        }
    }

    private fun BigDecimal.toOfferAmount(currency: CryptoCurrency, roundingMode: RoundingMode): String {
        val maxDecimals = currency.displayDecimals.coerceAtMost(MAX_DECIMALS)

        return format {
            crypto(currency, ignoreSymbolPosition = true).anyDecimals(
                maxDecimals = maxDecimals,
                minDecimals = maxDecimals.coerceAtMost(MIN_DECIMALS),
                roundingMode = roundingMode,
                canBeLower = true,
            )
        }
    }

    private data class SwapText(val from: String, val to: String)

    private companion object {
        const val MAX_DECIMALS = 8
        const val MIN_DECIMALS = 2
    }

    // TODO: [REDACTED_TASK_KEY] Replace English placeholders with Lokalise-backed resources.
    private object Texts {
        const val UNKNOWN_AGENT_NAME = "Unknown"

        val UNKNOWN_AGENT = stringReference(UNKNOWN_AGENT_NAME)
        val OFFER_DESCRIPTION = stringReference(
            "The agent is asking to swap from your account. You’ll review the details next.",
        )
        val PROCEED = stringReference("Proceed")

        fun offerTitle(agentIdentity: String, swap: SwapText): TextReference =
            stringReference("$agentIdentity Agent wants to swap ${swap.from} to ${swap.to}")
    }
}