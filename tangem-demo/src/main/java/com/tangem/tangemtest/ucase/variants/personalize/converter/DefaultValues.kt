package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.commands.EllipticCurve
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.impl.KeyValue
import com.tangem.tangemtest.ucase.variants.personalize.*
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
class PersonalizeConfigValuesHolder : BaseTypedHolder<Id, Value>() {

    // All registered association values can't be objects
    fun init(default: PersonalizeConfig) {
        register(CardNumber.Series, Value(default.series))
        register(CardNumber.Number, Value(default.startNumber))
        register(CardNumber.BatchId, Value(default.batchId))
        register(Common.Curve, Value(default.curveID, Helper.listOfCurves()))
        register(Common.Blockchain, Value(default.blockchain, Helper.listOfBlockchain()))
        register(Common.BlockchainCustom, Value(""))
        register(Common.MaxSignatures, Value(default.MaxSignatures))
        register(Common.CreateWallet, Value(default.createWallet))
        register(SigningMethod.SignTx, Value(default.SigningMethod0))
        register(SigningMethod.SignTxRaw, Value(default.SigningMethod1))
        register(SigningMethod.SignValidatedTx, Value(default.SigningMethod2))
        register(SigningMethod.SignValidatedTxRaw, Value(default.SigningMethod3))
        register(SigningMethod.SignValidatedTxIssuer, Value(default.SigningMethod4))
        register(SigningMethod.SignValidatedTxRawIssuer, Value(default.SigningMethod5))
        register(SigningMethod.SignExternal, Value(default.SigningMethod6))
        register(SignHashExProp.PinLessFloorLimit, Value(default.pinLessFloorLimit))
        register(SignHashExProp.CryptoExKey, Value(default.hexCrExKey))
        register(SignHashExProp.RequireTerminalCertSig, Value(default.requireTerminalCertSignature))
        register(SignHashExProp.RequireTerminalTxSig, Value(default.requireTerminalTxSignature))
        register(SignHashExProp.CheckPin3, Value(default.checkPIN3onCard))
        register(Denomination.WriteOnPersonalize, Value(default.writeOnPersonalization))
        register(Denomination.Denomination, Value(default.denomination))
        register(Token.ItsToken, Value(default.itsToken))
        register(Token.Symbol, Value(default.symbol))
        register(Token.ContractAddress, Value(default.contractAddress))
        register(Token.Decimal, Value(default.decimal))
        register(ProductMask.Note, Value(default.cardData.product_note))
        register(ProductMask.Tag, Value(default.cardData.product_tag))
        register(ProductMask.CardId, Value(default.cardData.product_id_card))
        register(ProductMask.IssuerId, Value(default.cardData.product_id_issuer))
        register(SettingsMask.IsReusable, Value(default.isReusable))
        register(SettingsMask.NeedActivation, Value(default.useActivation))
        register(SettingsMask.ForbidPurge, Value(default.forbidPurgeWallet))
        register(SettingsMask.AllowSelectBlockchain, Value(default.allowSelectBlockchain))
        register(SettingsMask.UseBlock, Value(default.useBlock))
        register(SettingsMask.OneApdu, Value(default.oneApdu))
        register(SettingsMask.UseCvc, Value(default.useCVC))
        register(SettingsMask.AllowSwapPin, Value(default.allowSwapPIN))
        register(SettingsMask.AllowSwapPin2, Value(default.allowSwapPIN2))
        register(SettingsMask.ForbidDefaultPin, Value(default.forbidDefaultPIN))
        register(SettingsMask.SmartSecurityDelay, Value(default.smartSecurityDelay))
        register(SettingsMask.ProtectIssuerDataAgainstReplay, Value(default.protectIssuerDataAgainstReplay))
        register(SettingsMask.SkipSecurityDelayIfValidated, Value(default.skipSecurityDelayIfValidatedByIssuer))
        register(SettingsMask.SkipPin2CvcIfValidated, Value(default.skipCheckPIN2andCVCIfValidatedByIssuer))
        register(SettingsMask.SkipSecurityDelayOnLinkedTerminal, Value(default.skipSecurityDelayIfValidatedByLinkedTerminal))
        register(SettingsMask.RestrictOverwriteExtraIssuerData, Value(default.restrictOverwriteIssuerDataEx))
        register(SettingsMaskProtocolEnc.AllowUnencrypted, Value(default.protocolAllowUnencrypted))
        register(SettingsMaskProtocolEnc.AllowStaticEncryption, Value(default.protocolAllowStaticEncryption))
        register(SettingsMaskNdef.UseNdef, Value(default.useNDEF))
        register(SettingsMaskNdef.DynamicNdef, Value(default.useDynamicNDEF))
        register(SettingsMaskNdef.DisablePrecomputedNdef, Value(default.disablePrecomputedNDEF))
        register(SettingsMaskNdef.Aar, Value(default.aar, Helper.aarList()))
        register(SettingsMaskNdef.AarCustom, Value(default.aar))
        register(Pins.Pin, Value(default.PIN))
        register(Pins.Pin2, Value(default.PIN2))
        register(Pins.Pin3, Value(default.PIN3))
        register(Pins.Cvc, Value(default.CVC))
        register(Pins.PauseBeforePin2, Value(default.pauseBeforePIN2, Helper.pauseBeforePin()))
    }
}

class Value(
        private var value: Any? = null,
        val list: List<KeyValue>? = mutableListOf()
) {
    fun get(): Any? = value
    fun set(newValue: Any?) {
        value = newValue
    }
}

internal class Helper {
    companion object {
        fun listOfCurves(): List<KeyValue> {
            return EllipticCurve.values().map { KeyValue(it.name, it.curve) }
        }

        fun listOfBlockchain(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("--- CUSTOM ---", ""),
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
                    KeyValue("CARDANO", "CARDANO"),
                    KeyValue("BNB", "BNB"),
                    KeyValue("XTZ", "XTZ"),
                    KeyValue("DUC", "DUC")
            )
        }

        fun aarList(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("--- CUSTOM ---", ""),
                    KeyValue("Release APP", "com.tangem.wallet"),
                    KeyValue("Debug APP", "com.tangem.wallet.debug"),
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