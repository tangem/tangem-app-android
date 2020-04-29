package com.tangem.devkit.ucase.variants.personalize.converter

import com.tangem.devkit._arch.structure.Additional
import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit._arch.structure.abstraction.*
import com.tangem.devkit._arch.structure.impl.*
import com.tangem.devkit.ucase.variants.personalize.*
import com.tangem.devkit.ucase.variants.personalize.dto.PersonalizationConfig
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
        export.series = getTyped(CardNumberId.Series)
        export.startNumber = getTyped(CardNumberId.Number)
        export.batchId = getTyped(CardNumberId.BatchId)
        export.curveID = getTyped(CommonId.Curve)
        export.blockchain = getTyped(CommonId.Blockchain)
        export.blockchainCustom = getTyped(CommonId.BlockchainCustom)
        export.MaxSignatures = getTyped(CommonId.MaxSignatures)
        export.createWallet = getTyped(CommonId.CreateWallet)
        export.SigningMethod0 = getTyped(SigningMethodId.SignTx)
        export.SigningMethod1 = getTyped(SigningMethodId.SignTxRaw)
        export.SigningMethod2 = getTyped(SigningMethodId.SignValidatedTx)
        export.SigningMethod3 = getTyped(SigningMethodId.SignValidatedTxRaw)
        export.SigningMethod4 = getTyped(SigningMethodId.SignValidatedTxIssuer)
        export.SigningMethod5 = getTyped(SigningMethodId.SignValidatedTxRawIssuer)
        export.SigningMethod6 = getTyped(SigningMethodId.SignExternal)
        export.pinLessFloorLimit = getTyped(SignHashExPropId.PinLessFloorLimit)
        export.hexCrExKey = getTyped(SignHashExPropId.CryptoExKey)
        export.requireTerminalCertSignature = getTyped(SignHashExPropId.RequireTerminalCertSig)
        export.requireTerminalTxSignature = getTyped(SignHashExPropId.RequireTerminalTxSig)
        export.checkPIN3onCard = getTyped(SignHashExPropId.CheckPin3)
        export.writeOnPersonalization = getTyped(DenominationId.WriteOnPersonalize)
        export.denomination = getTyped(DenominationId.Denomination)
        export.itsToken = getTyped(TokenId.ItsToken)
        export.symbol = getTyped(TokenId.Symbol)
        export.contractAddress = getTyped(TokenId.ContractAddress)
        export.decimal = getTyped(TokenId.Decimal)
        export.cardData = export.cardData.apply { this.product_note = getTyped(ProductMaskId.Note) }
        export.cardData = export.cardData.apply { this.product_tag = getTyped(ProductMaskId.Tag) }
        export.cardData = export.cardData.apply { this.product_id_card = getTyped(ProductMaskId.IdCard) }
        export.cardData = export.cardData.apply { this.product_id_issuer = getTyped(ProductMaskId.IdIssuerCard) }
        export.isReusable = getTyped(SettingsMaskId.IsReusable)
        export.useActivation = getTyped(SettingsMaskId.NeedActivation)
        export.forbidPurgeWallet = getTyped(SettingsMaskId.ForbidPurge)
        export.allowSelectBlockchain = getTyped(SettingsMaskId.AllowSelectBlockchain)
        export.useBlock = getTyped(SettingsMaskId.UseBlock)
        export.oneApdu = getTyped(SettingsMaskId.OneApdu)
        export.useCVC = getTyped(SettingsMaskId.UseCvc)
        export.allowSwapPIN = getTyped(SettingsMaskId.AllowSwapPin)
        export.allowSwapPIN2 = getTyped(SettingsMaskId.AllowSwapPin2)
        export.forbidDefaultPIN = getTyped(SettingsMaskId.ForbidDefaultPin)
        export.smartSecurityDelay = getTyped(SettingsMaskId.SmartSecurityDelay)
        export.protectIssuerDataAgainstReplay = getTyped(SettingsMaskId.ProtectIssuerDataAgainstReplay)
        export.skipSecurityDelayIfValidatedByIssuer = getTyped(SettingsMaskId.SkipSecurityDelayIfValidated)
        export.skipCheckPIN2andCVCIfValidatedByIssuer = getTyped(SettingsMaskId.SkipPin2CvcIfValidated)
        export.skipSecurityDelayIfValidatedByLinkedTerminal = getTyped(SettingsMaskId.SkipSecurityDelayOnLinkedTerminal)
        export.restrictOverwriteIssuerDataEx = getTyped(SettingsMaskId.RestrictOverwriteExtraIssuerData)
        export.protocolAllowUnencrypted = getTyped(SettingsMaskProtocolEncId.AllowUnencrypted)
        export.protocolAllowStaticEncryption = getTyped(SettingsMaskProtocolEncId.AllowStaticEncryption)
        export.useNDEF = getTyped(SettingsMaskNdefId.UseNdef)
        export.useDynamicNDEF = getTyped(SettingsMaskNdefId.DynamicNdef)
        export.disablePrecomputedNDEF = getTyped(SettingsMaskNdefId.DisablePrecomputedNdef)
        export.aar = getTyped(SettingsMaskNdefId.Aar)
        export.aarCustom = getTyped(SettingsMaskNdefId.AarCustom)
        export.uri = getTyped(SettingsMaskNdefId.Uri)
        export.PIN = getTyped(PinsId.Pin)
        export.PIN2 = getTyped(PinsId.Pin2)
        export.PIN3 = getTyped(PinsId.Pin3)
        export.CVC = getTyped(PinsId.Cvc)
        export.pauseBeforePIN2 = getTyped(PinsId.PauseBeforePin2)
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
//        blocList.add(signHashExProperties())
//        blocList.add(denomination())
//        blocList.add(token())
//        blocList.add(productMask())
//        blocList.add(settingsMask())
//        blocList.add(settingsMaskProtocolEnc())
//        blocList.add(settingsMaskNdef())
//        blocList.add(pins())
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
                CardNumberId.Series,
                CardNumberId.Number,
                CardNumberId.BatchId
        ).forEach { createItem(block, it as Id) }
        return block
    }

    private fun common(): ItemGroup {
        val block = createGroup(BlockId.Common)
        mutableListOf(
                CommonId.Blockchain,
                CommonId.BlockchainCustom,
                CommonId.Curve,
                CommonId.MaxSignatures,
                CommonId.CreateWallet,
                PinsId.PauseBeforePin2
        ).forEach { createItem(block, it as Id) }
        return block
    }

    private fun signingMethod(): ItemGroup {
        val block = createGroup(BlockId.SigningMethod)
        mutableListOf(
                SigningMethodId.SignTx,
                SigningMethodId.SignTxRaw,
                SigningMethodId.SignValidatedTx,
                SigningMethodId.SignValidatedTxRaw,
                SigningMethodId.SignValidatedTxIssuer,
                SigningMethodId.SignValidatedTxRawIssuer,
                SigningMethodId.SignExternal
        ).forEach { createItem(block, it) }
        return block
    }

    private fun signHashExProperties(): ItemGroup {
        val block = createGroup(BlockId.SignHashExProp)
        mutableListOf(
                SignHashExPropId.PinLessFloorLimit,
                SignHashExPropId.CryptoExKey,
                SignHashExPropId.RequireTerminalCertSig,
                SignHashExPropId.RequireTerminalTxSig,
                SignHashExPropId.CheckPin3
        ).forEach { createItem(block, it) }
        return block
    }

    private fun denomination(): ItemGroup {
        val block = createGroup(BlockId.Denomination)
        mutableListOf(
                DenominationId.WriteOnPersonalize,
                DenominationId.Denomination
        ).forEach { createItem(block, it) }
        return block
    }

    private fun token(): ItemGroup {
        val block = createGroup(BlockId.Token)
        mutableListOf(
                TokenId.ItsToken,
                TokenId.Symbol,
                TokenId.ContractAddress,
                TokenId.Decimal
        ).forEach { createItem(block, it) }
        return block
    }

    private fun productMask(): ItemGroup {
        val block = createGroup(BlockId.ProdMask)
        mutableListOf(
                ProductMaskId.Note,
                ProductMaskId.Tag,
                ProductMaskId.IdCard,
                ProductMaskId.IdIssuerCard
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMask(): ItemGroup {
        val block = createGroup(BlockId.SettingsMask)
        mutableListOf(
                SettingsMaskId.IsReusable,
                SettingsMaskId.NeedActivation,
                SettingsMaskId.ForbidPurge,
                SettingsMaskId.AllowSelectBlockchain,
                SettingsMaskId.UseBlock,
                SettingsMaskId.OneApdu,
                SettingsMaskId.UseCvc,
                SettingsMaskId.AllowSwapPin,
                SettingsMaskId.AllowSwapPin2,
                SettingsMaskId.ForbidDefaultPin,
                SettingsMaskId.SmartSecurityDelay,
                SettingsMaskId.ProtectIssuerDataAgainstReplay,
                SettingsMaskId.SkipSecurityDelayIfValidated,
                SettingsMaskId.SkipPin2CvcIfValidated,
                SettingsMaskId.SkipSecurityDelayOnLinkedTerminal,
                SettingsMaskId.RestrictOverwriteExtraIssuerData
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskProtocolEnc(): ItemGroup {
        val block = createGroup(BlockId.SettingsMaskProtocolEnc)
        mutableListOf(
                SettingsMaskProtocolEncId.AllowUnencrypted,
                SettingsMaskProtocolEncId.AllowStaticEncryption
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskNdef(): ItemGroup {
        val block = createGroup(BlockId.SettingsMaskNdef)
        mutableListOf(
                SettingsMaskNdefId.UseNdef,
                SettingsMaskNdefId.DynamicNdef,
                SettingsMaskNdefId.DisablePrecomputedNdef,
                SettingsMaskNdefId.Aar,
                SettingsMaskNdefId.AarCustom,
                SettingsMaskNdefId.Uri
        ).forEach { createItem(block, it) }
        return block
    }

    private fun pins(): ItemGroup {
        val block = createGroup(BlockId.Pins)
        mutableListOf(
                PinsId.Pin,
                PinsId.Pin2,
                PinsId.Pin3,
                PinsId.Cvc
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