package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.tasks.ScanNoteTask

class FinalizeTwinTask(
    private val twinPublicKey: ByteArray, private val issuerKeys: KeyPair,
) : CardSessionRunnable<ScanNoteResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit,
    ) {
        WriteProtectedIssuerDataTask(twinPublicKey, issuerKeys).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    PreflightReadTask(PreflightReadMode.FullCardRead).run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success ->
                                ScanNoteTask(readResult.data).run(session, callback)
                            is CompletionResult.Failure ->
                                callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}