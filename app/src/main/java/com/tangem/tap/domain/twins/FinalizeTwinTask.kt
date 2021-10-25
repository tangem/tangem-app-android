package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.tap.domain.tasks.product.ScanProductTask
import com.tangem.tap.domain.tasks.product.ScanResponse

class FinalizeTwinTask(
    private val twinPublicKey: ByteArray, private val issuerKeys: KeyPair,
) : CardSessionRunnable<ScanResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        WriteProtectedIssuerDataTask(twinPublicKey, issuerKeys).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    PreflightReadTask(PreflightReadMode.FullCardRead).run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success ->
                                ScanProductTask(readResult.data).run(session, callback)
                            is CompletionResult.Failure ->
                                callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}