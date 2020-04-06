package com.tangem.tangemtest.ucase.variants.personalize.converter.fromTo

import com.tangem.commands.CardData
import com.tangem.commands.EllipticCurve
import com.tangem.commands.ProductMaskBuilder
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.commands.personalization.entities.NdefRecord
import com.tangem.tangemtest._arch.structure.Additional
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.*
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.variants.personalize.*
import com.tangem.tangemtest.ucase.variants.personalize.converter.PersonalizeConfigValuesHolder
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class PersonalizeConfigToItem : ModelToItems<PersonalizeConfig> {
    private val valuesHolder = PersonalizeConfigValuesHolder()
    private val itemTypes = ItemTypes()

    override fun convert(from: PersonalizeConfig): List<Item> {
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
        val payloadBlock = ListItemBlock(Additional.JSON_TAILS)
        addPayload(payloadBlock, from)
        blocList.add(payloadBlock)
        blocList.iterate {
            if (itemTypes.hiddenList.contains(it.id)) {
                (it as BaseItem<*>)?.viewModel.viewState.isHiddenField = true
            }
        }
        return blocList
    }

    private fun cardNumber(): Block {
        val block = createBlock(BlockId.CardNumber)
        mutableListOf(
                CardNumber.Series,
                CardNumber.Number,
                CardNumber.BatchId
        ).forEach { createItem(block, it as Id) }
        return block
    }

    private fun common(): Block {
        val block = createBlock(BlockId.Common)
        mutableListOf(
                Common.Curve,
                Common.Blockchain,
                Common.BlockchainCustom,
                Common.MaxSignatures,
                Common.CreateWallet
        ).forEach { createItem(block, it) }
        return block
    }

    private fun signingMethod(): Block {
        val block = createBlock(BlockId.SigningMethod)
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

    private fun signHashExProperties(): Block {
        val block = createBlock(BlockId.SignHashExProp)
        mutableListOf(
                SignHashExProp.PinLessFloorLimit,
                SignHashExProp.CryptoExKey,
                SignHashExProp.RequireTerminalCertSig,
                SignHashExProp.RequireTerminalTxSig,
                SignHashExProp.CheckPin3
        ).forEach { createItem(block, it) }
        return block
    }

    private fun denomination(): Block {
        val block = createBlock(BlockId.Denomination)
        mutableListOf(
                Denomination.WriteOnPersonalize,
                Denomination.Denomination
        ).forEach { createItem(block, it) }
        return block
    }

    private fun token(): Block {
        val block = createBlock(BlockId.Token)
        mutableListOf(
                Token.ItsToken,
                Token.Symbol,
                Token.ContractAddress,
                Token.Decimal
        ).forEach { createItem(block, it) }
        return block
    }

    private fun productMask(): Block {
        val block = createBlock(BlockId.ProdMask)
        mutableListOf(
                ProductMask.Note,
                ProductMask.Tag,
                ProductMask.CardId,
                ProductMask.IssuerId
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMask(): Block {
        val block = createBlock(BlockId.SettingsMask)
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

    private fun settingsMaskProtocolEnc(): Block {
        val block = createBlock(BlockId.SettingsMaskProtocolEnc)
        mutableListOf(
                SettingsMaskProtocolEnc.AllowUnencrypted,
                SettingsMaskProtocolEnc.AllowStaticEncryption
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskNdef(): Block {
        val block = createBlock(BlockId.SettingsMaskNdef)
        mutableListOf(
                SettingsMaskNdef.UseNdef,
                SettingsMaskNdef.DynamicNdef,
                SettingsMaskNdef.DisablePrecomputedNdef,
                SettingsMaskNdef.Aar,
                SettingsMaskNdef.AarCustom
        ).forEach { createItem(block, it) }
        return block
    }

    private fun pins(): Block {
        val block = createBlock(BlockId.Pins)
        mutableListOf(Pins.Pin, Pins.Pin2, Pins.Pin3, Pins.Cvc, Pins.PauseBeforePin2)
                .forEach { createItem(block, it) }
        return block
    }

    private fun createBlock(id: Id): ListItemBlock {
        return ListItemBlock(id).apply { addItem(TextItem(id)) }
    }

    private fun addPayload(block: Block, from: PersonalizeConfig) {
        block.payload[PayloadKey.incomingJson] = from
    }

    private fun createItem(block: ListItemBlock, id: Id) {
        val holder = valuesHolder.get(id) ?: return
        val item = when {
            itemTypes.blockIdList.contains(id) -> TextItem(id, holder.get() as? String)
            itemTypes.listItemList.contains(id) -> ListItem(id, holder.list as List<KeyValue>, holder.get())
            itemTypes.boolList.contains(id) -> BoolItem(id, holder.get() as? Boolean)
            itemTypes.editTextList.contains(id) -> EditTextItem(id, holder.get() as? String)
            itemTypes.numberList.contains(id) -> NumberItem(id, holder.get() as? Number)
            else -> ListItemBlock(Additional.UNDEFINED)
        }
        block.addItem(item)
    }
}

class PersonalizeConfigToCardConfig : Converter<PersonalizeConfig, CardConfig> {

    override fun convert(from: PersonalizeConfig): CardConfig {
        val signingMethod = com.tangem.commands.SigningMethod.build(
                signHash = from.SigningMethod0,
                signRaw = from.SigningMethod1,
                signHashValidatedByIssuer = from.SigningMethod2,
                signRawValidatedByIssuer = from.SigningMethod3,
                signHashValidatedByIssuerAndWriteIssuerData = from.SigningMethod4,
                signRawValidatedByIssuerAndWriteIssuerData = from.SigningMethod5,
                signPos = from.SigningMethod6
        )

        val isNote = from.cardData.product_note
        val isTag = from.cardData.product_tag
        val isIdCard = from.cardData.product_id_card
        val isIdIssuer = from.cardData.product_id_issuer

        val productMaskBuilder = ProductMaskBuilder()
        if (isNote) productMaskBuilder.add(com.tangem.commands.ProductMask.note)
        if (isTag) productMaskBuilder.add(com.tangem.commands.ProductMask.tag)
        if (isIdCard) productMaskBuilder.add(com.tangem.commands.ProductMask.idCard)
        if (isIdIssuer) productMaskBuilder.add(com.tangem.commands.ProductMask.idIssuer)
        val productMask = productMaskBuilder.build()

        var tokenSymbol: String? = null
        var tokenContractAddress: String? = null
        var tokenDecimal: Int? = null
        if (from.itsToken) {
            tokenSymbol = from.symbol
            tokenContractAddress = from.contractAddress
            tokenDecimal = from.decimal.toInt()
        }

        val blockchain = if (from.blockchain.isNotEmpty()) from.blockchain else from.blockchainCustom
        val cardData = CardData(
                blockchainName = blockchain,
                batchId = from.batchId,
                productMask = productMask,
                tokenSymbol = tokenSymbol,
                tokenContractAddress = tokenContractAddress,
                tokenDecimal = tokenDecimal,
                issuerName = null,
                manufactureDateTime = Calendar.getInstance().time,
                manufacturerSignature = null)


        val ndefs = mutableListOf<NdefRecord>()
        when (from.aar) {
            "None" -> null
            "--- CUSTOM ---" -> NdefRecord(NdefRecord.Type.AAR, from.aarCustom)
            else -> NdefRecord(NdefRecord.Type.AAR, from.aar)
        }?.let { ndefs.add(it) }

        return CardConfig(
                "Tangem",
                "Tangem Test",
                from.series,
                from.startNumber,
                1000,
                from.PIN,
                from.PIN2,
                from.PIN3,
                from.hexCrExKey,
                from.CVC,
                from.pauseBeforePIN2.toInt(),
                from.smartSecurityDelay,
                EllipticCurve.byName(from.curveID) ?: EllipticCurve.Secp256k1,
                signingMethod,
                from.MaxSignatures.toInt(),
                from.isReusable,
                from.allowSwapPIN,
                from.allowSwapPIN2,
                from.useActivation,
                from.useCVC,
                from.useNDEF,
                from.useDynamicNDEF,
                from.oneApdu,
                from.useBlock,
                from.allowSelectBlockchain,
                from.forbidPurgeWallet,
                from.protocolAllowUnencrypted,
                from.protocolAllowStaticEncryption,
                from.protectIssuerDataAgainstReplay,
                from.forbidDefaultPIN,
                from.disablePrecomputedNDEF,
                from.skipSecurityDelayIfValidatedByIssuer,
                from.skipCheckPIN2andCVCIfValidatedByIssuer,
                from.skipSecurityDelayIfValidatedByLinkedTerminal,
                from.restrictOverwriteIssuerDataEx,
                from.requireTerminalTxSignature,
                from.requireTerminalCertSignature,
                from.checkPIN3onCard,
                from.createWallet,
                cardData,
                ndefs
        )
    }
}