package com.tangem.tangemtest.ucase.variants.personalize.dto

/**
[REDACTED_AUTHOR]
 */
class PersonalizeConfig {

    // Card number
    var series = "DB"
    var startNumber: Long = 300000000000


    // Common
    var curveID = "ed25519"
    var blockchain = "btc"
    var blockchainCustom = "custom"
    var MaxSignatures: Long = 1000000
    var createWallet = true

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
    var itsToken = true
    var symbol = "token symbol"
    var contractAddress = "contact adress"
    var decimal: Long = 1


    var cardData = CardData()


    // Settings mask
    var isReusable = true
    var useActivation = false
    var forbidPurgeWallet = false
    var allowSelectBlockchain = false
    var useBlock = false
    var useOneCommandAtTime = false
    var useCVC = false
    var allowSwapPIN = false
    var allowSwapPIN2 = true
    var forbidDefaultPIN = false
    var smartSecurityDelay = true
    var protectIssuerDataAgainstReplay = true
    var skipSecurityDelayIfValidatedByIssuer = false
    var skipCheckPIN2andCVCIfValidatedByIssuer = false
    var skipSecurityDelayIfValidatedByLinkedTerminal = true
    var restrictOverwriteIssuerDataEx = false


    // Settings mask - protocol encryption
    var protocolAllowUnencrypted = true
    var allowFastEncryption = false
    var protocolAllowStaticEncryption = true


    var useNDEF = true
    var useDynamicNDEF = true
    var disablePrecomputedNDEF = false
    var NDEF = "com.tangem.wallet"


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