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

        // ERC-681: `@<chainId>` names the network the payee expects. A URI for another EVM chain must not be
        // accepted as a payment on the currently selected one — the address would be valid on both, and the
        // funds would land on a chain the payee is not watching.
        parsed.chainId?.let { uriChainId ->
            val currencyChainId = runCatching { cryptoCurrency.network.toBlockchain().getChainId()?.toLong() }
                .getOrNull()
            if (currencyChainId != null && currencyChainId != uriChainId) return QrResult()
        }

        // ERC-681: only a plain `transfer` (or no function at all) is a payment request this screen can fulfil.
        // Any other function (approve, a contract call, …) must not be reinterpreted as "send to `address`".
        val functionName = parsed.functionName
        if (functionName != null && !functionName.equals(QrSentUriParser.FUNCTION_TRANSFER, ignoreCase = true)) {
            return QrResult()
        }

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
}