package com.tangem.features.txhistory.converter

import androidx.annotation.StringRes
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.converter.Converter

/**
 * Builds the plain-text summary of an express deal shared by the details card's "Share" action. Only an express row
 * (swap / onramp) carries enough data to describe itself as text, so an on-chain row offers "Explore" instead and
 * never reaches this converter.
 *
 * The layout is the one of the legacy express-status sheet (`makeExpressShareContent`) so a shared deal reads the same
 * before and after the history redesign:
 *
 * ```
 * Tangem
 *
 * Send 0.5 ETH
 * From: 0xAA5C...
 *
 * Receive 1 000.00 USDT
 * To: 0xBB6D...
 *
 * Provided by Changelly CEX
 * ID: 8edc9b5f-...
 * ```
 *
 * A line whose data is absent is dropped rather than left blank — an onramp has no pay-in address (the fiat was paid by
 * card), an unresolved provider has no name to print, and not every deal carries an id.
 */
internal class ExpressTxToShareTextConverter : Converter<ExpressTx, TextReference> {

    override fun convert(value: ExpressTx): TextReference = when (value) {
        is ExpressTx.Swap -> buildShareText(
            sentAmount = value.tx.fromAsset.formatAmount(),
            sentFromAddress = value.tx.fromAddress,
            receivedAmount = value.tx.toAsset.formatAmount(),
            receivedToAddress = value.tx.payoutAddress,
            provider = value.provider,
            // The provider-side id is the one their support desk can look up; the deal id stands in when absent.
            id = value.tx.externalTxId ?: value.txId,
            onChainHash = value.onChainHashOrNull(),
        )
        is ExpressTx.Onramp -> buildShareText(
            sentAmount = value.tx.fromFiat.formatFiatAmount(),
            sentFromAddress = null,
            receivedAmount = value.tx.toAsset.formatAmount(),
            receivedToAddress = value.tx.payoutAddress,
            provider = value.provider,
            id = value.tx.externalTxId ?: value.txId,
            onChainHash = value.onChainHashOrNull(),
        )
    }

    /**
     * The matched on-chain leg's hash, kept alongside the express id once a deal is merged — the express id
     * alone is not enough for a merged deal, support needs to find the on-chain leg too, and the leg's own hash
     * is what [Explore] already links to.
     */
    private fun ExpressTx.onChainHashOrNull(): String? = txInfo?.explorerHash

    @Suppress("LongParameterList")
    private fun buildShareText(
        sentAmount: String,
        sentFromAddress: String?,
        receivedAmount: String,
        receivedToAddress: String,
        provider: ExpressProvider?,
        id: String,
        onChainHash: String?,
    ): TextReference = joinLines(
        resourceReference(R.string.common_tangem),
        BLANK_LINE,
        labeledLine(R.string.common_send, " $sentAmount"),
        sentFromAddress?.let { labeledLine(R.string.common_from, ": $it") },
        BLANK_LINE,
        labeledLine(R.string.common_receive, " $receivedAmount"),
        labeledLine(R.string.common_to, ": $receivedToAddress"),
        BLANK_LINE,
        provider?.let(::providerLine),
        id.takeIf(String::isNotBlank)?.let { resourceReference(R.string.express_transaction_id, wrappedList(it)) },
        onChainHash?.takeIf(String::isNotBlank)?.let {
            resourceReference(R.string.express_transaction_hash, wrappedList(it))
        },
    )

    /** A localized label followed by its raw [suffix]: `Send 0.5 ETH`, `From: 0xAA5C...`. */
    private fun labeledLine(@StringRes labelResId: Int, suffix: String): TextReference =
        resourceReference(labelResId) + stringReference(suffix)

    /**
     * `Provided by Changelly CEX`. The type is the raw enum name (`CEX` / `DEX` / `DEX_BRIDGE` / `ONRAMP`) — the shape
     * the legacy sheet shared and the one the deal is stored under, not the prettified `typeName` of the provider row.
     */
    private fun providerLine(provider: ExpressProvider): TextReference =
        resourceReference(R.string.express_by_provider) + stringReference(" ${provider.name} ${provider.type.name}")

    /** Joins the present [lines] with a line break between them; an absent line leaves no gap behind. */
    private fun joinLines(vararg lines: TextReference?): TextReference =
        lines.filterNotNull().reduce { text, line -> text + LINE_BREAK + line }

    private companion object {
        private val LINE_BREAK = stringReference("\n")
        private val BLANK_LINE = stringReference("")
    }
}