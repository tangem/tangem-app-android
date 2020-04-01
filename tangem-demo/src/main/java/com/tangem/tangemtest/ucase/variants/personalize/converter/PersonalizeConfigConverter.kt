package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.commands.CardData
import com.tangem.commands.EllipticCurve
import com.tangem.commands.ProductMaskBuilder
import com.tangem.commands.personalization.entities.CardConfig
import com.tangem.commands.personalization.entities.NdefRecord
import com.tangem.tangemtest._arch.structure.Additional
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.ucase.variants.personalize.*
import com.tangem.tangemtest.ucase.variants.personalize.dto.PersonalizeConfig
import java.util.*

/**
[REDACTED_AUTHOR]
 */
class PersonalizeConfigConverter {

    private val associations: IdToValueAssociations = IdToValueAssociations()
    private val valueMapper = ValueMapper()

    fun toBlock(from: PersonalizeConfig): List<Block> {
        associations.init(from)
        val blocList = mutableListOf<Block>()
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
        return blocList
    }

    private fun cardNumber(): Block {
        val block = createBlock(BlockId.CardNumber)
        mutableListOf(
                CardNumber.Series,
                CardNumber.Number
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
                ProductMask.CardId
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
                SettingsMaskProtocolEnc.AlloFastEncryption).forEach { createItem(block, it) }
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
        block.payload[Additional.JSON_INCOMING.name] = from
    }

    private fun createItem(block: ListItemBlock, id: Id) {
        val holder = associations.get(id) ?: return
        val item = when {
            IdItemHelper.blockIdList.contains(id) -> TextItem(id, holder.get() as? String)
            IdItemHelper.listItemList.contains(id) -> ListItem(id, holder.list as List<KeyValue>, holder.get())
            IdItemHelper.boolList.contains(id) -> BoolItem(id, holder.get() as? Boolean)
            IdItemHelper.editTextList.contains(id) -> EditTextItem(id, holder.get() as? String)
            IdItemHelper.numberList.contains(id) -> NumberItem(id, holder.get() as? Number)
            else -> ListItemBlock(Additional.UNDEFINED)
        }
        block.addItem(item)
    }

    fun toConfig(blockList: List<Block>, defaultConfig: PersonalizeConfig): PersonalizeConfig {
        return valueMapper.mapOnObject(blockList, defaultConfig)
    }

    fun createCardConfig(applyConfig: PersonalizeConfig): CardConfig {
        val signingMethod = com.tangem.commands.SigningMethod.build(
                signHash = applyConfig.SigningMethod0,
                signRaw = applyConfig.SigningMethod1,
                signHashValidatedByIssuer = applyConfig.SigningMethod2,
                signRawValidatedByIssuer = applyConfig.SigningMethod3,
                signHashValidatedByIssuerAndWriteIssuerData = applyConfig.SigningMethod4,
                signRawValidatedByIssuerAndWriteIssuerData = applyConfig.SigningMethod5,
                signPos = applyConfig.SigningMethod6
        )

        val isNote = applyConfig.cardData.product_note
        val isTag = applyConfig.cardData.product_tag
        val isIdCard = applyConfig.cardData.product_id_card

        val productMaskBuilder = ProductMaskBuilder()
        if (isNote) productMaskBuilder.add(com.tangem.commands.ProductMask.note)
        if (isTag) productMaskBuilder.add(com.tangem.commands.ProductMask.tag)
        if (isIdCard) productMaskBuilder.add(com.tangem.commands.ProductMask.idCard)
        val productMask = productMaskBuilder.build()

        var tokenSymbol: String? = null
        var tokenContractAddress: String? = null
        var tokenDecimal: Int? = null
        if (applyConfig.itsToken) {
            tokenSymbol = applyConfig.symbol
            tokenContractAddress = applyConfig.contractAddress
            tokenDecimal = applyConfig.decimal.toInt()
        }

        val cardData = CardData(
                blockchainName = applyConfig.blockchain,
                batchId = applyConfig.cardData.batch,
                productMask = productMask,
                tokenSymbol = tokenSymbol,
                tokenContractAddress = tokenContractAddress,
                tokenDecimal = tokenDecimal,
                issuerName = null,
                manufactureDateTime = Calendar.getInstance().time,
                manufacturerSignature = null)


        val ndefs = mutableListOf<NdefRecord>()
        when (applyConfig.aar) {
            "None" -> null
            "--- CUSTOM ---" -> NdefRecord(NdefRecord.Type.AAR, applyConfig.aarCustom)
            else -> NdefRecord(NdefRecord.Type.AAR, applyConfig.aar)
        }?.let { ndefs.add(it) }

        return CardConfig(
                "Tangem",
                "Tangem Test",
                applyConfig.series,
                applyConfig.startNumber,
                1000,
                applyConfig.PIN,
                applyConfig.PIN2,
                applyConfig.PIN3,
                applyConfig.hexCrExKey,
                applyConfig.CVC,
                applyConfig.pauseBeforePIN2.toInt(),
                applyConfig.smartSecurityDelay,
                EllipticCurve.byName(applyConfig.curveID) ?: EllipticCurve.Secp256k1,
                signingMethod,
                applyConfig.MaxSignatures.toInt(),
                applyConfig.isReusable,
                applyConfig.allowSwapPIN,
                applyConfig.allowSwapPIN2,
                applyConfig.useActivation,
                applyConfig.useCVC,
                applyConfig.useNDEF,
                applyConfig.useDynamicNDEF,
                applyConfig.oneApdu,
                applyConfig.useBlock,
                applyConfig.allowSelectBlockchain,
                applyConfig.forbidPurgeWallet,
                applyConfig.protocolAllowUnencrypted,
                applyConfig.protocolAllowStaticEncryption,
                applyConfig.protectIssuerDataAgainstReplay,
                applyConfig.forbidDefaultPIN,
                applyConfig.disablePrecomputedNDEF,
                applyConfig.skipSecurityDelayIfValidatedByIssuer,
                applyConfig.skipCheckPIN2andCVCIfValidatedByIssuer,
                applyConfig.skipSecurityDelayIfValidatedByLinkedTerminal,
                applyConfig.restrictOverwriteIssuerDataEx,
                applyConfig.requireTerminalTxSignature,
                applyConfig.requireTerminalCertSignature,
                applyConfig.checkPIN3onCard,
                applyConfig.createWallet,
                cardData,
                ndefs
        )
    }
}

