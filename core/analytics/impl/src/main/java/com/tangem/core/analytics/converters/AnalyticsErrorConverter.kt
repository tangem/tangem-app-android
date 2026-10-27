package com.tangem.core.analytics.converters

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.utils.converter.Converter

/**
[REDACTED_AUTHOR]
 */
class AnalyticsErrorConverter : Converter<Throwable, Map<String, String>> {
    fun canBeHandled(error: Throwable): Boolean = error !is TangemSdkError.UserCancelled

    override fun convert(value: Throwable): Map<String, String> {
        return when (value) {
            is BlockchainSdkError -> BlockchainSdkErrorConverter(CardSdkErrorConverter()).convert(value)
            is TangemSdkError -> CardSdkErrorConverter().convert(value)
            else -> ThrowableErrorConverter().convert(value)
        }
    }
}

private class ThrowableErrorConverter : Converter<Throwable, Map<String, String>> {
    override fun convert(value: Throwable): Map<String, String> {
        val unknown = "unknown"
        return mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.App.value,
            AnalyticsParam.ERROR_DESCRIPTION to (value.message ?: value.cause?.message ?: unknown),
        )
    }
}

class CardSdkErrorConverter : Converter<TangemSdkError, Map<String, String>> {

    override fun convert(value: TangemSdkError): Map<String, String> {
        if (value is TangemSdkError.UserCancelled) return emptyMap()

        return mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.CardSdk.value,
            AnalyticsParam.ERROR_CODE to value.code.toString(),
            AnalyticsParam.ERROR_DESCRIPTION to value.toString(),
        )
    }
}

private class BlockchainSdkErrorConverter(
    private val cardSdkErrorConverter: CardSdkErrorConverter,
) : Converter<BlockchainSdkError, Map<String, String>> {

    override fun convert(value: BlockchainSdkError): Map<String, String> {
        if (value is BlockchainSdkError.WrappedTangemError) {
            return (value.tangemError as? TangemSdkError)?.let { cardSdkErrorConverter.convert(it) }.orEmpty()
        }

        return mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.BlockchainSdk.value,
            AnalyticsParam.ERROR_CODE to value.code.toString(),
            AnalyticsParam.ERROR_DESCRIPTION to "${value.javaClass.simpleName}: ${value.customMessage}",
        )
    }
}