package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.Additional
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.*
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.ucase.variants.personalize.*
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizationConfig
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class PersonalizationConfigConverter : ModelConverter<PersonalizationConfig> {

    private val toModel: ItemsToModel<PersonalizationConfig> = ItemsToPersonalizationConfig()
    private val toItems: ModelToItems<PersonalizationConfig> = PersonalizationConfigToItems()

    override fun convert(from: List<Item>, default: PersonalizationConfig): PersonalizationConfig {
        return toModel.convert(from, default)
    }

    override fun convert(from: PersonalizationConfig): List<Item> {
        return toItems.convert(from)
    }
}

class ItemsToPersonalizationConfig : ItemsToModel<PersonalizationConfig> {
    protected val valuesHolder = ConfigValuesHolder()

    override fun convert(from: List<Item>, default: PersonalizationConfig): PersonalizationConfig {
        valuesHolder.init(default)
        mapListItems(from)
        return createModel()
    }

    private fun mapListItems(itemList: List<Item>) {
        itemList.iterate { item -> mapItemToHolder(item) }
    }

    private fun mapItemToHolder(item: Item) {
        when (item) {
            is ItemGroup -> mapListItems(item.itemList)
            is BaseItem -> {
                val defValue = valuesHolder.get(item.id) ?: return
                defValue.set(item.viewModel.data)
            }
        }
    }

    private fun createModel(): PersonalizationConfig {
        val export = PersonalizationConfig()
        export.series = getTyped(CardNumber.Series)
        export.startNumber = getTyped(CardNumber.Number)
        export.batchId = getTyped(CardNumber.BatchId)
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
        export.uri = getTyped(SettingsMaskNdef.Uri)
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

    private inline fun <reified Type> getTypedBy(holder: ConfigValuesHolder, id: Id): Type? {
        Log.d(this, "getTyped for id: $id")
        var typedValue = holder.get(id)?.get()

        typedValue = when (typedValue) {
            is ListViewModel -> typedValue.selectedItem as Type
            else -> typedValue as? Type ?: null
        }
        return typedValue
    }
}

class PersonalizationConfigToItems : ModelToItems<PersonalizationConfig> {
    private val valuesHolder = ConfigValuesHolder()
    private val itemTypes = ItemTypes()

    override fun convert(from: PersonalizationConfig): List<Item> {
        valuesHolder.init(from)
        val blocList = mutableListOf<Item>()
        blocList.add(cardNumber())
        blocList.add(common())
        blocList.add(signingMethod())
        blocList.add(signHashExProperties())
        blocList.add(denomination())
        blocList.add(token())
        blocList.add(productMask())
        blocList.add(settingsMask())
        blocList.add(settingsMaskProtocolEnc())
        blocList.add(settingsMaskNdef())
        blocList.add(pins())
        blocList.iterate {
            if (itemTypes.hiddenList.contains(it.id)) {
                it.viewModel.viewState.isVisibleState.value = false
            }
        }
        return blocList
    }

    private fun cardNumber(): ItemGroup {
        val block = createGroup(BlockId.CardNumber)
        mutableListOf(
                CardNumber.Series,
                CardNumber.Number,
                CardNumber.BatchId
        ).forEach { createItem(block, it as Id) }
        return block
    }

    private fun common(): ItemGroup {
        val block = createGroup(BlockId.Common)
        mutableListOf(
                Common.Blockchain,
                Common.BlockchainCustom,
                Common.Curve,
                Common.MaxSignatures,
                Common.CreateWallet,
                Pins.PauseBeforePin2
        ).forEach { createItem(block, it as Id) }
        return block
    }

    private fun signingMethod(): ItemGroup {
        val block = createGroup(BlockId.SigningMethod)
        mutableListOf(
                SigningMethod.SignTx,
                SigningMethod.SignTxRaw,
                SigningMethod.SignValidatedTx,
                SigningMethod.SignValidatedTxRaw,
                SigningMethod.SignValidatedTxIssuer,
                SigningMethod.SignValidatedTxRawIssuer,
                SigningMethod.SignExternal
        ).forEach { createItem(block, it) }
        return block
    }

