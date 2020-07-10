package com.tangem.commands.personalization.entities

import com.tangem.commands.CardData
import com.tangem.commands.EllipticCurve
import com.tangem.commands.SigningMethodMask
import com.tangem.common.extensions.calculateSha256

data class NdefRecord(
        val type: Type,
        val value: String
) {
    enum class Type {
        URI, AAR, TEXT
    }

    @delegate:Transient
    val valueInBytes: ByteArray by lazy { value.toByteArray() }
}

/**
 * It is a configuration file with all the card settings that are written on the card
 * during [PersonalizeCommand].
 */
class CardConfig(
        val issuerName: String? = null,
        val acquirerName: String? = null,
        val series: String? = null,
        val startNumber: Long = 0,
        val count: Int = 0,
        pin: String,
        pin2: String,
        pin3: String,
        val hexCrExKey: String?,
        val cvc: String,
        val pauseBeforePin2: Int,
        val smartSecurityDelay: Boolean,
        val curveID: EllipticCurve,
        val signingMethods: SigningMethodMask,
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

    val pin: ByteArray = pin.calculateSha256()
    val pin2: ByteArray = pin2.calculateSha256()
    val pin3: ByteArray = pin3.calculateSha256()

    companion object
}