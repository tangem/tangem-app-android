package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.impl.KeyValue
import com.tangem.tangemtest._arch.structure.impl.ListValueWrapper
import com.tangem.tangemtest.ucase.variants.personalize.*
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ValueMapper {
    private val default: IdToValueAssociations = IdToValueAssociations()
    private val updatedByItemList: IdToValueAssociations = IdToValueAssociations()


    fun mapOnObject(itemList: List<Item>, defaultConfig: PersonalizeConfig): PersonalizeConfig {
        default.init(defaultConfig)
        updatedByItemList.init(defaultConfig)
        startMapping(itemList)
        return createJson(defaultConfig)
    }

    private fun startMapping(itemList: List<Item>) {
        itemList.forEach { item -> mapItem(item) }
    }

    private fun mapItem(item: Item) {
        when (item) {
            is Block -> startMapping(item.itemList)
            is BaseItem<*> -> {
                val value = updatedByItemList.get(item.id) ?: return
                value.set(item.viewModel.data)
            }
        }
    }

    private fun createJson(defaultConfig: PersonalizeConfig): PersonalizeConfig {
        val export = PersonalizeConfig()
        export.series = getTyped(CardNumber.Series, defaultConfig.series)
        export.startNumber = getTyped(CardNumber.Number, defaultConfig.startNumber)
        export.batchId = getTyped(CardNumber.BatchId, defaultConfig.batchId)
        export.curveID = getTyped(Common.Curve, defaultConfig.curveID)
        export.blockchain = getTyped(Common.Blockchain, defaultConfig.blockchain)
        export.blockchainCustom = getTyped(Common.BlockchainCustom, defaultConfig.blockchainCustom)
        export.MaxSignatures = getTyped(Common.MaxSignatures, defaultConfig.MaxSignatures)
        export.createWallet = getTyped(Common.CreateWallet, defaultConfig.createWallet)
        export.SigningMethod0 = getTyped(SigningMethod.SignTx, defaultConfig.SigningMethod0)
        export.SigningMethod1 = getTyped(SigningMethod.SignTxRaw, defaultConfig.SigningMethod1)
        export.SigningMethod2 = getTyped(SigningMethod.SignValidatedTx, defaultConfig.SigningMethod2)
        export.SigningMethod3 = getTyped(SigningMethod.SignValidatedTxRaw, defaultConfig.SigningMethod3)
        export.SigningMethod4 = getTyped(SigningMethod.SignValidatedTxIssuer, defaultConfig.SigningMethod4)
        export.SigningMethod5 = getTyped(SigningMethod.SignValidatedTxRawIssuer, defaultConfig.SigningMethod5)
        export.SigningMethod6 = getTyped(SigningMethod.SignExternal, defaultConfig.SigningMethod6)
        export.pinLessFloorLimit = getTyped(SignHashExProp.PinLessFloorLimit, defaultConfig.pinLessFloorLimit)
        export.hexCrExKey = getTyped(SignHashExProp.CryptoExKey, defaultConfig.hexCrExKey)
        export.requireTerminalCertSignature = getTyped(SignHashExProp.RequireTerminalCertSig, defaultConfig.requireTerminalCertSignature)
        export.requireTerminalTxSignature = getTyped(SignHashExProp.RequireTerminalTxSig, defaultConfig.requireTerminalTxSignature)
        export.checkPIN3onCard = getTyped(SignHashExProp.CheckPin3, defaultConfig.checkPIN3onCard)
        export.writeOnPersonalization = getTyped(Denomination.WriteOnPersonalize, defaultConfig.writeOnPersonalization)
        export.denomination = getTyped(Denomination.Denomination, defaultConfig.denomination)
        export.itsToken = getTyped(Token.ItsToken, defaultConfig.itsToken)
        export.symbol = getTyped(Token.Symbol, defaultConfig.symbol)
        export.contractAddress = getTyped(Token.ContractAddress, defaultConfig.contractAddress)
        export.decimal = getTyped(Token.Decimal, defaultConfig.decimal)
        export.cardData = export.cardData.apply { this.product_note = getTyped(ProductMask.Note, defaultConfig.cardData.product_note) }
        export.cardData = export.cardData.apply { this.product_tag = getTyped(ProductMask.Tag, defaultConfig.cardData.product_tag) }
        export.cardData = export.cardData.apply { this.product_id_card = getTyped(ProductMask.CardId, defaultConfig.cardData.product_id_card) }
        export.isReusable = getTyped(SettingsMask.IsReusable, defaultConfig.isReusable)
        export.useActivation = getTyped(SettingsMask.NeedActivation, defaultConfig.useActivation)
        export.forbidPurgeWallet = getTyped(SettingsMask.ForbidPurge, defaultConfig.forbidPurgeWallet)
        export.allowSelectBlockchain = getTyped(SettingsMask.AllowSelectBlockchain, defaultConfig.allowSelectBlockchain)
        export.useBlock = getTyped(SettingsMask.UseBlock, defaultConfig.useBlock)
        export.oneApdu = getTyped(SettingsMask.OneApdu, defaultConfig.oneApdu)
        export.useCVC = getTyped(SettingsMask.UseCvc, defaultConfig.useCVC)
        export.allowSwapPIN = getTyped(SettingsMask.AllowSwapPin, defaultConfig.allowSwapPIN)
        export.allowSwapPIN2 = getTyped(SettingsMask.AllowSwapPin2, defaultConfig.allowSwapPIN2)
        export.forbidDefaultPIN = getTyped(SettingsMask.ForbidDefaultPin, defaultConfig.forbidDefaultPIN)
        export.smartSecurityDelay = getTyped(SettingsMask.SmartSecurityDelay, defaultConfig.smartSecurityDelay)
        export.protectIssuerDataAgainstReplay = getTyped(SettingsMask.ProtectIssuerDataAgainstReplay, defaultConfig.protectIssuerDataAgainstReplay)
        export.skipSecurityDelayIfValidatedByIssuer = getTyped(SettingsMask.SkipSecurityDelayIfValidated, defaultConfig.skipSecurityDelayIfValidatedByIssuer)
        export.skipCheckPIN2andCVCIfValidatedByIssuer = getTyped(SettingsMask.SkipPin2CvcIfValidated, defaultConfig.skipCheckPIN2andCVCIfValidatedByIssuer)
        export.skipSecurityDelayIfValidatedByLinkedTerminal = getTyped(SettingsMask.SkipSecurityDelayOnLinkedTerminal, defaultConfig.skipSecurityDelayIfValidatedByLinkedTerminal)
        export.restrictOverwriteIssuerDataEx = getTyped(SettingsMask.RestrictOverwriteExtraIssuerData, defaultConfig.restrictOverwriteIssuerDataEx)
        export.protocolAllowUnencrypted = getTyped(SettingsMaskProtocolEnc.AllowUnencrypted, defaultConfig.protocolAllowUnencrypted)
        export.allowFastEncryption = getTyped(SettingsMaskProtocolEnc.AlloFastEncryption, defaultConfig.protocolAllowStaticEncryption)
        export.useNDEF = getTyped(SettingsMaskNdef.UseNdef, defaultConfig.useNDEF)
        export.useDynamicNDEF = getTyped(SettingsMaskNdef.DynamicNdef, defaultConfig.useDynamicNDEF)
        export.disablePrecomputedNDEF = getTyped(SettingsMaskNdef.DisablePrecomputedNdef, defaultConfig.disablePrecomputedNDEF)
        export.aar = getTyped(SettingsMaskNdef.Aar, defaultConfig.aar)
        export.aarCustom = getTyped(SettingsMaskNdef.AarCustom, defaultConfig.aarCustom)
        export.PIN = getTyped(Pins.Pin, defaultConfig.PIN)
        export.PIN2 = getTyped(Pins.Pin2, defaultConfig.PIN2)
        export.PIN3 = getTyped(Pins.Pin3, defaultConfig.PIN3)
        export.CVC = getTyped(Pins.Cvc, defaultConfig.CVC)
        export.pauseBeforePIN2 = getTyped(Pins.PauseBeforePin2, defaultConfig.pauseBeforePIN2)
        return export
    }

    private inline fun <reified Type> getTyped(id: Id, def: Type): Type {
        return getTypedBy<Type>(updatedByItemList, id) ?: getTypedBy<Type>(default, id)!!
    }

    private inline fun <reified Type> getTypedBy(associations: IdToValueAssociations, id: Id): Type? {
        Log.d(this, "getTyped for id: $id")
        var typedValue = associations.get(id)?.get()

        typedValue = when (typedValue) {
            is ListValueWrapper -> (typedValue.selectedItem as KeyValue).value as Type
            else -> typedValue as? Type ?: null
        }
        return typedValue
    }
}