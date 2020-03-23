package com.tangem.commands.personalization

import com.tangem.commands.*

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

    fun getSettingsMask(): SettingsMask {
        val builder = SettingsMaskBuilder()

        if (allowSwapPin) builder.add(Settings.AllowSwapPIN)
        if (allowSwapPin2) builder.add(Settings.AllowSwapPIN2)
        if (useCvc) builder.add(Settings.UseCVC)
        if (isReusable) builder.add(Settings.IsReusable)

        if (useOneCommandAtTime) builder.add(Settings.UseOneCommandAtTime)
        if (useNdef) builder.add(Settings.UseNdef)
        if (useDynamicNdef) builder.add(Settings.UseDynamicNdef)
        if (disablePrecomputedNdef) builder.add(Settings.DisablePrecomputedNdef)

        if (protocolAllowUnencrypted) builder.add(Settings.ProtocolAllowUnencrypted)
        if (protocolAllowStaticEncryption) builder.add(Settings.ProtocolAllowStaticEncryption)

        if (forbidDefaultPin) builder.add(Settings.ForbidDefaultPIN)

        if (useActivation) builder.add(Settings.UseActivation)

        if (useBlock) builder.add(Settings.UseBlock)
        if (smartSecurityDelay) builder.add(Settings.SmartSecurityDelay)

        if (protectIssuerDataAgainstReplay) builder.add(Settings.ProtectIssuerDataAgainstReplay)

        if (forbidPurgeWallet) builder.add(Settings.ForbidPurgeWallet)
        if (allowSelectBlockchain) builder.add(Settings.AllowSelectBlockchain)

        if (skipCheckPIN2andCVCIfValidatedByIssuer) builder.add(Settings.SkipCheckPin2andCvcIfValidatedByIssuer)
        if (skipSecurityDelayIfValidatedByIssuer) builder.add(Settings.SkipSecurityDelayIfValidatedByIssuer)

        if (skipSecurityDelayIfValidatedByLinkedTerminal) builder.add(Settings.SkipSecurityDelayIfValidatedByLinkedTerminal)
        if (restrictOverwriteIssuerDataEx) builder.add(Settings.RestrictOverwriteIssuerDataEx)

        if (requireTerminalTxSignature) builder.add(Settings.RequireTermTxSignature)

        if (requireTerminalCertSignature) builder.add(Settings.RequireTermCertSignature)

        if (checkPin3onCard) builder.add(Settings.CheckPIN3onCard)

        return builder.build()
    }

    companion object
}