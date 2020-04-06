package com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.ItemsToModel
import com.tangem.tangemtest._arch.structure.impl.KeyValue
import com.tangem.tangemtest._arch.structure.impl.ListValueWrapper
import com.tangem.tangemtest.ucase.variants.personalize.*
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigValuesHolder
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ItemsToPersonalizeConfig : ItemsToModel<PersonalizeConfig> {
    protected val valuesHolder = PersonalizeConfigValuesHolder()

    override fun convert(from: List<Item>, default: PersonalizeConfig): PersonalizeConfig {
        valuesHolder.init(default)
        mapListItems(from)
        return createModel()
    }

    private fun mapListItems(itemList: List<Item>) {
        itemList.forEach { item -> mapItemToHolder(item) }
    }

    private fun mapItemToHolder(item: Item) {
        when (item) {
            is Block -> mapListItems(item.itemList)
            is BaseItem<*> -> {
                val defValue = valuesHolder.get(item.id) ?: return
                defValue.set(item.viewModel.data)
            }
        }
    }

    private fun createModel(): PersonalizeConfig {
        val export = PersonalizeConfig()
        export.series = getTyped(CardNumber.Series)
        export.startNumber = getTyped(CardNumber.Number)
        export.curveID = getTyped(Common.Curve)
        export.blockchain = getTyped(Common.Blockchain)
        export.blockchainCustom = getTyped(Common.BlockchainCustom)
        export.MaxSignatures = getTyped(Common.MaxSignatures)
        export.createWallet = getTyped(Common.CreateWallet)
        export.SigningMethod0 = getTyped(SigningMethod.SignTx)
        export.SigningMethod1 = getTyped(SigningMethod.SignTxRaw)
        export.SigningMethod2 = getTyped(SigningMethod.SignValidatedTx)
        export.SigningMethod3 = getTyped(SigningMethod.SignValidatedTxRaw)
        export.SigningMethod4 = getTyped(SigningMethod.SignValidatedTxIssuer)
        export.SigningMethod5 = getTyped(SigningMethod.SignValidatedTxRawIssuer)
        export.SigningMethod6 = getTyped(SigningMethod.SignExternal)
        export.pinLessFloorLimit = getTyped(SignHashExProp.PinLessFloorLimit)
        export.hexCrExKey = getTyped(SignHashExProp.CryptoExKey)
        export.requireTerminalCertSignature = getTyped(SignHashExProp.RequireTerminalCertSig)
        export.requireTerminalTxSignature = getTyped(SignHashExProp.RequireTerminalTxSig)
        export.checkPIN3onCard = getTyped(SignHashExProp.CheckPin3)
        export.writeOnPersonalization = getTyped(Denomination.WriteOnPersonalize)
        export.denomination = getTyped(Denomination.Denomination)
        export.itsToken = getTyped(Token.ItsToken)
        export.symbol = getTyped(Token.Symbol)
        export.contractAddress = getTyped(Token.ContractAddress)
        export.decimal = getTyped(Token.Decimal)
        export.cardData = export.cardData.apply { this.product_note = getTyped(ProductMask.Note) }
        export.cardData = export.cardData.apply { this.product_tag = getTyped(ProductMask.Tag) }
        export.cardData = export.cardData.apply { this.product_id_card = getTyped(ProductMask.IdCard) }
        export.cardData = export.cardData.apply { this.product_id_issuer = getTyped(ProductMask.IdIssuerCard) }
        export.isReusable = getTyped(SettingsMask.IsReusable)
        export.useActivation = getTyped(SettingsMask.NeedActivation)
        export.forbidPurgeWallet = getTyped(SettingsMask.ForbidPurge)
        export.allowSelectBlockchain = getTyped(SettingsMask.AllowSelectBlockchain)
        export.useBlock = getTyped(SettingsMask.UseBlock)
        export.oneApdu = getTyped(SettingsMask.OneApdu)
        export.useCVC = getTyped(SettingsMask.UseCvc)
        export.allowSwapPIN = getTyped(SettingsMask.AllowSwapPin)
        export.allowSwapPIN2 = getTyped(SettingsMask.AllowSwapPin2)
        export.forbidDefaultPIN = getTyped(SettingsMask.ForbidDefaultPin)
        export.smartSecurityDelay = getTyped(SettingsMask.SmartSecurityDelay)
        export.protectIssuerDataAgainstReplay = getTyped(SettingsMask.ProtectIssuerDataAgainstReplay)
        export.skipSecurityDelayIfValidatedByIssuer = getTyped(SettingsMask.SkipSecurityDelayIfValidated)
        export.skipCheckPIN2andCVCIfValidatedByIssuer = getTyped(SettingsMask.SkipPin2CvcIfValidated)
        export.skipSecurityDelayIfValidatedByLinkedTerminal = getTyped(SettingsMask.SkipSecurityDelayOnLinkedTerminal)
        export.restrictOverwriteIssuerDataEx = getTyped(SettingsMask.RestrictOverwriteExtraIssuerData)
        export.protocolAllowUnencrypted = getTyped(SettingsMaskProtocolEnc.AllowUnencrypted)
        export.protocolAllowStaticEncryption = getTyped(SettingsMaskProtocolEnc.AllowStaticEncryption)
        export.useNDEF = getTyped(SettingsMaskNdef.UseNdef)
        export.useDynamicNDEF = getTyped(SettingsMaskNdef.DynamicNdef)
        export.disablePrecomputedNDEF = getTyped(SettingsMaskNdef.DisablePrecomputedNdef)
        export.aar = getTyped(SettingsMaskNdef.Aar)
        export.aarCustom = getTyped(SettingsMaskNdef.AarCustom)
        export.PIN = getTyped(Pins.Pin)
        export.PIN2 = getTyped(Pins.Pin2)
        export.PIN3 = getTyped(Pins.Pin3)
        export.CVC = getTyped(Pins.Cvc)
        export.pauseBeforePIN2 = getTyped(Pins.PauseBeforePin2)
        return export
    }

    private inline fun <reified Type> getTyped(id: Id): Type {
        return getTypedBy<Type>(valuesHolder, id)!!
    }

    private inline fun <reified Type> getTypedBy(holder: PersonalizeConfigValuesHolder, id: Id): Type? {
        Log.d(this, "getTyped for id: $id")
        var typedValue = holder.get(id)?.get()

        typedValue = when (typedValue) {
            is ListValueWrapper -> (typedValue.selectedItem as KeyValue).value as Type
            else -> typedValue as? Type ?: null
        }
        return typedValue
    }
}