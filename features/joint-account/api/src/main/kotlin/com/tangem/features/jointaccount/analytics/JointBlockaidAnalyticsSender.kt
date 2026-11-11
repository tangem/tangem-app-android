package com.tangem.features.jointaccount.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate.Verdict
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.BlockaidResult
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.BlockaidSource
import com.tangem.features.jointaccount.blockaid.JointBlockaidSheetKind
import javax.inject.Inject

class JointBlockaidAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun sendCheck(source: BlockaidSource, verdict: Verdict, blockchain: String) {
        analyticsEventHandler.send(
            JointAccountAnalyticsEvent.BlockaidCheck(
                source = source,
                result = verdict.toResult(),
                blockchain = blockchain,
            ),
        )
    }

    fun sendSheetShown(source: BlockaidSource, kind: JointBlockaidSheetKind, blockchain: String) {
        val event = when (kind) {
            JointBlockaidSheetKind.RISKY -> JointAccountAnalyticsEvent.NoticeRiskyAddress(
                source = source,
                blockchain = blockchain,
            )
            JointBlockaidSheetKind.MALICIOUS -> JointAccountAnalyticsEvent.NoticeMaliciousTransaction(
                source = source,
                blockchain = blockchain,
            )
        }
        analyticsEventHandler.send(event)
    }

    fun sendSignAnyway(source: BlockaidSource, blockchain: String) {
        analyticsEventHandler.send(
            JointAccountAnalyticsEvent.ButtonSignAnyway(source = source, blockchain = blockchain),
        )
    }

    private fun Verdict.toResult(): BlockaidResult = when (this) {
        Verdict.Safe -> BlockaidResult.Benign
        Verdict.Warning -> BlockaidResult.Warning
        Verdict.Malicious -> BlockaidResult.Malicious
        is Verdict.NoVerdict -> when (reason) {
            Verdict.NoVerdict.Reason.ERROR -> BlockaidResult.Error
            Verdict.NoVerdict.Reason.TIMEOUT -> BlockaidResult.Timeout
            Verdict.NoVerdict.Reason.UNSUPPORTED_NETWORK -> BlockaidResult.UnsupportedNetwork
        }
    }
}