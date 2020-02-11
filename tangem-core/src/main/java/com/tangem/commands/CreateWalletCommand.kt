package com.tangem.commands

import com.tangem.common.CardEnvironment
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvMapper
import com.tangem.common.tlv.TlvTag
import com.tangem.tasks.TaskError

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
class CreateWalletCommand : CommandSerializer<CreateWalletResponse>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Pin, cardEnvironment.pin1)
        tlvBuilder.append(TlvTag.CardId, cardEnvironment.cardId)
        tlvBuilder.append(TlvTag.Pin2, cardEnvironment.pin2)
        tlvBuilder.append(TlvTag.Cvc, cardEnvironment.cvc)
        return CommandApdu(Instruction.CreateWallet, tlvBuilder.serialize())
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): CreateWalletResponse? {
        val tlvData = responseApdu.getTlvData() ?: return null

        return try {
            val mapper = TlvMapper(tlvData)
            CreateWalletResponse(
                    cardId = mapper.map(TlvTag.CardId),
                    status = mapper.map(TlvTag.Status),
                    walletPublicKey = mapper.map(TlvTag.WalletPublicKey)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }
}