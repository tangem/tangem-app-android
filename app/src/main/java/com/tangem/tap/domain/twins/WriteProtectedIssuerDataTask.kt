package com.tangem.tap.domain.twins

import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.SuccessResponse
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.guard
import com.tangem.operations.files.FileHashHelper
import com.tangem.operations.issuerAndUserData.ReadIssuerDataCommand
import com.tangem.operations.issuerAndUserData.ReadIssuerDataResponse
import com.tangem.operations.issuerAndUserData.WriteIssuerDataCommand
import com.tangem.operations.sign.SignHashCommand

class WriteProtectedIssuerDataTask(
    private val twinPublicKey: ByteArray, private val issuerKeys: KeyPair,
) : CardSessionRunnable<SuccessResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<SuccessResponse>) -> Unit,
    ) {
        SignHashCommand(
            twinPublicKey.calculateSha256(),
            session.environment.card!!.wallets.first().publicKey,
        )
            .run(session) { signResult ->
                when (signResult) {
                    is CompletionResult.Success -> {
                        ReadIssuerDataCommand().run(session) { readResult ->
                            when (readResult) {
                                is CompletionResult.Success -> {
                                    writeIssuerData(
                                        twinPublicKey, issuerKeys, signResult.data.signature,
                                        readResult.data, session, callback
                                    )
                                }
                                is CompletionResult.Failure -> callback(CompletionResult.Failure(
                                    readResult.error))
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
        session: CardSession,
        callback: (
            result: CompletionResult<SuccessResponse>,
        ) -> Unit,
    ) {
        val cardId = session.environment.card?.cardId.guard {
            callback(CompletionResult.Failure(TangemSdkError.CardError()))
            return
        }
        val counter = (readResponse.issuerDataCounter ?: 0) + 1
        val data = twinPublicKey + cardSignature
        val signedByIssuer = FileHashHelper.prepareHashes(
            cardId = cardId,
            fileData = data,
            fileCounter = counter,
            fileName = null,
            privateKey = issuerKeys.privateKey,
        )
        WriteIssuerDataCommand(
            issuerData = data,
            issuerDataSignature = signedByIssuer.finalizingSignature!!,
            issuerDataCounter = counter,
            issuerPublicKey = issuerKeys.publicKey,
        ).run(session, callback)
    }
}
