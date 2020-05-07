package com.tangem.devkit.ucase.variants.personalize.dto

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tangem.commands.EllipticCurve
import com.tangem.commands.Product
import com.tangem.commands.ProductMaskBuilder
import com.tangem.commands.SigningMethodMask
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.commands.personalization.entities.NdefRecord
import java.util.*

class PersonalizationJson {

    var issuerName = ""
    var series = ""
    var startNumber = 0L
    var count = 0L
    var PIN = ""
    var PIN2 = ""
    var PIN3 = ""
    var hexCrExKey = ""
    var CVC = ""
    var pauseBeforePIN2 = 0L
    var smartSecurityDelay = false
    var curveID = ""
    var SigningMethod = 0L
    var MaxSignatures = 0L
    var isReusable = false
    var allowSwapPIN = false
    var allowSwapPIN2 = false
    var useActivation = false
    var useCVC = false
    var useNDEF = false
    var useDynamicNDEF = false
    var useOneCommandAtTime = false
    var useBlock = false
    var allowSelectBlockchain = false
    var forbidPurgeWallet = false
    var protocolAllowUnencrypted = false
    var protocolAllowStaticEncryption = false
    var protectIssuerDataAgainstReplay = false
    var forbidDefaultPIN = false
    var disablePrecomputedNDEF = false
    var skipSecurityDelayIfValidatedByIssuer = false
    var skipCheckPIN2andCVCIfValidatedByIssuer = false
    var skipSecurityDelayIfValidatedByLinkedTerminal = false
    var restrictOverwriteIssuerDataEx = false
    var requireTerminalTxSignature = false
    var requireTerminalCertSignature = false
    var checkPIN3onCard = false
    var createWallet = 0L
    var issuerData = null

    var ndef = mutableListOf<NdefRecord>()
    var cardData = CardData()

    var releaseVersion = false
    var numberFormat = ""

    companion object {
        const val CUSTOM = "--- CUSTOM ---"

        fun getJsonConverter(): Gson {
            val builder = GsonBuilder().setPrettyPrinting()
            return builder.create()
        }
    }
}

fun PersonalizationJson.toCardConfig(): CardConfig {
    val isNote = cardData.product_note
    val isTag = cardData.product_tag
    val isIdCard = cardData.product_id_card
    val isIdIssuer = cardData.product_id_issuer

    val productMaskBuilder = ProductMaskBuilder()
    if (isNote) productMaskBuilder.add(Product.Note)
    if (isTag) productMaskBuilder.add(Product.Tag)
    if (isIdCard) productMaskBuilder.add(Product.IdCard)
    if (isIdIssuer) productMaskBuilder.add(Product.IdIssuer)
    val productMask = productMaskBuilder.build()

    val sdkCardData = com.tangem.commands.CardData(
            blockchainName = this.cardData.blockchain,
            batchId = this.cardData.batch,
            productMask = productMask,
            tokenSymbol = this.cardData.token_symbol,
            tokenContractAddress = this.cardData.token_contract_address,
            tokenDecimal = this.cardData.token_decimal?.toInt(),
            issuerName = this.issuerName,
            manufactureDateTime = Calendar.getInstance().time,
            manufacturerSignature = null)

    return CardConfig(
            this.issuerName,
            "Tangem Test",
            this.series,
            this.startNumber,
            this.count.toInt(),
            this.PIN,
            this.PIN2,
            this.PIN3,
            this.hexCrExKey,
            this.CVC,
            this.pauseBeforePIN2.toInt(),
            this.smartSecurityDelay,
            EllipticCurve.byName(this.curveID) ?: EllipticCurve.Secp256k1,
            SigningMethodMask(this.SigningMethod.toInt()),
            this.MaxSignatures.toInt(),
            this.isReusable,
            this.allowSwapPIN,
            this.allowSwapPIN2,
            this.useActivation,
            this.useCVC,
            this.useNDEF,
            this.useDynamicNDEF,
            this.useOneCommandAtTime,
            this.useBlock,
            this.allowSelectBlockchain,
            this.forbidPurgeWallet,
            this.protocolAllowUnencrypted,
            this.protocolAllowStaticEncryption,
            this.protectIssuerDataAgainstReplay,
            this.forbidDefaultPIN,
            this.disablePrecomputedNDEF,
            this.skipSecurityDelayIfValidatedByIssuer,
            this.skipCheckPIN2andCVCIfValidatedByIssuer,
            this.skipSecurityDelayIfValidatedByLinkedTerminal,
            this.restrictOverwriteIssuerDataEx,
            this.requireTerminalTxSignature,
            this.requireTerminalCertSignature,
            this.checkPIN3onCard,
            this.createWallet != 0L,
            sdkCardData,
            this.ndef
    )
}