package com.tangem.commands.personalization

import com.tangem.SessionEnvironment
import com.tangem.SessionError
import com.tangem.commands.Card
import com.tangem.commands.CardData
import com.tangem.commands.Command
import com.tangem.commands.personalization.entities.*
import com.tangem.common.apdu.CommandApdu
import com.tangem.common.apdu.Instruction
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvBuilder
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag
import com.tangem.crypto.sign

/**
 * Command available on SDK cards only
 *
 * Personalization is an initialization procedure, required before starting using a card.
 * During this procedure a card setting is set up.
 * During this procedure all data exchange is encrypted.
 * @property config is a configuration file with all the card settings that are written on the card
 * during personalization.
 * @property issuer Issuer is a third-party team or company wishing to use Tangem cards.
 * @property manufacturer Tangem Card Manufacturer.
 * @property acquirer Acquirer is a trusted third-party company that operates proprietary
 * (non-EMV) POS terminal infrastructure and transaction processing back-end.
 */
class PersonalizeCommand(
        private val config: CardConfig,
        private val issuer: Issuer, private val manufacturer: Manufacturer,
        private val acquirer: Acquirer? = null
) : Command<Card>() {

    override fun serialize(environment: SessionEnvironment): CommandApdu {
        return CommandApdu(
                Instruction.Personalize,
                serializePersonalizationData(config),
                encryptionKey = devPersonalizationKey
        )
    }

    override fun deserialize(environment: SessionEnvironment, apdu: ResponseApdu): Card {
        val tlvData = apdu.getTlvData(devPersonalizationKey)
                ?: throw SessionError.DeserializeApduFailed()

        val decoder = TlvDecoder(tlvData)
        return Card(
                cardId = decoder.decodeOptional(TlvTag.CardId) ?: "",
                manufacturerName = decoder.decodeOptional(TlvTag.ManufactureId) ?: "",
                status = decoder.decodeOptional(TlvTag.Status),

                firmwareVersion = decoder.decodeOptional(TlvTag.Firmware),
                cardPublicKey = decoder.decodeOptional(TlvTag.CardPublicKey),
                settingsMask = decoder.decodeOptional(TlvTag.SettingsMask),
                issuerPublicKey = decoder.decodeOptional(TlvTag.IssuerDataPublicKey),
                curve = decoder.decodeOptional(TlvTag.CurveId),
                maxSignatures = decoder.decodeOptional(TlvTag.MaxSignatures),
                signingMethods = decoder.decodeOptional(TlvTag.SigningMethod),
                pauseBeforePin2 = decoder.decodeOptional(TlvTag.PauseBeforePin2),
                walletPublicKey = decoder.decodeOptional(TlvTag.WalletPublicKey),
                walletRemainingSignatures = decoder.decodeOptional(TlvTag.RemainingSignatures),
                walletSignedHashes = decoder.decodeOptional(TlvTag.SignedHashes),
                health = decoder.decodeOptional(TlvTag.Health),
                isActivated = decoder.decode(TlvTag.IsActivated),
                activationSeed = decoder.decodeOptional(TlvTag.ActivationSeed),
                paymentFlowVersion = decoder.decodeOptional(TlvTag.PaymentFlowVersion),
                userCounter = decoder.decodeOptional(TlvTag.UserCounter),
                userProtectedCounter = decoder.decodeOptional(TlvTag.UserProtectedCounter),
                terminalIsLinked = decoder.decode(TlvTag.TerminalIsLinked),

                cardData = deserializeCardData(tlvData)
        )
    }

    private fun deserializeCardData(tlvData: List<Tlv>): CardData? {
        val cardDataTlvs = tlvData.find { it.tag == TlvTag.CardData }?.let {
            Tlv.deserialize(it.value)
        }
        if (cardDataTlvs.isNullOrEmpty()) return null

        val decoder = TlvDecoder(cardDataTlvs)
        return CardData(
                batchId = decoder.decodeOptional(TlvTag.Batch),
                manufactureDateTime = decoder.decodeOptional(TlvTag.ManufactureDateTime),
                issuerName = decoder.decodeOptional(TlvTag.IssuerId),
                blockchainName = decoder.decodeOptional(TlvTag.BlockchainId),
                manufacturerSignature = decoder.decodeOptional(TlvTag.ManufacturerSignature),
                productMask = decoder.decodeOptional(TlvTag.ProductMask),

                tokenSymbol = decoder.decodeOptional(TlvTag.TokenSymbol),
                tokenContractAddress = decoder.decodeOptional(TlvTag.TokenContractAddress),
                tokenDecimal = decoder.decodeOptional(TlvTag.TokenDecimal)
        )
    }

    private fun serializePersonalizationData(config: CardConfig): ByteArray {
        val cardId = config.createCardId() ?: throw SessionError.SerializeCommandError()

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, cardId)
        tlvBuilder.append(TlvTag.CurveId, config.curveID)
        tlvBuilder.append(TlvTag.MaxSignatures, config.maxSignatures)
        tlvBuilder.append(TlvTag.SigningMethod, config.signingMethods)
        tlvBuilder.append(TlvTag.SettingsMask, config.createSettingsMask())
        tlvBuilder.append(TlvTag.PauseBeforePin2, config.pauseBeforePin2 / 10)
        tlvBuilder.append(TlvTag.Cvc, config.cvc.toByteArray())
        if (!config.ndefRecords.isNullOrEmpty())
            tlvBuilder.append(TlvTag.NdefData, serializeNdef(config))

        tlvBuilder.append(TlvTag.CreateWalletAtPersonalize, config.createWallet)

        tlvBuilder.append(TlvTag.NewPin, config.pin)
        tlvBuilder.append(TlvTag.NewPin2, config.pin2)
        tlvBuilder.append(TlvTag.NewPin3, config.pin3)
        tlvBuilder.append(TlvTag.CrExKey, config.hexCrExKey)
        tlvBuilder.append(TlvTag.IssuerDataPublicKey, issuer.dataKeyPair.publicKey)
        tlvBuilder.append(TlvTag.IssuerTransactionPublicKey, issuer.transactionKeyPair.publicKey)

        tlvBuilder.append(TlvTag.AcquirerPublicKey, acquirer?.keyPair?.publicKey)

        tlvBuilder.append(TlvTag.CardData, serializeCardData(cardId, config.cardData))
        return tlvBuilder.serialize()
    }

    private fun serializeNdef(config: CardConfig): ByteArray {
        return NdefEncoder(config.ndefRecords, config.useDynamicNdef).encode()
    }

    private fun serializeCardData(cardId: String, cardData: CardData): ByteArray {
        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.Batch, cardData.batchId)
        tlvBuilder.append(TlvTag.ProductMask, cardData.productMask)
        tlvBuilder.append(TlvTag.ManufactureDateTime, cardData.manufactureDateTime)
        tlvBuilder.append(TlvTag.IssuerId, issuer.id)
        tlvBuilder.append(TlvTag.BlockchainId, cardData.blockchainName)

        if (cardData.tokenSymbol != null) {
            tlvBuilder.append(TlvTag.TokenSymbol, cardData.tokenSymbol)
            tlvBuilder.append(TlvTag.TokenContractAddress, cardData.tokenContractAddress)
            tlvBuilder.append(TlvTag.TokenDecimal, cardData.tokenDecimal)
        }
        tlvBuilder.append(
                TlvTag.CardIdManufacturerSignature,
                cardId.hexToBytes().sign(manufacturer.keyPair.privateKey)
        )
        return tlvBuilder.serialize()
    }

    companion object {
        val devPersonalizationKey = "1234".calculateSha256().copyOf(32)
    }
}