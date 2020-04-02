package com.tangem.tangemtest.ucase.variants.personalize.dto

/**
[REDACTED_AUTHOR]
 */
class PersonalizeConfig {

    // Card number
    var series = "BB"
    var startNumber: Long = 300000000000
    var batchId = "ffff"


    // Common
    var curveID = "ed25519"
    var blockchain = "BTC"
    var blockchainCustom = ""
    var MaxSignatures: Long = 100
    var createWallet = false

    // Signing method
    var SigningMethod0 = true
    var SigningMethod1 = false
    var SigningMethod2 = false
    var SigningMethod3 = false
    var SigningMethod4 = false
    var SigningMethod5 = false
    var SigningMethod6 = false


    // Sign hash external properties
    var pinLessFloorLimit: Long = 100000
    var hexCrExKey = "00112233445566778899AABBCCDDEEFFFFEEDDCCBBAA998877665544332211000000111122223333444455556666777788889999AAAABBBBCCCCDDDDEEEEFFFF"
    var requireTerminalTxSignature = false
    var requireTerminalCertSignature = false
    var checkPIN3onCard = true


    // Denomination
    var writeOnPersonalization = false
    var denomination: Long = 1000000


    // Token
    var itsToken = false
    var symbol = ""
    var contractAddress = ""
    var decimal: Long = 0


    var cardData = CardData()


    // Settings mask
    var isReusable = true
    var useActivation = false
    var forbidPurgeWallet = false
    var allowSelectBlockchain = false
    var useBlock = false
    var oneApdu = false
    var useCVC = false
    var allowSwapPIN = true
    var allowSwapPIN2 = true
    var forbidDefaultPIN = false
    var smartSecurityDelay = false
    var protectIssuerDataAgainstReplay = true
    var skipSecurityDelayIfValidatedByIssuer = true
    var skipCheckPIN2andCVCIfValidatedByIssuer = true
    var skipSecurityDelayIfValidatedByLinkedTerminal = true
    var restrictOverwriteIssuerDataEx = false


    // Settings mask - protocol encryption
    var protocolAllowUnencrypted = true
    var allowFastEncryption = false
    var protocolAllowStaticEncryption = true


    var useNDEF = true
    var useDynamicNDEF = true
    var disablePrecomputedNDEF = false
    var aar = "com.tangem.wallet"
    var aarCustom = ""


    // Pins
    var PIN = "000000"
    var PIN2 = "000"
    var PIN3 = ""
    var CVC = "000"
    var pauseBeforePIN2: Long = 15000


    // not used
//    var count: Long = 20
//    var numberFormat = ""
//    var issuerData = null
//    var releaseVersion = false
//    var issuerName = "TANGEM SDK"
}

class CardData {
    var date = "2020-02-17"
    var batch = "FF87"
    var blockchain = "IROHA"
    var product_note = true
    var product_tag = false
    var product_id_card = false
}