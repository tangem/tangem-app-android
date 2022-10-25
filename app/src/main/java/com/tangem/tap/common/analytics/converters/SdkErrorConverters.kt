package com.tangem.tap.common.analytics.converters

import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.Converter
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.AnalyticsAnOld
import com.tangem.tap.common.analytics.AnalyticsParamAnOld
import com.tangem.tap.common.analytics.TangemSdk
import com.tangem.tap.features.demo.DemoTransactionSender

/**
[REDACTED_AUTHOR]
 */
internal data class ErrorEvent(
    val throwable: Throwable,
    val params: Map<String, String>,
)

internal class CardSdkErrorConverter : Converter<CardSdkErrorConverter.Model, ErrorEvent?> {

    override fun convert(value: Model): ErrorEvent? {
        if (value.error is TangemSdkError.UserCancelled) return null

        val mutableParams = value.params.convertToParamValue().toMutableMap().apply {
            putAll(value.cardParams)
        }
        mutableParams[AnalyticsParamAnOld.ACTION.param] = value.action.key
        mutableParams[AnalyticsParamAnOld.ERROR_CODE.param] = value.error.code.toString()
        mutableParams[AnalyticsParamAnOld.ERROR_DESCRIPTION.param] = value.error.javaClass.simpleName
        mutableParams[AnalyticsParamAnOld.ERROR_KEY.param] = "TangemSdkError"

        return ErrorEvent(TangemSdk.map(value.error), mutableParams)
    }

    internal data class Model(
        val error: TangemSdkError,
        val action: AnalyticsAnOld.ActionToLog,
        val params: Map<AnalyticsParamAnOld, String>,
        val cardParams: Map<String, String>,
    )
}

internal class BlockchainSdkErrorConverter(
    private val cardSdkErrorConverter: CardSdkErrorConverter,
) : Converter<BlockchainSdkErrorConverter.Model, ErrorEvent?> {

    override fun convert(value: Model): ErrorEvent? {
        val error = (value.error as? BlockchainSdkError) ?: return null

        if (error is BlockchainSdkError.WrappedTangemError) {
            val tangemSdkError = (error.tangemError as? TangemSdkError) ?: return null

            val cardSdkModel = CardSdkErrorConverter.Model(
                error = tangemSdkError,
                action = value.action,
                params = value.params,
                cardParams = value.cardParams,
            )
            return cardSdkErrorConverter.convert(cardSdkModel)
        }

        if (error.customMessage.contains(DemoTransactionSender.ID)) return null

        val mutableParams = value.params.convertToParamValue().toMutableMap()
        mutableParams[AnalyticsParamAnOld.ACTION.param] = value.action.key
        mutableParams[AnalyticsParamAnOld.ERROR_CODE.param] = error.code.toString()
        mutableParams[AnalyticsParamAnOld.ERROR_DESCRIPTION.param] = "${error.javaClass.simpleName}: ${error.customMessage}"
        mutableParams[AnalyticsParamAnOld.ERROR_KEY.param] = "BlockchainSdkError"

        return ErrorEvent(value.error, mutableParams)
    }

    data class Model(
        val error: BlockchainError,
        val action: AnalyticsAnOld.ActionToLog,
        val params: Map<AnalyticsParamAnOld, String>,
        val cardParams: Map<String, String>,
    )
}

internal fun Map<AnalyticsParamAnOld, String>.convertToParamValue(): Map<String, String> {
    return mutableMapOf<String, String>().also { newMap ->
        this.forEach {
            newMap[it.key.param] = it.value
        }
    }
}