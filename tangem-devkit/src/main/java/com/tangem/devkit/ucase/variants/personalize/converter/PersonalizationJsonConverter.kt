package com.tangem.devkit.ucase.variants.personalize.converter

import com.tangem.commands.EllipticCurve
import com.tangem.commands.SigningMethod
import com.tangem.commands.SigningMethodMask
import com.tangem.commands.personalization.entities.NdefRecord
import com.tangem.devkit._arch.structure.abstraction.TwoWayConverter
import com.tangem.devkit.ucase.variants.personalize.dto.CardData
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationConfig
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationJson
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

/**
[REDACTED_AUTHOR]
 */
class PersonalizationJsonConverter : TwoWayConverter<PersonalizationJson, PersonalizationConfig> {

    override fun aToB(from: PersonalizationJson): PersonalizationConfig = JsonToConfig().convert(from)

    override fun bToA(from: PersonalizationConfig): PersonalizationJson = ConfigToJson().convert(from)
}

internal class JsonToConfig : Converter<PersonalizationJson, PersonalizationConfig> {
    override fun convert(from: PersonalizationJson): PersonalizationConfig {
        val config = PersonalizationConfig.default()
        config.apply {
            // Card number
            series = from.series
            startNumber = from.startNumber

            // Common
            curveID = EllipticCurve.byName(from.curveID)?.curve ?: ""
            blockchainCustom = ""
            MaxSignatures = from.MaxSignatures
            createWallet = from.createWallet != 0L

            // Sign hash external properties
//            pinLessFloorLimit = 100000L
            hexCrExKey = from.hexCrExKey
            requireTerminalTxSignature = from.requireTerminalTxSignature
            requireTerminalCertSignature = from.requireTerminalCertSignature
            checkPIN3onCard = from.checkPIN3onCard

            // Product mask
            cardData = from.cardData

            // Settings mask
            isReusable = from.isReusable
            useActivation = from.useActivation
            forbidPurgeWallet = from.forbidPurgeWallet
            allowSelectBlockchain = from.allowSelectBlockchain
            useBlock = from.useBlock
            useOneCommandAtTime = from.useOneCommandAtTime
            useCVC = from.useCVC
            allowSwapPIN = from.allowSwapPIN
            allowSwapPIN2 = from.allowSwapPIN2
            forbidDefaultPIN = from.forbidDefaultPIN
            smartSecurityDelay = from.smartSecurityDelay
            protectIssuerDataAgainstReplay = from.protectIssuerDataAgainstReplay
            skipSecurityDelayIfValidatedByIssuer = from.skipSecurityDelayIfValidatedByIssuer
            skipCheckPIN2andCVCIfValidatedByIssuer = from.skipCheckPIN2andCVCIfValidatedByIssuer
            skipSecurityDelayIfValidatedByLinkedTerminal = from.skipSecurityDelayIfValidatedByLinkedTerminal
            restrictOverwriteIssuerDataEx = from.restrictOverwriteIssuerDataEx

            // Settings mask - protocol encryption
            protocolAllowUnencrypted = from.protocolAllowUnencrypted
            protocolAllowStaticEncryption = from.protocolAllowStaticEncryption

            // Settings mask
            useNDEF = from.useNDEF
            useDynamicNDEF = from.useDynamicNDEF
            disablePrecomputedNDEF = from.disablePrecomputedNDEF

            // Pins
            PIN = from.PIN
            PIN2 = from.PIN2
            PIN3 = from.PIN3
            CVC = from.CVC
            pauseBeforePIN2 = from.pauseBeforePIN2

            count = from.count
            issuerName = from.issuerName
            issuerData = from.issuerData
//            numberFormat = from.numberFormat
        }

        fillToken(config, from.cardData)
        fillSigningMethod(config, from.SigningMethod)
        fillNdef(config, from.ndef)

        return config
    }

    private fun fillToken(config: PersonalizationConfig, cardData: CardData) {
        config.itsToken = cardData.token_contract_address.isNotEmpty() || cardData.token_symbol.isNotEmpty()
        config.cardData.token_contract_address = cardData.token_contract_address
        config.cardData.token_symbol = cardData.token_symbol
        config.cardData.token_decimal = cardData.token_decimal
    }

