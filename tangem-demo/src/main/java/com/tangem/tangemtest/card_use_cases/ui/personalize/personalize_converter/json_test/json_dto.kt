package com.tangem.tangemtest.card_use_cases.ui.personalize.personalize_converter.json_test

class TestJsonDto {

    // Card number
    val series = "DB"
    val startNumber = 300000000000


    // Common
    val curveID = "ed25519"
    val blockchain = Blockchain()                       // новое поле
    val MaxSignatures = 1000000
    val createWallet = 1
    val createWalletB = createWallet == 1


    // Signing method
    val SigningMethod0 = false
    val SigningMethod1 = false
    val SigningMethod2 = false
    val SigningMethod3 = false
    val SigningMethod4 = false
    val SigningMethod5 = false
    val SigningMethod6 = false


    // Sign hash external properties
    val pinLessFloorLimit = 0                           // новое поле
    val hexCrExKey = ""
    val requireTerminalTxSignature = false
    val requireTerminalCertSignature = false
    val checkPIN3onCard = false


    // Denomination
    val writeOnPersonalization = false                  // новое поле
    val denomination = 0                                // новое поле


    // Token
    val itsToken = true                                 // новое поле
    val symbol = "token symbol"                         // новое поле
    val contractAddress = "contact adress"              // новое поле
    val decimal = 1                                     // новое поле


    val cardData = CardData()


    // Settings mask
    val isReusable = true
    val useActivation = false
    val forbidPurgeWallet = false
    val allowSelectBlockchain = false
    val useBlock = false
    val useOneCommandAtTime = false
    val useCVC = false
    val allowSwapPIN = false
    val allowSwapPIN2 = true
    val forbidDefaultPIN = false
    val smartSecurityDelay = true
    val protectIssuerDataAgainstReplay = true
    val skipSecurityDelayIfValidatedByIssuer = false
    val skipCheckPIN2andCVCIfValidatedByIssuer = false
    val skipSecurityDelayIfValidatedByLinkedTerminal = true
    val restrictOverwriteIssuerDataEx = false


    // Settings mask - protocol encryption
    val protocolAllowUnencrypted = true
    val protocolAllowStaticEncryption = true


    val useNDEF = true
    val useDynamicNDEF = true
    val disablePrecomputedNDEF = false
    val NDEF: MutableList<Ndef> = mutableListOf(Ndef("AAR", "com.tangem.wallet")) // aar, customAAR package


    // Pins
    val PIN = "000000"
    val PIN2 = "000"
    val PIN3 = ""
    val CVC = "000"
    val pauseBeforePIN2 = 15000


    // not used
    val count = 20
    val numberFormat = ""
    val issuerData = null
    val releaseVersion = false
    val issuerName = "TANGEM SDK"
}

class CardData {
    val date = "2020-02-17"
    val batch = "FF87"
    val blockchain = "IROHA"
    val product_note = true
    val product_tag = false
    val product_id_card = false
}

class Blockchain {
    val customName: String? = null
    val name = "BTC"
}

class Ndef(val type: String, val value: String)