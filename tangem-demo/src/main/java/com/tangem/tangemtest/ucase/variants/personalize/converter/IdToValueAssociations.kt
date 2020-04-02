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
        register(CardNumber.Series, Value(from.series))
        register(CardNumber.Number, Value(from.startNumber))
        register(CardNumber.BatchId, Value(from.batchId))
        register(Common.Curve, Value(from.curveID, Helper.listOfCurves()))
        register(Common.Blockchain, Value(from.blockchain, Helper.listOfBlockchain()))
        register(Common.BlockchainCustom, Value(""))
        register(Common.MaxSignatures, Value(from.MaxSignatures))
        register(Common.CreateWallet, Value(from.createWallet))
        register(SigningMethod.SignTx, Value(from.SigningMethod0))
        register(SigningMethod.SignTxRaw, Value(from.SigningMethod1))
        register(SigningMethod.SignValidatedTx, Value(from.SigningMethod2))
        register(SigningMethod.SignValidatedTxRaw, Value(from.SigningMethod3))
        register(SigningMethod.SignValidatedTxIssuer, Value(from.SigningMethod4))
        register(SigningMethod.SignValidatedTxRawIssuer, Value(from.SigningMethod5))
        register(SigningMethod.SignExternal, Value(from.SigningMethod6))
        register(SignHashExProp.PinLessFloorLimit, Value(from.pinLessFloorLimit))
        register(SignHashExProp.CryptoExKey, Value(from.hexCrExKey))
        register(SignHashExProp.RequireTerminalCertSig, Value(from.requireTerminalCertSignature))
        register(SignHashExProp.RequireTerminalTxSig, Value(from.requireTerminalTxSignature))
        register(SignHashExProp.CheckPin3, Value(from.checkPIN3onCard))
        register(Denomination.WriteOnPersonalize, Value(from.writeOnPersonalization))
        register(Denomination.Denomination, Value(from.denomination))
        register(Token.ItsToken, Value(from.itsToken))
        register(Token.Symbol, Value(from.symbol))
        register(Token.ContractAddress, Value(from.contractAddress))
        register(Token.Decimal, Value(from.decimal))
        register(ProductMask.Note, Value(from.cardData.product_note))
        register(ProductMask.Tag, Value(from.cardData.product_tag))
        register(ProductMask.CardId, Value(from.cardData.product_id_card))
        register(SettingsMask.IsReusable, Value(from.isReusable))
        register(SettingsMask.NeedActivation, Value(from.useActivation))
        register(SettingsMask.ForbidPurge, Value(from.forbidPurgeWallet))
        register(SettingsMask.AllowSelectBlockchain, Value(from.allowSelectBlockchain))
        register(SettingsMask.UseBlock, Value(from.useBlock))
        register(SettingsMask.OneApdu, Value(from.oneApdu))
        register(SettingsMask.UseCvc, Value(from.useCVC))
        register(SettingsMask.AllowSwapPin, Value(from.allowSwapPIN))
        register(SettingsMask.AllowSwapPin2, Value(from.allowSwapPIN2))
        register(SettingsMask.ForbidDefaultPin, Value(from.forbidDefaultPIN))
        register(SettingsMask.SmartSecurityDelay, Value(from.smartSecurityDelay))
        register(SettingsMask.ProtectIssuerDataAgainstReplay, Value(from.protectIssuerDataAgainstReplay))
        register(SettingsMask.SkipSecurityDelayIfValidated, Value(from.skipSecurityDelayIfValidatedByIssuer))
        register(SettingsMask.SkipPin2CvcIfValidated, Value(from.skipCheckPIN2andCVCIfValidatedByIssuer))
        register(SettingsMask.SkipSecurityDelayOnLinkedTerminal, Value(from.skipSecurityDelayIfValidatedByLinkedTerminal))
        register(SettingsMask.RestrictOverwriteExtraIssuerData, Value(from.restrictOverwriteIssuerDataEx))
        register(SettingsMaskProtocolEnc.AllowUnencrypted, Value(from.protocolAllowUnencrypted))
        register(SettingsMaskProtocolEnc.AllowStaticEncryption, Value(from.protocolAllowStaticEncryption))
        register(SettingsMaskNdef.UseNdef, Value(from.useNDEF))
        register(SettingsMaskNdef.DynamicNdef, Value(from.useDynamicNDEF))
        register(SettingsMaskNdef.DisablePrecomputedNdef, Value(from.disablePrecomputedNDEF))
        register(SettingsMaskNdef.Aar, Value(from.aar, Helper.aarList()))
        register(SettingsMaskNdef.AarCustom, Value(from.aar))
        register(Pins.Pin, Value(from.PIN))
        register(Pins.Pin2, Value(from.PIN2))
        register(Pins.Pin3, Value(from.PIN3))
        register(Pins.Cvc, Value(from.CVC))
        register(Pins.PauseBeforePin2, Value(from.pauseBeforePIN2, Helper.pauseBeforePin()))
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
                    KeyValue("CARDANO", "CARDANO")
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