class IdItemHelper {
    companion object {
        val blockIdList = mutableListOf<com.tangem.tangemtest.ucase.variants.personalize.BlockId>(
                BlockId.CardNumber, BlockId.Common, BlockId.SigningMethod, BlockId.SignHashExProp, BlockId.Denomination,
                BlockId.Token, BlockId.ProdMask, BlockId.SettingsMask, BlockId.SettingsMaskProtocolEnc,
                BlockId.SettingsMaskNdef, BlockId.Pins
        )

        val listItemList = mutableListOf(Common.Curve, Common.Blockchain, SettingsMaskNdef.Aar, Pins.PauseBeforePin2)

        val boolList = mutableListOf(
                Common.CreateWallet, SigningMethod.SignTx, SigningMethod.SignTxRaw, SigningMethod.SignValidatedTx,
                SigningMethod.SignValidatedTxRaw, SigningMethod.SignValidatedTxIssuer,
                SigningMethod.SignValidatedTxRawIssuer, SigningMethod.SignExternal, SignHashExProp.RequireTerminalCertSig,
                SignHashExProp.RequireTerminalTxSig, SignHashExProp.CheckPin3, Denomination.WriteOnPersonalize, Token.ItsToken,
                ProductMask.Note, ProductMask.Tag, ProductMask.CardId, SettingsMask.IsReusable, SettingsMask.NeedActivation,
                SettingsMask.ForbidPurge, SettingsMask.AllowSelectBlockchain, SettingsMask.UseBlock, SettingsMask.OneApdu,
                SettingsMask.UseCvc, SettingsMask.AllowSwapPin, SettingsMask.AllowSwapPin2, SettingsMask.ForbidDefaultPin,
                SettingsMask.SmartSecurityDelay, SettingsMask.ProtectIssuerDataAgainstReplay,
                SettingsMask.SkipSecurityDelayIfValidated, SettingsMask.SkipPin2CvcIfValidated,
                SettingsMask.SkipSecurityDelayOnLinkedTerminal, SettingsMask.RestrictOverwriteExtraIssuerData,
                SettingsMaskProtocolEnc.AllowUnencrypted, SettingsMaskProtocolEnc.AlloFastEncryption,
                SettingsMaskNdef.UseNdef, SettingsMaskNdef.DynamicNdef, SettingsMaskNdef.DisablePrecomputedNdef
        )

        val editTextList = mutableListOf(
                CardNumber.Series, Common.BlockchainCustom, SignHashExProp.CryptoExKey, Token.Symbol,
                Token.ContractAddress, Pins.Pin, Pins.Pin2, Pins.Pin3, Pins.Cvc, SettingsMaskNdef.AarCustom
        )

        val numberList = mutableListOf(
                CardNumber.Number, Common.MaxSignatures, SignHashExProp.PinLessFloorLimit, Denomination.Denomination, Token.Decimal
        )
    }
}

class JsonTails(
        val count: Int,
        val numberFormat: String,
        val issuerData: Any?,
        val releaseVersion: Boolean,
        val issuerName: String
)