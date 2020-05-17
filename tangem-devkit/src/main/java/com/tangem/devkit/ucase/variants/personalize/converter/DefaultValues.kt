package com.tangem.devkit.ucase.variants.personalize.converter

import com.tangem.commands.EllipticCurve
import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit._arch.structure.abstraction.KeyValue
import com.tangem.devkit.ucase.variants.personalize.*
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationConfig
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationJson
import ru.dev.gbixahue.eu4d.lib.kotlin.common.BaseTypedHolder

/**
[REDACTED_AUTHOR]
 */
class ConfigValuesHolder : BaseTypedHolder<Id, Value>() {

    // All registered association values can't be objects
    fun init(default: PersonalizationConfig) {
        register(CardNumberId.Series, Value(default.series))
        register(CardNumberId.Number, Value(default.startNumber))
        register(CardNumberId.BatchId, Value(default.cardData.batch))
        register(CommonId.Curve, Value(default.curveID, Helper.listOfCurves()))
        register(CommonId.Blockchain, Value(default.cardData.blockchain, Helper.listOfBlockchain()))
        register(CommonId.BlockchainCustom, Value(default.blockchainCustom))
        register(CommonId.MaxSignatures, Value(default.MaxSignatures))
        register(CommonId.CreateWallet, Value(default.createWallet))
        register(SigningMethodId.SignTx, Value(default.SigningMethod0))
        register(SigningMethodId.SignTxRaw, Value(default.SigningMethod1))
        register(SigningMethodId.SignValidatedTx, Value(default.SigningMethod2))
        register(SigningMethodId.SignValidatedTxRaw, Value(default.SigningMethod3))
        register(SigningMethodId.SignValidatedTxIssuer, Value(default.SigningMethod4))
        register(SigningMethodId.SignValidatedTxRawIssuer, Value(default.SigningMethod5))
        register(SigningMethodId.SignExternal, Value(default.SigningMethod6))
        register(SignHashExPropId.PinLessFloorLimit, Value(default.pinLessFloorLimit))
        register(SignHashExPropId.CryptoExKey, Value(default.hexCrExKey))
        register(SignHashExPropId.RequireTerminalCertSig, Value(default.requireTerminalCertSignature))
        register(SignHashExPropId.RequireTerminalTxSig, Value(default.requireTerminalTxSignature))
        register(SignHashExPropId.CheckPin3, Value(default.checkPIN3onCard))
        register(TokenId.ItsToken, Value(default.itsToken))
        register(TokenId.Symbol, Value(default.cardData.token_symbol))
        register(TokenId.ContractAddress, Value(default.cardData.token_contract_address))
        register(TokenId.Decimal, Value(default.cardData.token_decimal))
        register(ProductMaskId.Note, Value(default.cardData.product_note))
        register(ProductMaskId.Tag, Value(default.cardData.product_tag))
        register(ProductMaskId.IdCard, Value(default.cardData.product_id_card))
        register(ProductMaskId.IdIssuerCard, Value(default.cardData.product_id_issuer))
        register(SettingsMaskId.IsReusable, Value(default.isReusable))
        register(SettingsMaskId.NeedActivation, Value(default.useActivation))
        register(SettingsMaskId.ForbidPurge, Value(default.forbidPurgeWallet))
        register(SettingsMaskId.AllowSelectBlockchain, Value(default.allowSelectBlockchain))
        register(SettingsMaskId.UseBlock, Value(default.useBlock))
        register(SettingsMaskId.OneApdu, Value(default.useOneCommandAtTime))
        register(SettingsMaskId.UseCvc, Value(default.useCVC))
        register(SettingsMaskId.AllowSwapPin, Value(default.allowSwapPIN))
        register(SettingsMaskId.AllowSwapPin2, Value(default.allowSwapPIN2))
        register(SettingsMaskId.ForbidDefaultPin, Value(default.forbidDefaultPIN))
        register(SettingsMaskId.SmartSecurityDelay, Value(default.smartSecurityDelay))
        register(SettingsMaskId.ProtectIssuerDataAgainstReplay, Value(default.protectIssuerDataAgainstReplay))
        register(SettingsMaskId.SkipSecurityDelayIfValidated, Value(default.skipSecurityDelayIfValidatedByIssuer))
        register(SettingsMaskId.SkipPin2CvcIfValidated, Value(default.skipCheckPIN2andCVCIfValidatedByIssuer))
        register(SettingsMaskId.SkipSecurityDelayOnLinkedTerminal, Value(default.skipSecurityDelayIfValidatedByLinkedTerminal))
        register(SettingsMaskId.RestrictOverwriteExtraIssuerData, Value(default.restrictOverwriteIssuerDataEx))
        register(SettingsMaskProtocolEncId.AllowUnencrypted, Value(default.protocolAllowUnencrypted))
        register(SettingsMaskProtocolEncId.AllowStaticEncryption, Value(default.protocolAllowStaticEncryption))
        register(SettingsMaskNdefId.UseNdef, Value(default.useNDEF))
        register(SettingsMaskNdefId.DynamicNdef, Value(default.useDynamicNDEF))
        register(SettingsMaskNdefId.DisablePrecomputedNdef, Value(default.disablePrecomputedNDEF))
        register(SettingsMaskNdefId.Aar, Value(default.aar, Helper.aarList()))
        register(SettingsMaskNdefId.AarCustom, Value(default.aarCustom))
        register(SettingsMaskNdefId.Uri, Value(default.uri))
        register(PinsId.Pin, Value(default.PIN))
        register(PinsId.Pin2, Value(default.PIN2))
        register(PinsId.Pin3, Value(default.PIN3))
        register(PinsId.Cvc, Value(default.CVC))
        register(PinsId.PauseBeforePin2, Value(default.pauseBeforePIN2, Helper.pauseBeforePin()))
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
                    KeyValue(PersonalizationJson.CUSTOM, PersonalizationJson.CUSTOM),
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
                    KeyValue("BNB", "BINANCE"),
                    KeyValue("XTZ", "TEZOS"),
                    KeyValue("DUC", "DUC")
            )
        }

        fun aarList(): List<KeyValue> {
            return mutableListOf(
                    KeyValue(PersonalizationJson.CUSTOM, PersonalizationJson.CUSTOM),
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