package com.tangem.commands.personalization

import com.tangem.commands.Card
import com.tangem.commands.CardData
import com.tangem.commands.CommandSerializer
import com.tangem.commands.personalization.entities.*
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
import com.tangem.crypto.sign
import com.tangem.tasks.TaskError

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
) : CommandSerializer<Card>() {

    override fun serialize(cardEnvironment: CardEnvironment): CommandApdu {
        return CommandApdu(
                Instruction.Personalize,
                serializePersonalizationData(config),
                encryptionKey = devPersonalizationKey
        )
    }

    override fun deserialize(cardEnvironment: CardEnvironment, responseApdu: ResponseApdu): Card? {
        val tlvData = responseApdu.getTlvData(devPersonalizationKey) ?: return null

        return try {
            val tlvMapper = TlvMapper(tlvData)
            Card(
                    cardId = tlvMapper.mapOptional(TlvTag.CardId) ?: "",
                    manufacturerName = tlvMapper.mapOptional(TlvTag.ManufactureId) ?: "",
                    status = tlvMapper.mapOptional(TlvTag.Status),

                    firmwareVersion = tlvMapper.mapOptional(TlvTag.Firmware),
                    cardPublicKey = tlvMapper.mapOptional(TlvTag.CardPublicKey),
                    settingsMask = tlvMapper.mapOptional(TlvTag.SettingsMask),
                    issuerPublicKey = tlvMapper.mapOptional(TlvTag.IssuerDataPublicKey),
                    curve = tlvMapper.mapOptional(TlvTag.CurveId),
                    maxSignatures = tlvMapper.mapOptional(TlvTag.MaxSignatures),
                    signingMethod = tlvMapper.mapOptional(TlvTag.SigningMethod),
                    pauseBeforePin2 = tlvMapper.mapOptional(TlvTag.PauseBeforePin2),
                    walletPublicKey = tlvMapper.mapOptional(TlvTag.WalletPublicKey),
                    walletRemainingSignatures = tlvMapper.mapOptional(TlvTag.RemainingSignatures),
                    walletSignedHashes = tlvMapper.mapOptional(TlvTag.SignedHashes),
                    health = tlvMapper.mapOptional(TlvTag.Health),
                    isActivated = tlvMapper.map(TlvTag.IsActivated),
                    activationSeed = tlvMapper.mapOptional(TlvTag.ActivationSeed),
                    paymentFlowVersion = tlvMapper.mapOptional(TlvTag.PaymentFlowVersion),
                    userCounter = tlvMapper.mapOptional(TlvTag.UserCounter),
                    terminalIsLinked = tlvMapper.map(TlvTag.TerminalIsLinked),

                    cardData = deserializeCardData(tlvData)
            )
        } catch (exception: Exception) {
            throw TaskError.SerializeCommandError()
        }
    }

    private fun deserializeCardData(tlvData: List<Tlv>): CardData? {
        val cardDataTlvs = tlvData.find { it.tag == TlvTag.CardData }?.let {
            Tlv.deserialize(it.value)
        }
        if (cardDataTlvs.isNullOrEmpty()) return null

        val tlvMapper = TlvMapper(cardDataTlvs)
        return CardData(
                batchId = tlvMapper.mapOptional(TlvTag.Batch),
                manufactureDateTime = tlvMapper.mapOptional(TlvTag.ManufactureDateTime),
                issuerName = tlvMapper.mapOptional(TlvTag.IssuerId),
                blockchainName = tlvMapper.mapOptional(TlvTag.BlockchainId),
                manufacturerSignature = tlvMapper.mapOptional(TlvTag.ManufacturerSignature),
                productMask = tlvMapper.mapOptional(TlvTag.ProductMask),

                tokenSymbol = tlvMapper.mapOptional(TlvTag.TokenSymbol),
                tokenContractAddress = tlvMapper.mapOptional(TlvTag.TokenContractAddress),
                tokenDecimal = tlvMapper.mapOptional(TlvTag.TokenDecimal)
        )
    }

    private fun serializePersonalizationData(config: CardConfig): ByteArray {
        val cardId = config.createCardId() ?: throw TaskError.SerializeCommandError()

        val tlvBuilder = TlvBuilder()
        tlvBuilder.append(TlvTag.CardId, cardId)
        tlvBuilder.append(TlvTag.CurveId, config.curveID)
        tlvBuilder.append(TlvTag.MaxSignatures, config.maxSignatures)
        tlvBuilder.append(TlvTag.SigningMethod, config.signingMethod)
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