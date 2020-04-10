package com.tangem.commands

import com.tangem.CardEnvironment
import com.tangem.SessionError
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

    override fun serialize(environment: CardEnvironment): CommandApdu {
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

    override fun deserialize(environment: CardEnvironment, apdu: ResponseApdu): CreateWalletResponse {
        val tlvData = apdu.getTlvData(environment.encryptionKey)
                ?: throw SessionError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return CreateWalletResponse(
                cardId = decoder.decode(TlvTag.CardId),
                status = decoder.decode(TlvTag.Status),
                walletPublicKey = decoder.decode(TlvTag.WalletPublicKey)
        )
    }
}