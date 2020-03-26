package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.*
import com.tangem.tangemtest._arch.structure.impl.KeyValue
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
class Value(
        private var value: Any? = null,
        val list: List<KeyValue>? = mutableListOf()
) {
    fun get(): Any? = value
    fun set(newValue: Any?) {
        value = newValue
    }
}

class IdToValueAssociations : BaseTypedHolder<Id, Value>() {

    // All registered association values can't be objects
    fun init(from: PersonalizeConfig) {
        register(CardNumber.SERIES, Value(from.series))
        register(CardNumber.NUMBER, Value(from.startNumber))
        register(Common.CURVE, Value(from.curveID, Helper.listOfCurves()))
        register(Common.BLOCKCHAIN, Value(from.blockchain, Helper.listOfBlockchain()))
        register(Common.BLOCKCHAIN_CUSTOM, Value(""))
        register(Common.MAX_SIGNATURES, Value(from.MaxSignatures))
        register(Common.CREATE_WALLET, Value(from.createWallet))
        register(SigningMethod.SIGN_TX, Value(from.SigningMethod0))
        register(SigningMethod.SIGN_TX_RAW, Value(from.SigningMethod1))
        register(SigningMethod.SIGN_VALIDATED_TX, Value(from.SigningMethod2))
        register(SigningMethod.SIGN_VALIDATED_TX_RAW, Value(from.SigningMethod3))
        register(SigningMethod.SIGN_VALIDATED_TX_ISSUER, Value(from.SigningMethod4))
        register(SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER, Value(from.SigningMethod5))
        register(SigningMethod.SIGN_EXTERNAL, Value(from.SigningMethod6))
        register(SignHashExProp.PIN_LESS_FLOOR_LIMIT, Value(from.pinLessFloorLimit))
        register(SignHashExProp.CRYPTO_EXTRACT_KEY, Value(from.hexCrExKey))
        register(SignHashExProp.REQUIRE_TERMINAL_CERT_SIG, Value(from.requireTerminalCertSignature))
        register(SignHashExProp.REQUIRE_TERMINAL_TX_SIG, Value(from.requireTerminalTxSignature))
        register(SignHashExProp.CHECK_PIN3, Value(from.checkPIN3onCard))
        register(Denomination.WRITE_ON_PERSONALIZE, Value(from.writeOnPersonalization))
        register(Denomination.DENOMINATION, Value(from.denomination))
        register(Token.ITS_TOKEN, Value(from.itsToken))
        register(Token.SYMBOL, Value(from.symbol))
        register(Token.CONTRACT_ADDRESS, Value(from.contractAddress))
        register(Token.DECIMAL, Value(from.decimal))
        register(ProductMask.NOTE, Value(from.cardData.product_note))
        register(ProductMask.TAG, Value(from.cardData.product_tag))
        register(ProductMask.ID_CARD, Value(from.cardData.product_id_card))
        register(SettingsMask.IS_REUSABLE, Value(from.isReusable))
        register(SettingsMask.NEED_ACTIVATION, Value(from.useActivation))
        register(SettingsMask.FORBID_PURGE, Value(from.forbidPurgeWallet))
        register(SettingsMask.ALLOW_SELECT_BLOCKCHAIN, Value(from.allowSelectBlockchain))
        register(SettingsMask.USE_BLOCK, Value(from.useBlock))
        register(SettingsMask.ONE_APDU, Value(from.useOneCommandAtTime))
        register(SettingsMask.USE_CVC, Value(from.useCVC))
        register(SettingsMask.ALLOW_SWAP_PIN, Value(from.allowSwapPIN))
        register(SettingsMask.ALLOW_SWAP_PIN2, Value(from.allowSwapPIN2))
        register(SettingsMask.FORBID_DEFAULT_PIN, Value(from.forbidDefaultPIN))
        register(SettingsMask.SMART_SECURITY_DELAY, Value(from.smartSecurityDelay))
        register(SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY, Value(from.protectIssuerDataAgainstReplay))
        register(SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED, Value(from.skipSecurityDelayIfValidatedByIssuer))
        register(SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED, Value(from.skipCheckPIN2andCVCIfValidatedByIssuer))
        register(SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL, Value(from.skipSecurityDelayIfValidatedByLinkedTerminal))
        register(SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA, Value(from.restrictOverwriteIssuerDataEx))
        register(SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED, Value(from.protocolAllowUnencrypted))
        register(SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION, Value(false))
        register(SettingsMaskNdef.USE_NDEF, Value(from.useNDEF))
        register(SettingsMaskNdef.DYNAMIC_NDEF, Value(from.useDynamicNDEF))
        register(SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF, Value(from.disablePrecomputedNDEF))
        register(SettingsMaskNdef.AAR, Value(from.NDEF, Helper.aarList()))
        register(Pins.PIN, Value(from.PIN))
        register(Pins.PIN2, Value(from.PIN2))
        register(Pins.PIN3, Value(from.PIN3))
        register(Pins.CVC, Value(from.CVC))
        register(Pins.PAUSE_BEFORE_PIN2, Value(from.pauseBeforePIN2, Helper.pauseBeforePin()))
    }
}

internal class Helper {
    companion object {
        fun listOfCurves(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("secp256k1", "secp256k1"),
                    KeyValue("ed25519", "ed25519")
            )
        }

        fun listOfBlockchain(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("--- CUSTOM ---", "BTC/test"),
                    KeyValue("BTC", "BTC"),
                    KeyValue("BTC/test", "BTC/test"),
                    KeyValue("ETH", "ETH"),
                    KeyValue("ETH/test", "ETH/test"),
                    KeyValue("BCH", "BCH"),
                    KeyValue("BCH/test", "BCH/test"),
                    KeyValue("LTC", "LTC"),
                    KeyValue("XLM", "XLM"),
                    KeyValue("XLM/test", "XLM/test"),
                    KeyValue("RSK", "RSK"),
                    KeyValue("XPR", "XPR"),
                    KeyValue("CARDANO", "CARDANO")
            )
        }

        fun aarList(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("--- CUSTOM ---", ""),
                    KeyValue("Release APP", "com.some.release"),
                    KeyValue("Debug APP", "com.some.debug"),
                    KeyValue("None", "")
            )
        }

        fun pauseBeforePin(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("immediately", 0L),
                    KeyValue("2 seconds", 2000L),
                    KeyValue("5 seconds", 5000L),
                    KeyValue("15 seconds", 15000L),
                    KeyValue("30 seconds", 30000L),
                    KeyValue("1 minute", 60000L),
                    KeyValue("2 minute", 120000L)
            )
        }
    }
}