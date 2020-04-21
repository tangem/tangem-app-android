package com.tangem.tangemtest.ucase.variants.personalize.dto

import com.tangem.commands.EllipticCurve

/**
[REDACTED_AUTHOR]
 */
class PersonalizationConfig {

    // Card number
    var series = ""
    var startNumber: Long = 0
    var batchId = ""

    // Common
    var curveID = ""
    var blockchain = ""
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

    // Denomination
    var writeOnPersonalization = false
    var denomination: Long = 0

    // Token
    var itsToken = false
    var symbol = ""
    var contractAddress = ""
    var decimal: Long = 0

    var cardData = CardData()

    // Settings mask
    var isReusable = false
    var useActivation = false
    var forbidPurgeWallet = false
    var allowSelectBlockchain = false
    var useBlock = false
    var oneApdu = false
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

    companion object {
        fun default(): PersonalizationConfig {
            return PersonalizationConfig().apply {
                // Card number
                series = "BB"
                startNumber = 300000000000L
                batchId = "ffff"

                // Common
                curveID = EllipticCurve.Secp256k1.curve
                blockchain = "ETH"
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

                // Denomination
                writeOnPersonalization = false
                denomination = 1000000L

                // Token
                itsToken = false
                symbol = ""
                contractAddress = ""
                decimal = 0L

                cardData = CardData()

                // Settings mask
                isReusable = true
                useActivation = false
                forbidPurgeWallet = false
                allowSelectBlockchain = false
                useBlock = false
                oneApdu = false
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
            }
        }
    }
}

class CardData {
    var date = "2020-02-17"
    var batch = "FF87"
    var blockchain = "IROHA"
    var product_note = true
    var product_tag = false
    var product_id_card = false
    var product_id_issuer = false
}