package com.tangem.tap.domain.twins

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.KeyPair
import com.tangem.TangemSdkError
import com.tangem.commands.*
import com.tangem.common.CompletionResult
import com.tangem.common.TangemSdkConstants
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.guard
import com.tangem.common.files.FileHashHelper
import com.tangem.tap.domain.extensions.getDefaultWalletIndex

class WriteProtectedIssuerDataTask(
        private val twinPublicKey: ByteArray, private val issuerKeys: KeyPair
) : CardSessionRunnable<WriteIssuerDataResponse> {
    override val requiresPin2 = true

    override fun run(session: CardSession, callback: (result: CompletionResult<WriteIssuerDataResponse>) -> Unit) {
        val hashes = arrayOf(twinPublicKey.calculateSha256())
        SignCommand(hashes, TangemSdkConstants.getDefaultWalletIndex()).run(session) { signResult ->
            when (signResult) {
                is CompletionResult.Success -> {
                    ReadIssuerDataCommand().run(session) { readResult ->
                        when (readResult) {
                            is CompletionResult.Success -> {
                                writeIssuerData(
                                        twinPublicKey, issuerKeys, signResult.data.signatures[0],
                                        readResult.data, session, callback
                                )
                            }
                            is CompletionResult.Failure -> callback(CompletionResult.Failure(readResult.error))
                        }
                    }
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(signResult.error))
            }
        }
    }

    private fun writeIssuerData(
            twinPublicKey: ByteArray, issuerKeys: KeyPair, cardSignature: ByteArray,
            readResponse: ReadIssuerDataResponse,
            session: CardSession, callback: (result: CompletionResult<WriteIssuerDataResponse>
            ) -> Unit) {
        val cardId = session.environment.card?.cardId.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        val counter = (readResponse.issuerDataCounter ?: 0) + 1
        val data = twinPublicKey + cardSignature
        val signedByIssuer = FileHashHelper.prepareHashes(
                cardId, data, counter, issuerKeys.privateKey
        )
        WriteIssuerDataCommand(
                data, signedByIssuer.finalizingSignature!!,
                counter, issuerKeys.publicKey
        ).run(session, callback)
    }
}