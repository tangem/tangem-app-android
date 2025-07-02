package com.tangem.data.qrscanning.repository

import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.qrscanning.models.QrResult
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.repository.QrScanningEventsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.yield
import java.math.BigDecimal
import java.net.URLDecoder

internal class DefaultQrScanningEventsRepository : QrScanningEventsRepository {

    private data class QrScanningEvent(val type: SourceType, val qrCode: String)

    private val scannedEvents = MutableSharedFlow<QrScanningEvent>(replay = 1)

    override suspend fun emitResult(type: SourceType, qrCode: String) {
        scannedEvents.emit(QrScanningEvent(type, qrCode))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun subscribeToScanningResults(type: SourceType) = scannedEvents
        .filter { it.type == type }
        .map { it.qrCode }
        .onEach {
            yield() // if we have more than one sub, we must allow them to collect emitted value
            scannedEvents.resetReplayCache()
        }

    override fun parseQrCode(qrCode: String, cryptoCurrency: CryptoCurrency): QrResult {
        val withoutSchema = stripSchema(qrCode, cryptoCurrency)

        // A poor man's ERC-681 parser: we want to extract only the destination address, and we don't care
        // about other parts of the ERC-681 payload string like `chain_id` and/or `function_name`.
        //
        // We're extracting the destination address by parsing the given string until we meet
        // any of the possible string delimiters (@ ? /).
        val address = withoutSchema.takeWhile { char ->
            char != CHAIN_DELIMITER && char != FUNCTION_DELIMITER && char != PARAM_DELIMITER
        }

        val result = QrResult(address = address)

        extractParameters(withoutSchema)
            .forEach {
                when (it.key) {
                    Parameter.Amount -> {
                        // According to BIP-0021, the value is specified in decimals. No conversion needed
                        result.amount = it.value.parseBigDecimalOrNull()
                    }
                    Parameter.Message,
                    Parameter.Memo,
                    -> {
                        result.memo = URLDecoder.decode(it.value, "UTF-8")
                    }
                    Parameter.Address -> {
                        // If 'address' parameter is exists, then currency must be TOKEN.
                        val tokenCurrency = cryptoCurrency as? CryptoCurrency.Token ?: return QrResult()

                        // Overrides destination address for token transfers (ERC-681)
                        // `address` parameter is used only if the contract address, encoded in the QR,
                        // matches the contract address of the token.
                        // Otherwise, the scanned string is likely malformed, and we stop the entire parsing routin
                        if (tokenCurrency.contractAddress.equals(address, ignoreCase = true)) {
                            result.address = it.value
                        } else {
                            return QrResult()
                        }
                    }
                    Parameter.Value,
                    Parameter.Uint256,
                    -> {
                        // Extra convert parses scientific notation to decimal
                        // This is necessary to be able comparing BigDecimal values
                        result.amount = it.value.parseBigDecimalOrNull()
                            ?.toPlainString()?.toBigDecimalOrNull()
                            ?.divide(BigDecimal.TEN.pow(cryptoCurrency.decimals))
                    }
                }
            }

        return result
    }

    private fun stripSchema(raw: String, currency: CryptoCurrency): String {
        val qrSchemas = currency.network.toBlockchain().getShareScheme()

        // The most specific (i.e. the most lengthy) prefixes always come first
        qrSchemas
            .sortedByDescending { it.length }
            .forEach { schema ->
                val stripped = raw.split(schema)

                if (stripped.size > 1) return stripped.last()
            }

        return raw
    }

    private fun extractParameters(from: String): Map<Parameter, String> {
        val parametersBlock = from.substringAfter(PARAM_DELIMITER)
        if (parametersBlock.isBlank()) return emptyMap()

        val paramList = parametersBlock.split(PARAMS_DELIMITER)
            .mapNotNull { param ->
                val parameterWithValue = param.split(PARAM_VALUE_DELIMITER)
                if (parameterWithValue.size == 2) {
                    val name = Parameter.getParam(parameterWithValue.first())
                    val value = parameterWithValue.last()

                    if (name != null) {
                        name to value
                    } else {
                        null
                    }
                } else {
                    null
                }
            }.associate { it }

        return paramList
    }

    private enum class Parameter {
        Amount,
        Message,
        Memo,
        Address,
        Value,
        Uint256,
        ;

        companion object {
            fun getParam(name: String): Parameter? {
                return Parameter.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            }
        }
    }

    private companion object {
        // See https://eips.ethereum.org/EIPS/eip-681 for details.
        const val CHAIN_DELIMITER = '@' // ERC-681 [ "@" chain_id ]
        const val FUNCTION_DELIMITER = '/' // ERC-681 [ "/" function_name ]
        const val PARAM_DELIMITER = '?' // BIP-021, ERC-681
        const val PARAMS_DELIMITER = '&'
        const val PARAM_VALUE_DELIMITER = '='
    }
}