package com.tangem.commands.verifycard

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.commands.Command
import com.tangem.commands.CommandResponse
import com.tangem.commands.common.network.Result
import com.tangem.commands.common.network.TangemService
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.CardType
import com.tangem.common.extensions.getType
import com.tangem.common.extensions.toHexString
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.CryptoUtils
import kotlinx.coroutines.launch

class VerifyCardResponse(
    val cardId: String,
    val verificationState: VerifyCardState? = null,
    val artworkInfo: ArtworkInfo? = null,
    internal val salt: ByteArray,
    internal val cardSignature: ByteArray
) : CommandResponse {

    fun verify(publicKey: ByteArray, challenge: ByteArray): Boolean {
        return CryptoUtils.verify(
            publicKey,
            challenge + salt,
            cardSignature
        )
    }
}

enum class VerifyCardState {
    VerifiedOnline,
    VerifiedOffline,
}

class VerifyCardCommand(private val onlineVerification: Boolean) : Command<VerifyCardResponse>() {

    private val challenge = CryptoUtils.generateRandomBytes(16)
    private val tangemService = TangemService()

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<VerifyCardResponse>) -> Unit
    ) {
        val card = session.environment.card
        val cardPublicKey = card?.cardPublicKey
        if (cardPublicKey == null) {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }
        super.run(session) { result ->
            when (result) {
                is CompletionResult.Failure -> {
                    callback(CompletionResult.Failure(result.error))
                }
                is CompletionResult.Success -> {
                    val response = result.data
                    val verified = response.verify(cardPublicKey, challenge)
                    if (!verified) {
                        callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                        return@run
                    }
                    if (!onlineVerification || card.getType() != CardType.Release) {
                        callback(
                            CompletionResult.Success(
                                VerifyCardResponse(
                                    response.cardId, VerifyCardState.VerifiedOffline, null,
                                    response.salt, response.cardSignature
                                )
                            )
                        )
                    } else {
                        verify(result.data, card.cardId, cardPublicKey, session, callback)
                    }
                }
            }
        }
    }

    private fun verify(
        response: VerifyCardResponse, cardId: String, cardPublicKey: ByteArray,
        session: CardSession,
        callback: (result: CompletionResult<VerifyCardResponse>) -> Unit
    ) {
        session.scope.launch {
            val result = tangemService.verifyAndGetInfo(cardId, cardPublicKey.toHexString())

            when (result) {
                is Result.Success -> {
                    if (result.data.results?.firstOrNull()?.passed == true) {
                        callback(
                            CompletionResult.Success(
                                VerifyCardResponse(
                                    response.cardId, VerifyCardState.VerifiedOnline, response.artworkInfo,
                                    response.salt, response.cardSignature
                                )
                            )
                        )
                    } else {
                        callback(CompletionResult.Failure(TangemSdkError.VerificationFailed()))
                    }
                }
                is Result.Failure -> {
                    callback(
                        CompletionResult.Success(
                            VerifyCardResponse(
                                response.cardId, VerifyCardState.VerifiedOffline, null,
                                response.salt, response.cardSignature
                            )
                        )
                    )
                }
            }
        }
    }

    override fun performPreCheck(card: Card): TangemSdkError? {
        if (card.status == CardStatus.NotPersonalized) {
            return TangemSdkError.NotPersonalized()
        }
        if (card.isActivated) {
            return TangemSdkError.NotActivated()
        }
        return null
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Challenge, challenge)
        return CommandApdu(Instruction.VerifyCard, tlvBuilder.serialize())
    }

    override fun deserialize(
        environment: SessionEnvironment,
        apdu: ResponseApdu
    ): VerifyCardResponse {
        val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return VerifyCardResponse(
            cardId = decoder.decode(TlvTag.CardId),
            salt = decoder.decode(TlvTag.Salt),
            cardSignature = decoder.decode(TlvTag.CardSignature)
        )
    }
}