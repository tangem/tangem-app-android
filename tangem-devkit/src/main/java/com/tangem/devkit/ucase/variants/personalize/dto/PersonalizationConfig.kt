package com.tangem.devkit.ucase.variants.personalize.dto

import com.tangem.commands.EllipticCurve
import com.tangem.commands.SigningMethod
import com.tangem.commands.SigningMethodMask
import com.tangem.commands.SigningMethodMaskBuilder

/**
[REDACTED_AUTHOR]
 */
class PersonalizationConfig {

    // Card number
    var series = ""
    var startNumber: Long = 0

    // Common
    var curveID = ""
    var blockchainCustom = ""
    var MaxSignatures: Long = 0
    var createWallet = false

    // Signing method
    var SigningMethod0 = false
    var SigningMethod1 = false
    var SigningMethod2 = false
    var SigningMethod3 = false
    var SigningMethod4 = false
    var SigningMethod5 = false
    var SigningMethod6 = false

    // Sign hash external properties
    var pinLessFloorLimit: Long = 0
    var hexCrExKey = ""
    var requireTerminalTxSignature = false
    var requireTerminalCertSignature = false
    var checkPIN3onCard = false

    // Token
    var itsToken = false

    // Product mask
    var cardData = CardData()

    // Settings mask
    var isReusable = false
    var useActivation = false
    var forbidPurgeWallet = false
    var allowSelectBlockchain = false
    var useBlock = false
    var useOneCommandAtTime = false
    var useCVC = false
    var allowSwapPIN = false
    var allowSwapPIN2 = false
    var forbidDefaultPIN = false
    var smartSecurityDelay = false
    var protectIssuerDataAgainstReplay = false
    var skipSecurityDelayIfValidatedByIssuer = false
    var skipCheckPIN2andCVCIfValidatedByIssuer = false
    var skipSecurityDelayIfValidatedByLinkedTerminal = false
    var restrictOverwriteIssuerDataEx = false

    // Settings mask - protocol encryption
    var protocolAllowUnencrypted = false
    var protocolAllowStaticEncryption = false

    // Settings mask
    var useNDEF = false
    var useDynamicNDEF = false
    var disablePrecomputedNDEF = false
    var aar = ""
    var aarCustom = ""
    var uri = ""

    // Pins
    var PIN = ""
    var PIN2 = ""
    var PIN3 = ""
    var CVC = ""
    var pauseBeforePIN2: Long = 0

    var count: Long = 0
    var issuerName = ""
    var issuerData = null

    companion object {

        fun default(): PersonalizationConfig {
            return PersonalizationConfig().apply {
                // Card number
                series = "BB"
                startNumber = 300000000000L

                // Common
                curveID = EllipticCurve.Secp256k1.curve
                blockchainCustom = ""
                MaxSignatures = 999999L
                createWallet = true

                // Signing method
                SigningMethod0 = true
                SigningMethod1 = false
                SigningMethod2 = false
                SigningMethod3 = false
                SigningMethod4 = false
                SigningMethod5 = false
                SigningMethod6 = false

                // Sign hash external properties
                pinLessFloorLimit = 100000L
                hexCrExKey = "00112233445566778899AABBCCDDEEFFFFEEDDCCBBAA998877665544332211000000111122223333444455556666777788889999AAAABBBBCCCCDDDDEEEEFFFF"
                requireTerminalTxSignature = false
                requireTerminalCertSignature = false
                checkPIN3onCard = true

                // Token
                itsToken = false

                cardData = CardData.default()

                // Settings mask
                isReusable = true
                useActivation = false
                forbidPurgeWallet = false
                allowSelectBlockchain = false
                useBlock = false
                useOneCommandAtTime = false
                useCVC = false
                allowSwapPIN = true
                allowSwapPIN2 = true
                forbidDefaultPIN = false
                smartSecurityDelay = true
                protectIssuerDataAgainstReplay = true
                skipSecurityDelayIfValidatedByIssuer = true
                skipCheckPIN2andCVCIfValidatedByIssuer = true
                skipSecurityDelayIfValidatedByLinkedTerminal = true
                restrictOverwriteIssuerDataEx = false

                // Settings mask - protocol encryption
                protocolAllowUnencrypted = true
                protocolAllowStaticEncryption = true

                useNDEF = true
                useDynamicNDEF = true
                disablePrecomputedNDEF = false
                aar = "com.tangem.wallet"
                aarCustom = ""
                uri = "https://tangem.com"

                // Pins
                PIN = "000000"
                PIN2 = "000"
                PIN3 = ""
                CVC = "000"
                pauseBeforePIN2 = 5000L

                count = 1050
                issuerName = "TANGEM"
                issuerData = null
            }
        }

        fun makeSigningMethodMask(from: PersonalizationConfig): SigningMethodMask {
            val signingMethodMaskBuilder = SigningMethodMaskBuilder()
            if (from.SigningMethod0) {
                signingMethodMaskBuilder.add(SigningMethod.SignHash)
            }
            if (from.SigningMethod1) {
                signingMethodMaskBuilder.add(SigningMethod.SignRaw)
            }
            if (from.SigningMethod2) {
                signingMethodMaskBuilder.add(SigningMethod.SignHashValidateByIssuer)
            }
            if (from.SigningMethod3) {
                signingMethodMaskBuilder.add(SigningMethod.SignRawValidateByIssuer)
            }
            if (from.SigningMethod4) {
                signingMethodMaskBuilder.add(SigningMethod.SignHashValidateByIssuerWriteIssuerData)
            }
            if (from.SigningMethod5) {
                signingMethodMaskBuilder.add(SigningMethod.SignRawValidateByIssuerWriteIssuerData)
            }
            if (from.SigningMethod6) {
                signingMethodMaskBuilder.add(SigningMethod.SignHash)
            }
            return signingMethodMaskBuilder.build()
        }
    }
}

class CardData {
    var date = ""
    var batch = ""
    var blockchain = ""
    var token_symbol: String? = null
    var token_contract_address: String? = null
    var token_decimal: Long? = null
    var product_note = false
    var product_tag = false
    var product_id_card = false
    var product_id_issuer = false

    companion object {
        fun default(): CardData {
            return CardData().apply {
                date = "2020-02-17"
                batch = "FFFF"
                blockchain = "ETH"
                token_symbol = ""
                token_contract_address = ""
                token_decimal = 0
                product_note = true
                product_tag = false
                product_id_card = false
                product_id_issuer = false
            }
        }
    }
}