package com.tangem.tangemtest.extensions

import android.app.Application
import android.content.Context
import com.tangem.commands.*
import com.tangem.commands.personalization.CardConfig
import com.tangem.commands.personalization.NdefRecord
import java.util.*

fun CardConfig.Companion.create(application: Application): CardConfig {

    val preferences = application.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    val signingMethod = SigningMethod.build(
            signHash = preferences.getBoolean("personalization_SigningMethod_0", false),
            signRaw = preferences.getBoolean("personalization_SigningMethod_1", false),
            signHashValidatedByIssuer = preferences.getBoolean("personalization_SigningMethod_2", false),
            signRawValidatedByIssuer = preferences.getBoolean("personalization_SigningMethod_3", false),
            signHashValidatedByIssuerAndWriteIssuerData = preferences.getBoolean("personalization_SigningMethod_4", false),
            signRawValidatedByIssuerAndWriteIssuerData = preferences.getBoolean("personalization_SigningMethod_5", false),
            signPos = preferences.getBoolean("personalization_SigningMethod_6", false)
    )

    val isNote = preferences.getBoolean("personalization_ProductMask_IsNote", true)
    val isTag = preferences.getBoolean("personalization_ProductMask_IsTag", false)
    val isIdCard = preferences.getBoolean("personalization_ProductMask_IsIDCard", false)

    val productMaskBuilder = ProductMaskBuilder()
    if (isNote) productMaskBuilder.add(ProductMask.note)
    if (isTag) productMaskBuilder.add(ProductMask.tag)
    if (isIdCard) productMaskBuilder.add(ProductMask.idCard)
    val productMask = productMaskBuilder.build()

    var tokenSymbol: String? = null
    var tokenContractAddress: String? = null
    var tokenDecimal: Int? = null
    if (preferences.getBoolean("personalization_isToken", false)) {
        tokenSymbol = preferences.getString("personalization_token_symbol", "")
        tokenContractAddress = preferences.getString("personalization_token_contract_address", "")
        tokenDecimal = preferences.getString("personalization_token_decimal", "")!!.toInt()
    }

    val cardData = CardData(
            blockchainName = preferences.getString("personalization_Blockchain", "BTC"),
            batchId = preferences.getString("personalization_card_batch", "FFFF"),
            productMask = productMask,
            tokenSymbol = tokenSymbol,
            tokenContractAddress = tokenContractAddress,
            tokenDecimal = tokenDecimal,
            issuerName = null,
            manufactureDateTime = Calendar.getInstance().time,
            manufacturerSignature = null)


    val ndefAar = preferences.getString("personalization_NDEF_AAR", "Release APP")
    val ndefUri = preferences.getString("personalization_NDEF_URI", "https://tangem.com")

    val ndefs = mutableListOf<NdefRecord>()
    if (!ndefUri.isNullOrEmpty()) {
        ndefs.add(NdefRecord(NdefRecord.Type.URI, ndefUri))
    }
    if (ndefAar != "None") {
        val type = NdefRecord.Type.AAR
        val value = when (ndefAar) {
            "Debug APP" -> {
                "com.tangem.wallet.debug"
            }
            "Release APP" -> {
                "com.tangem.wallet"
            }
            "--- CUSTOM ---" -> {
                preferences.getString("personalization_NDEF_CUSTOM_AAR", "com.tangem.wallet")!!
            }
            else -> ""
        }
        ndefs.add(NdefRecord(type, value))
    }

    return CardConfig(
            cardData = cardData,
            curveID = EllipticCurve.byName(preferences.getString("personalization_CurveId", "secp256k1")!!)
                    ?: EllipticCurve.Secp256k1,
            signingMethod = signingMethod,
            createWallet = preferences.getBoolean("personalization_CreateWallet", true),
            maxSignatures = preferences.getString("personalization_MaxSignatures", "1000")!!.toInt(),
            isReusable = preferences.getBoolean("personalization_SettingsMask_IsReusable", true),
            protocolAllowUnencrypted = preferences.getBoolean("personalization_SettingsMask_AllowEncryption_None", true),
            protocolAllowStaticEncryption = preferences.getBoolean("personalization_SettingsMask_AllowEncryption_Fast", true),
            useActivation = preferences.getBoolean("personalization_SettingsMask_NeedActivation", false),

            useOneCommandAtTime = preferences.getBoolean("personalization_SettingsMask_OneApduAtOnce", false),
            useCvc = preferences.getBoolean("personalization_SettingsMask_UseCVC", false),
            useBlock = preferences.getBoolean("personalization_SettingsMask_UseBlock", false),
            allowSwapPin = preferences.getBoolean("personalization_SettingsMask_AllowSwapPIN", true),
            allowSwapPin2 = preferences.getBoolean("personalization_SettingsMask_AllowSwapPIN2", true),
            useNdef = preferences.getBoolean("personalization_SettingsMask_UseNDEF", true),
            useDynamicNdef = preferences.getBoolean("personalization_SettingsMask_UseDynamicNDEF", true),
            protectIssuerDataAgainstReplay = preferences.getBoolean("personalization_SettingsMask_ProtectIssuerDataAgainstReplay", true),
            forbidDefaultPin = preferences.getBoolean("personalization_SettingsMask_ForbidDefaultPIN", false),
            smartSecurityDelay = preferences.getBoolean("personalization_SettingsMask_SmartSecurityDelay", false),
            pauseBeforePin2 = preferences.getString("personalization_PauseBeforePIN2", "15")!!.toInt() * 1000,
            allowSelectBlockchain = preferences.getBoolean("personalization_SettingsMask_AllowSelectBlockchain", false),
            forbidPurgeWallet = preferences.getBoolean("personalization_SettingsMask_ForbidPurgeWallet", false)
                    ?: false,
            disablePrecomputedNdef = preferences.getBoolean("personalization_SettingsMask_DisablePrecomputedNDEF", false)
                    ?: false,
            skipSecurityDelayIfValidatedByIssuer = preferences.getBoolean("personalization_SettingsMask_SkipSecurityDelayIfValidatedByIssuer", true),
            skipCheckPIN2andCVCIfValidatedByIssuer = preferences.getBoolean("personalization_SettingsMask_SkipCheckPIN2andCVCIfValidatedByIssuer", true),

            skipSecurityDelayIfValidatedByLinkedTerminal = preferences.getBoolean("personalization_SettingsMask_SkipSecurityDelayIfValidatedByLinkedTerminal", true),
            restrictOverwriteIssuerDataEx = preferences.getBoolean("personalization_SettingsMask_RestrictOverwriteIssuerDataEx", true),

            requireTerminalTxSignature = preferences.getBoolean("personalization_SettingsMask_RequireTerminalTxSignature", false),
            requireTerminalCertSignature = preferences.getBoolean("personalization_SettingsMask_RequireTerminalCertSignature", false),
            checkPin3onCard = preferences.getBoolean("personalization_SettingsMask_CheckPIN3onCard", true),

            cvc = preferences.getString("personalization_cvc", "000") ?: "000",
            pin = preferences.getString("personalization_pin", "000000") ?: "000000",
            pin2 = preferences.getString("personalization_pin2", "000") ?: "000",
            pin3 = preferences.getString("personalization_pin3", "123") ?: "123",
            hexCrExKey = preferences.getString("personalization_CrEx_Key", "00112233445566778899AABBCCDDEEFFFFEEDDCCBBAA998877665544332211000000111122223333444455556666777788889999AAAABBBBCCCCDDDDEEEEFFFF"),

            ndefRecords = ndefs
    )
}