    private fun signHashExProperties(): ItemGroup {
        val block = createGroup(BlockId.SignHashExProp)
        mutableListOf(
                SignHashExProp.PinLessFloorLimit,
                SignHashExProp.CryptoExKey,
                SignHashExProp.RequireTerminalCertSig,
                SignHashExProp.RequireTerminalTxSig,
                SignHashExProp.CheckPin3
        ).forEach { createItem(block, it) }
        return block
    }

    private fun denomination(): ItemGroup {
        val block = createGroup(BlockId.Denomination)
        mutableListOf(
                Denomination.WriteOnPersonalize,
                Denomination.Denomination
        ).forEach { createItem(block, it) }
        return block
    }

    private fun token(): ItemGroup {
        val block = createGroup(BlockId.Token)
        mutableListOf(
                Token.ItsToken,
                Token.Symbol,
                Token.ContractAddress,
                Token.Decimal
        ).forEach { createItem(block, it) }
        return block
    }

    private fun productMask(): ItemGroup {
        val block = createGroup(BlockId.ProdMask)
        mutableListOf(
                ProductMask.Note,
                ProductMask.Tag,
                ProductMask.IdCard,
                ProductMask.IdIssuerCard
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMask(): ItemGroup {
        val block = createGroup(BlockId.SettingsMask)
        mutableListOf(
                SettingsMask.IsReusable,
                SettingsMask.NeedActivation,
                SettingsMask.ForbidPurge,
                SettingsMask.AllowSelectBlockchain,
                SettingsMask.UseBlock,
                SettingsMask.OneApdu,
                SettingsMask.UseCvc,
                SettingsMask.AllowSwapPin,
                SettingsMask.AllowSwapPin2,
                SettingsMask.ForbidDefaultPin,
                SettingsMask.SmartSecurityDelay,
                SettingsMask.ProtectIssuerDataAgainstReplay,
                SettingsMask.SkipSecurityDelayIfValidated,
                SettingsMask.SkipPin2CvcIfValidated,
                SettingsMask.SkipSecurityDelayOnLinkedTerminal,
                SettingsMask.RestrictOverwriteExtraIssuerData
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskProtocolEnc(): ItemGroup {
        val block = createGroup(BlockId.SettingsMaskProtocolEnc)
        mutableListOf(
                SettingsMaskProtocolEnc.AllowUnencrypted,
                SettingsMaskProtocolEnc.AllowStaticEncryption
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskNdef(): ItemGroup {
        val block = createGroup(BlockId.SettingsMaskNdef)
        mutableListOf(
                SettingsMaskNdef.UseNdef,
                SettingsMaskNdef.DynamicNdef,
                SettingsMaskNdef.DisablePrecomputedNdef,
                SettingsMaskNdef.Aar,
                SettingsMaskNdef.AarCustom,
                SettingsMaskNdef.Uri
        ).forEach { createItem(block, it) }
        return block
    }

    private fun pins(): ItemGroup {
        val block = createGroup(BlockId.Pins)
        mutableListOf(
                Pins.Pin,
                Pins.Pin2,
                Pins.Pin3,
                Pins.Cvc
        ).forEach { createItem(block, it) }
        return block
    }

    private fun createGroup(id: Id): ItemGroup {
        return SimpleItemGroup(id).apply { addItem(TextItem(id)) }
    }

    private fun createItem(itemGroup: ItemGroup, id: Id) {
        val holder = valuesHolder.get(id) ?: return
        val item = when {
            itemTypes.blockIdList.contains(id) -> TextItem(id, holder.get() as? String)
            itemTypes.listItemList.contains(id) -> SpinnerItem(id, holder.list as List<KeyValue>, holder.get())
            itemTypes.boolList.contains(id) -> BoolItem(id, holder.get() as? Boolean)
            itemTypes.editTextList.contains(id) -> EditTextItem(id, holder.get() as? String)
            itemTypes.numberList.contains(id) -> NumberItem(id, holder.get() as? Number)
            else -> SimpleItemGroup(Additional.UNDEFINED)
        }
        itemGroup.addItem(item)
    }
}