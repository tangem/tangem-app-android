package com.tangem.commands.personalization.entities

import com.tangem.commands.CardData
import com.tangem.commands.EllipticCurve
import com.tangem.commands.SigningMethod

data class NdefRecord(
        val type: Type,
        val value: String
) {
    enum class Type {
        URI, AAR, TEXT
    }

    val valueInBytes: ByteArray by lazy { value.toByteArray() }
}

/**
 * It is a configuration file with all the card settings that are written on the card
 * during [PersonalizeCommand].
 */
data class CardConfig(
        val issuerName: String? = null,
        val acquirerName: String? = null,
        val series: String? = null,
        val startNumber: Long = 0,
        val count: Int = 0,
        val pin: String,
        val pin2: String,
        val pin3: String,
        val hexCrExKey: String?,
        val cvc: String,
        val pauseBeforePin2: Int,
        val smartSecurityDelay: Boolean,
        val curveID: EllipticCurve,
        val signingMethod: SigningMethod,
        val maxSignatures: Int,
        val isReusable: Boolean,
        val allowSwapPin: Boolean,
        val allowSwapPin2: Boolean,
        val useActivation: Boolean,
        val useCvc: Boolean,
        val useNdef: Boolean,
        val useDynamicNdef: Boolean,
        val useOneCommandAtTime: Boolean,
        val useBlock: Boolean,
        val allowSelectBlockchain: Boolean,
        val forbidPurgeWallet: Boolean,
        val protocolAllowUnencrypted: Boolean,
        val protocolAllowStaticEncryption: Boolean,
        val protectIssuerDataAgainstReplay: Boolean,
        val forbidDefaultPin: Boolean,
        val disablePrecomputedNdef: Boolean,
        val skipSecurityDelayIfValidatedByIssuer: Boolean,
        val skipCheckPIN2andCVCIfValidatedByIssuer: Boolean,
        val skipSecurityDelayIfValidatedByLinkedTerminal: Boolean,

        val restrictOverwriteIssuerDataEx: Boolean,

        val requireTerminalTxSignature: Boolean,
        val requireTerminalCertSignature: Boolean,
        val checkPin3onCard: Boolean,

        val createWallet: Boolean,

        val cardData: CardData,
        val ndefRecords: List<NdefRecord>
) {
    companion object
}