package com.tangem.devkit.ucase.variants.personalize.converter

import com.tangem.commands.CardData
import com.tangem.commands.EllipticCurve
import com.tangem.commands.Product
import com.tangem.commands.ProductMaskBuilder
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.commands.personalization.entities.NdefRecord
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationConfig
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter
import java.util.*

class PersonalizationConfigToCardConfig : Converter<PersonalizationConfig, CardConfig> {

    override fun convert(from: PersonalizationConfig): CardConfig {
        val signingMethod = PersonalizationConfig.makeSigningMethodMask(from)

        val isNote = from.cardData.product_note
        val isTag = from.cardData.product_tag
        val isIdCard = from.cardData.product_id_card
        val isIdIssuer = from.cardData.product_id_issuer

        val productMaskBuilder = ProductMaskBuilder()
        if (isNote) productMaskBuilder.add(Product.Note)
        if (isTag) productMaskBuilder.add(Product.Tag)
        if (isIdCard) productMaskBuilder.add(Product.IdCard)
        if (isIdIssuer) productMaskBuilder.add(Product.IdIssuer)
        val productMask = productMaskBuilder.build()

        var tokenSymbol: String? = null
        var tokenContractAddress: String? = null
        var tokenDecimal: Int? = null
        if (from.itsToken) {
            tokenSymbol = from.cardData.token_symbol
            tokenContractAddress = from.cardData.token_contract_address
            tokenDecimal = from.cardData.token_decimal
        }

        val blockchain = if (from.cardData.blockchain.isNotEmpty()) from.cardData.blockchain else from.blockchainCustom
        val cardData = CardData(
                blockchainName = blockchain,
                batchId = from.cardData.batch,
                productMask = productMask,
                tokenSymbol = tokenSymbol,
                tokenContractAddress = tokenContractAddress,
                tokenDecimal = tokenDecimal,
                issuerName = null,
                manufactureDateTime = Calendar.getInstance().time,
                manufacturerSignature = null)


        val ndefs = mutableListOf<NdefRecord>()
        if (from.uri.isNotEmpty()) {
            ndefs.add(NdefRecord(NdefRecord.Type.URI, from.uri))
        }
        when (from.aar) {
            "None" -> null
            "--- CUSTOM ---" -> NdefRecord(NdefRecord.Type.AAR, from.aarCustom)
            else -> NdefRecord(NdefRecord.Type.AAR, from.aar)
        }?.let { ndefs.add(it) }

        return CardConfig(
                "Tangem",
                "Tangem Test",
                from.series,
                from.startNumber,
                1000,
                from.PIN,
                from.PIN2,
                from.PIN3,
                from.hexCrExKey,
                from.CVC,
                from.pauseBeforePIN2.toInt(),
                from.smartSecurityDelay,
                EllipticCurve.byName(from.curveID) ?: EllipticCurve.Secp256k1,
                signingMethod,
                from.MaxSignatures.toInt(),
                from.isReusable,
                from.allowSwapPIN,
                from.allowSwapPIN2,
                from.useActivation,
                from.useCVC,
                from.useNDEF,
                from.useDynamicNDEF,
                from.useOneCommandAtTime,
                from.useBlock,
                from.allowSelectBlockchain,
                from.forbidPurgeWallet,
                from.protocolAllowUnencrypted,
                from.protocolAllowStaticEncryption,
                from.protectIssuerDataAgainstReplay,
                from.forbidDefaultPIN,
                from.disablePrecomputedNDEF,
                from.skipSecurityDelayIfValidatedByIssuer,
                from.skipCheckPIN2andCVCIfValidatedByIssuer,
                from.skipSecurityDelayIfValidatedByLinkedTerminal,
                from.restrictOverwriteIssuerDataEx,
                from.requireTerminalTxSignature,
                from.requireTerminalCertSignature,
                from.checkPIN3onCard,
                from.createWallet,
                cardData,
                ndefs
        )
    }
}