    private fun fillSigningMethod(config: PersonalizationConfig, signingMethod: Long) {
        val mask = SigningMethodMask(signingMethod.toInt())
        if (mask.contains(SigningMethod.SignHash)) config.SigningMethod0 = true
        if (mask.contains(SigningMethod.SignRaw)) config.SigningMethod1 = true
        if (mask.contains(SigningMethod.SignHashValidateByIssuer)) config.SigningMethod2 = true
        if (mask.contains(SigningMethod.SignRawValidateByIssuer)) config.SigningMethod3 = true
        if (mask.contains(SigningMethod.SignHashValidateByIssuerWriteIssuerData)) config.SigningMethod4 = true
        if (mask.contains(SigningMethod.SignRawValidateByIssuerWriteIssuerData)) config.SigningMethod5 = true
    }

    private fun fillNdef(config: PersonalizationConfig, ndef: MutableList<NdefRecord>) {
        ndef.forEach { record ->
            when (record.type) {
                NdefRecord.Type.URI -> config.uri = record.value
                NdefRecord.Type.AAR -> config.aar = record.value
                NdefRecord.Type.TEXT -> {
                }
            }
        }
    }
}

internal class ConfigToJson : Converter<PersonalizationConfig, PersonalizationJson> {
    override fun convert(from: PersonalizationConfig): PersonalizationJson {
        val jsonDto = PersonalizationJson()
        jsonDto.apply {
            releaseVersion = false
            issuerName = from.issuerName
            series = from.series
            startNumber = from.startNumber
            count = from.count
            PIN = from.PIN
            PIN2 = from.PIN2
            PIN3 = from.PIN3
            hexCrExKey = from.hexCrExKey
            CVC = from.CVC
            pauseBeforePIN2 = from.pauseBeforePIN2
            smartSecurityDelay = from.smartSecurityDelay
            curveID = from.curveID
            MaxSignatures = from.MaxSignatures
            isReusable = from.isReusable
            allowSwapPIN = from.allowSwapPIN
            allowSwapPIN2 = from.allowSwapPIN2
            useActivation = from.useActivation
            useCVC = from.useCVC
            useNDEF = from.useNDEF
            useDynamicNDEF = from.useDynamicNDEF
            useOneCommandAtTime = from.useOneCommandAtTime
            useBlock = from.useBlock
            allowSelectBlockchain = from.allowSelectBlockchain
            forbidPurgeWallet = from.forbidPurgeWallet
            protocolAllowUnencrypted = from.protocolAllowUnencrypted
            protocolAllowStaticEncryption = from.protocolAllowStaticEncryption
            protectIssuerDataAgainstReplay = from.protectIssuerDataAgainstReplay
            forbidDefaultPIN = from.forbidDefaultPIN
            disablePrecomputedNDEF = from.disablePrecomputedNDEF
            skipSecurityDelayIfValidatedByIssuer = from.skipSecurityDelayIfValidatedByIssuer
            skipCheckPIN2andCVCIfValidatedByIssuer = from.skipCheckPIN2andCVCIfValidatedByIssuer
            skipSecurityDelayIfValidatedByLinkedTerminal = from.skipSecurityDelayIfValidatedByLinkedTerminal
            restrictOverwriteIssuerDataEx = from.restrictOverwriteIssuerDataEx
            requireTerminalTxSignature = from.requireTerminalTxSignature
            requireTerminalCertSignature = from.requireTerminalCertSignature
            checkPIN3onCard = from.checkPIN3onCard
            createWallet = if (from.createWallet) 1 else 0
            issuerData = from.issuerData
            cardData = from.cardData
//            numberFormat = from.numberFormat
        }

        fillSigningMethod(jsonDto, from)
        fillNdef(jsonDto, from)
        return jsonDto
    }

    private fun fillSigningMethod(jsonDto: PersonalizationJson, from: PersonalizationConfig) {
        jsonDto.SigningMethod = PersonalizationConfig.makeSigningMethodMask(from).rawValue.toLong()
    }

    private fun fillNdef(jsonDto: PersonalizationJson, from: PersonalizationConfig) {
        fun add(value: String, type: NdefRecord.Type) {
            if (value.isNotEmpty()) jsonDto.ndef.add(NdefRecord(type, value))
        }

        if (from.aarCustom.isNotEmpty()) add(from.aarCustom, NdefRecord.Type.AAR)
        else add(from.aar, NdefRecord.Type.AAR)

        add(from.uri, NdefRecord.Type.URI)
    }
}