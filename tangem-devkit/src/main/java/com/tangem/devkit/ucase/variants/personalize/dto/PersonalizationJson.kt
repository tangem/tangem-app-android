package com.tangem.devkit.ucase.variants.personalize.dto

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tangem.commands.personalization.entities.NdefRecord

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