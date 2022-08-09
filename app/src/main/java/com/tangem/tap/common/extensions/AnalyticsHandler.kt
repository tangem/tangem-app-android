package com.tangem.tap.common.extensions

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.AnalyticsHandler
import com.tangem.tap.common.analytics.AnalyticsParam
import com.tangem.tap.features.demo.DemoTransactionSender

/**
[REDACTED_AUTHOR]
 */
fun AnalyticsHandler.logSendTransactionError(
    error: BlockchainError,
    action: Analytics.ActionToLog,
    parameters: Map<AnalyticsParam, String>? = mapOf(),
    card: Card? = null,
) {
    when (val blockchainSdkError = (error as BlockchainSdkError)) {
        is BlockchainSdkError.WrappedTangemError -> {
            val tangemSdkError = (blockchainSdkError.tangemError as? TangemSdkError) ?: return

            logCardSdkError(
                error = tangemSdkError,
                actionToLog = action,
                parameters = parameters,
                card = card,
            )
        }
        else -> {
            when {
                blockchainSdkError.customMessage.contains(DemoTransactionSender.ID) -> return
                else -> {
                    val params = parameters?.toMutableMap() ?: mutableMapOf()
                    params[AnalyticsParam.ACTION] = action.key
                    params[AnalyticsParam.ERROR_CODE] = error.code.toString()
                    params[AnalyticsParam.ERROR_DESCRIPTION] = "${error.javaClass.simpleName}: ${error.customMessage}"
                    params[AnalyticsParam.ERROR_KEY] = "BlockchainSdkError"

                    FirebaseCrashlytics.getInstance().apply {
                        params.forEach { setCustomKey(it.key.param, it.value) }
                        recordException(error)
                    }
                }
            }
        }
    }
}