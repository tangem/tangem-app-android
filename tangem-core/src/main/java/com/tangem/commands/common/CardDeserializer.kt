package com.tangem.commands.common

import com.tangem.TangemSdkError
import com.tangem.commands.Card
import com.tangem.commands.CardData
import com.tangem.common.apdu.ResponseApdu
import com.tangem.common.tlv.Tlv
import com.tangem.common.tlv.TlvDecoder
import com.tangem.common.tlv.TlvTag

class CardDeserializer() {
    companion object {
        fun deserialize(apdu: ResponseApdu): Card {
            val tlvData = apdu.getTlvData() ?: throw TangemSdkError.DeserializeApduFailed()

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
    }
}