package com.tangem.commands

import com.tangem.CardSession
import com.tangem.SessionEnvironment
import com.tangem.TangemSdkError
import com.tangem.common.CompletionResult
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

class CreateWalletResponse(
        /**
         * CID, Unique Tangem card ID number.
         */
        val cardId: String,
        /**
         * Current status of the card [1 - Empty, 2 - Loaded, 3- Purged]
         */
        val status: CardStatus,
        /**

         */
        val walletPublicKey: ByteArray
) : CommandResponse

/**
 * This command will create a new wallet on the card having ‘Empty’ state.
 * A key pair WalletPublicKey / WalletPrivateKey is generated and securely stored in the card.
 * App will need to obtain Wallet_PublicKey from the response of [CreateWalletCommand] or [ReadCommand]
 * and then transform it into an address of corresponding blockchain wallet
 * according to a specific blockchain algorithm.
 * WalletPrivateKey is never revealed by the card and will be used by [SignCommand] and [CheckWalletCommand].
 * RemainingSignature is set to MaxSignatures.
 *
 * @property cardId CID, Unique Tangem card ID number.
 */
class CreateWalletCommand : Command<CreateWalletResponse>() {

    override fun performPreCheck(session: CardSession, callback: (result: CompletionResult<CreateWalletResponse>) -> Unit): Boolean {
        if (session.environment.card?.status == CardStatus.NotPersonalized) {
            callback(CompletionResult.Failure(TangemSdkError.NotPersonalized()))
            return true
        }
        if (session.environment.card?.isActivated == true) {
            callback(CompletionResult.Failure(TangemSdkError.NotActivated()))
            return true
        }
        if (session.environment.card?.status == CardStatus.Purged) {
            callback(CompletionResult.Failure(TangemSdkError.CardIsPurged()))
            return true
        }
        if (session.environment.card?.status == CardStatus.Loaded) {
            callback(CompletionResult.Failure(TangemSdkError.AlreadyCreated()))
            return true
        }
        return false
    }

    override fun performAfterCheck(session: CardSession,
                                   result: CompletionResult<CreateWalletResponse>,
                                   callback: (result: CompletionResult<CreateWalletResponse>) -> Unit): Boolean {
        when (result) {
            is CompletionResult.Failure -> {
                if (result.error is TangemSdkError.InvalidParams) {
                    callback(CompletionResult.Failure(TangemSdkError.Pin2OrCvcRequired()))
                    return true
                }
                return false
            }
            else -> return false
        }
    }

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, environment.pin1)
        tlvBuilder.append(TlvTag.CardId, environment.card?.cardId)
        tlvBuilder.append(TlvTag.Pin2, environment.pin2)
        tlvBuilder.append(TlvTag.Cvc, environment.cvc)
        return CommandApdu(
                Instruction.CreateWallet, tlvBuilder.serialize(),
                environment.encryptionMode, environment.encryptionKey
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): CreateWalletResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw TangemSdkError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return CreateWalletResponse(
                cardId = decoder.decode(TlvTag.CardId),
                status = decoder.decode(TlvTag.Status),
                walletPublicKey = decoder.decode(TlvTag.WalletPublicKey)
        )
    }
}