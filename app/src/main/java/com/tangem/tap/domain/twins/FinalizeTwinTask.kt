package com.tangem.tap.domain.twins

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.KeyPair
import com.tangem.common.CompletionResult
import com.tangem.tap.domain.tasks.ScanNoteResponse
import com.tangem.tap.domain.tasks.ScanNoteTask
import com.tangem.tasks.PreflightReadSettings
import com.tangem.tasks.PreflightReadTask

class FinalizeTwinTask(
    private val twinPublicKey: ByteArray, private val issuerKeys: KeyPair,
) : CardSessionRunnable<ScanNoteResponse> {
    override val requiresPin2 = true

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanNoteResponse>) -> Unit,
    ) {
        WriteProtectedIssuerDataTask(twinPublicKey, issuerKeys).run(session) { result ->
            when (result) {
                is CompletionResult.Success ->
                    PreflightReadTask(PreflightReadSettings.FullCardRead).run(session) { readResult ->
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