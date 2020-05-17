package com.tangem.devkit.ucase.variants.personalize.converter

import com.tangem.commands.EllipticCurve
import com.tangem.commands.SigningMethod
import com.tangem.commands.SigningMethodMask
import com.tangem.commands.personalization.entities.NdefRecord
import com.tangem.devkit._arch.structure.abstraction.TwoWayConverter
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

    override fun convert(jsonDto: PersonalizationJson): PersonalizationConfig {
        val config = PersonalizationConfig.default()
        config.apply {
            series = jsonDto.series
            startNumber = jsonDto.startNumber
            curveID = EllipticCurve.byName(jsonDto.curveID)?.curve ?: ""
            blockchainCustom = ""
            MaxSignatures = jsonDto.MaxSignatures
            createWallet = jsonDto.createWallet != 0L
            hexCrExKey = jsonDto.hexCrExKey
            requireTerminalTxSignature = jsonDto.requireTerminalTxSignature
            requireTerminalCertSignature = jsonDto.requireTerminalCertSignature
            checkPIN3onCard = jsonDto.checkPIN3onCard
            isReusable = jsonDto.isReusable
            useActivation = jsonDto.useActivation
            forbidPurgeWallet = jsonDto.forbidPurgeWallet
            allowSelectBlockchain = jsonDto.allowSelectBlockchain
            useBlock = jsonDto.useBlock
            useOneCommandAtTime = jsonDto.useOneCommandAtTime
            useCVC = jsonDto.useCVC
            allowSwapPIN = jsonDto.allowSwapPIN
            allowSwapPIN2 = jsonDto.allowSwapPIN2
            forbidDefaultPIN = jsonDto.forbidDefaultPIN
            smartSecurityDelay = jsonDto.smartSecurityDelay
            protectIssuerDataAgainstReplay = jsonDto.protectIssuerDataAgainstReplay
            skipSecurityDelayIfValidatedByIssuer = jsonDto.skipSecurityDelayIfValidatedByIssuer
            skipCheckPIN2andCVCIfValidatedByIssuer = jsonDto.skipCheckPIN2andCVCIfValidatedByIssuer
            skipSecurityDelayIfValidatedByLinkedTerminal = jsonDto.skipSecurityDelayIfValidatedByLinkedTerminal
            restrictOverwriteIssuerDataEx = jsonDto.restrictOverwriteIssuerDataEx
            protocolAllowUnencrypted = jsonDto.protocolAllowUnencrypted
            protocolAllowStaticEncryption = jsonDto.protocolAllowStaticEncryption
            useNDEF = jsonDto.useNDEF
            useDynamicNDEF = jsonDto.useDynamicNDEF
            disablePrecomputedNDEF = jsonDto.disablePrecomputedNDEF
            PIN = jsonDto.PIN
            PIN2 = jsonDto.PIN2
            PIN3 = jsonDto.PIN3
            CVC = jsonDto.CVC
            pauseBeforePIN2 = jsonDto.pauseBeforePIN2
            count = jsonDto.count
            issuerName = jsonDto.issuerName
            issuerData = jsonDto.issuerData
//            numberFormat = from.numberFormat
//            pinLessFloorLimit = 100000L
        }

        fillCardData(jsonDto, config)
        fillSigningMethod(jsonDto, config)
        fillNdef(jsonDto, config)

        return config
    }

    private fun fillCardData(jsonDto: PersonalizationJson, config: PersonalizationConfig) {
        val jsonCardData = jsonDto.cardData

        // copy whole object and then checking tricky places
        config.cardData.copyFrom(jsonCardData)

        config.itsToken = jsonCardData.token_contract_address?.isNotEmpty() ?: false
                || jsonCardData.token_symbol?.isNotEmpty() ?: false

        val selectedBlockchain = Helper.listOfBlockchain().firstOrNull { it.value == jsonCardData.blockchain }
        if (selectedBlockchain == null) {
            config.cardData.blockchain = PersonalizationJson.CUSTOM
            config.blockchainCustom = jsonCardData.blockchain
        }
    }

    private fun fillSigningMethod(jsonDto: PersonalizationJson, config: PersonalizationConfig) {
        val mask = SigningMethodMask(jsonDto.SigningMethod.toInt())
        if (mask.contains(SigningMethod.SignHash)) config.SigningMethod0 = true
        if (mask.contains(SigningMethod.SignRaw)) config.SigningMethod1 = true
        if (mask.contains(SigningMethod.SignHashValidateByIssuer)) config.SigningMethod2 = true
        if (mask.contains(SigningMethod.SignRawValidateByIssuer)) config.SigningMethod3 = true
        if (mask.contains(SigningMethod.SignHashValidateByIssuerWriteIssuerData)) config.SigningMethod4 = true
        if (mask.contains(SigningMethod.SignRawValidateByIssuerWriteIssuerData)) config.SigningMethod5 = true
    }

    private fun fillNdef(jsonDto: PersonalizationJson, config: PersonalizationConfig) {
        jsonDto.ndef.forEach { record ->
            when (record.type) {
                NdefRecord.Type.URI -> config.uri = record.value
                NdefRecord.Type.AAR -> config.aar = record.value
                NdefRecord.Type.TEXT -> {
                }
            }
        }
        val selectedAar = Helper.aarList().firstOrNull { it.value == config.aar }
        if (selectedAar == null) {
            config.aarCustom = config.aar
            config.aar = ""
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
//            numberFormat = from.numberFormat
//            pinLessFloorLimit = 100000L
        }

        fillCardData(from, jsonDto)
        fillSigningMethod(from, jsonDto)
        fillNdef(from, jsonDto)
        return jsonDto
    }

    private fun fillCardData(config: PersonalizationConfig, jsonDto: PersonalizationJson) {
        jsonDto.cardData = config.cardData
        if (config.cardData.blockchain == PersonalizationJson.CUSTOM) {
            jsonDto.cardData.blockchain = config.blockchainCustom
        }
        if (config.itsToken) {
            jsonDto.cardData.token_contract_address = config.cardData.token_contract_address
            jsonDto.cardData.token_symbol = config.cardData.token_symbol
            jsonDto.cardData.token_decimal = config.cardData.token_decimal
        } else {
            jsonDto.cardData.token_contract_address = null
            jsonDto.cardData.token_symbol = null
            jsonDto.cardData.token_decimal = null
        }
    }

    private fun fillSigningMethod(config: PersonalizationConfig, jsonDto: PersonalizationJson) {
        jsonDto.SigningMethod = PersonalizationConfig.makeSigningMethodMask(config).rawValue.toLong()
    }

    private fun fillNdef(config: PersonalizationConfig, jsonDto: PersonalizationJson) {
        fun add(value: String, type: NdefRecord.Type) {
            if (value.isNotEmpty()) jsonDto.ndef.add(NdefRecord(type, value))
        }

        if (config.aarCustom.isNotEmpty()) add(config.aarCustom, NdefRecord.Type.AAR)
        else add(config.aar, NdefRecord.Type.AAR)

        add(config.uri, NdefRecord.Type.URI)
    }
}