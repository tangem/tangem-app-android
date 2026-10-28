package com.tangem.data.qrscanning.repository

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.data.qrscanning.parser.QrSentUriParser
import com.tangem.data.qrscanning.parser.QrContentClassifierParser
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.ClassifiedQrContent
import com.tangem.domain.qrscanning.models.QrResult
import com.tangem.domain.qrscanning.models.RawQrResult
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.yield
import java.math.BigDecimal

internal class DefaultQrScanningEventsRepository(
    private val qrContentClassifierParser: QrContentClassifierParser,
    private val paymentUriParser: QrSentUriParser = QrSentUriParser(),
) : QrScanningEventsRepository {

    private data class QrScanningEvent(val qrCode: RawQrResult)

    private val scannedEvents = MutableSharedFlow<QrScanningEvent>(replay = 1)

    override suspend fun emitResult(qrCode: RawQrResult) {
        scannedEvents.emit(QrScanningEvent(qrCode))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribeToScanningResults(type: SourceType) = scannedEvents
        .filter { it.qrCode.requestSource == type }
        .map { it.qrCode }
        .onEach {
            yield() // if we have more than one sub, we must allow them to collect emitted value
            scannedEvents.resetReplayCache()
        }

    override fun parseQrCode(qrCode: String, cryptoCurrency: CryptoCurrency): QrResult {
        val withoutSchema = stripSchema(qrCode, cryptoCurrency)
        val parsed = paymentUriParser.parse(withoutSchema)
            ?: return QrResult(address = withoutSchema)

        val result = QrResult(address = parsed.address)
        result.amount = parsed.amount
        result.memo = parsed.memo?.second

        // ERC-681: if 'address' parameter exists, currency must be a token,
        // and the URI address must match the token's contract address.
        parsed.remainingParams[QrSentUriParser.PARAM_ADDRESS]?.let { addressValue ->
            val tokenCurrency = cryptoCurrency as? CryptoCurrency.Token ?: return QrResult()
            if (tokenCurrency.contractAddress.equals(parsed.address, ignoreCase = true)) {
                result.address = addressValue
            } else {
                return QrResult()
            }
        }

        // ERC-681: value/uint256 is in the smallest unit, needs conversion
        val valueStr = parsed.remainingParams[QrSentUriParser.PARAM_VALUE]
            ?: parsed.remainingParams[QrSentUriParser.PARAM_UINT256]
        if (valueStr != null) {
            result.amount = valueStr.parseBigDecimalOrNull()
                ?.toPlainString()?.toBigDecimalOrNull()
                ?.divide(BigDecimal.TEN.pow(cryptoCurrency.decimals))
        }

        return result
    }

    override fun classify(qrCode: String, userCurrencies: List<CryptoCurrency>): ClassifiedQrContent {
        return qrContentClassifierParser.parse(qrCode, userCurrencies)
    }

    override fun extractRecipientAddress(qrCode: String): String {
        val trimmed = qrCode.trim()
        val scheme = trimmed.substringBefore(QrSentUriParser.PARAM_DELIMITER)
            .substringBefore(SCHEME_DELIMITER, missingDelimiterValue = "")
            .takeIf(URI_SCHEME_REGEX::matches)
        val withoutScheme = if (scheme != null) trimmed.substringAfter(SCHEME_DELIMITER) else trimmed
        val isEthereumScheme = scheme.equals(EIP681_SCHEME, ignoreCase = true)
        val target = withoutScheme.removePrefix(EIP681_PAY_PREFIX)

        return if (isEthereumScheme || target.startsWith(EVM_ADDRESS_PREFIX, ignoreCase = true)) {
            extractEip681Recipient(target, isEthereumScheme)
        } else {
            extractPlainRecipient(withoutScheme)
        }
    }

    private fun extractEip681Recipient(target: String, isEthereumScheme: Boolean): String {
        val parsed = paymentUriParser.parse(target) ?: return ""
        val recipientParam = parsed.remainingParams[QrSentUriParser.PARAM_ADDRESS]

        return when (parsed.functionName) {
            null -> if (isEthereumScheme) recipientParam ?: parsed.address else parsed.address
            QrSentUriParser.FUNCTION_TRANSFER -> recipientParam.orEmpty()
            else -> ""
        }
    }

    /**
     * Non-EVM addresses may contain '/' (Base64 TON), so only the query and the chain id are cut off.
     * For hierarchical URIs such as `ton://transfer/<address>` the address is the path after the authority.
     */
    private fun extractPlainRecipient(withoutScheme: String): String {
        val withoutQueryAndChain = withoutScheme
            .substringBefore(QrSentUriParser.PARAM_DELIMITER)
            .substringBefore(QrSentUriParser.CHAIN_DELIMITER)
        if (!withoutQueryAndChain.startsWith(AUTHORITY_PREFIX)) return withoutQueryAndChain

        val afterAuthorityPrefix = withoutQueryAndChain.removePrefix(AUTHORITY_PREFIX)
        return afterAuthorityPrefix.substringAfter(QrSentUriParser.FUNCTION_DELIMITER, afterAuthorityPrefix)
    }

    private fun stripSchema(raw: String, currency: CryptoCurrency): String {
        val qrSchemas = currency.network.toBlockchain().getShareScheme()

        qrSchemas
            .sortedByDescending { it.length }
            .forEach { schema ->
                val stripped = raw.split(schema)
                if (stripped.size > 1) return stripped.last()
            }

        return raw
    }

    private companion object {
        const val SCHEME_DELIMITER = ':'
        const val EIP681_SCHEME = "ethereum"
        const val EIP681_PAY_PREFIX = "pay-"
        const val EVM_ADDRESS_PREFIX = "0x"
        const val AUTHORITY_PREFIX = "//"
        val URI_SCHEME_REGEX = Regex("[A-Za-z][A-Za-z0-9+.-]*")
    }
}