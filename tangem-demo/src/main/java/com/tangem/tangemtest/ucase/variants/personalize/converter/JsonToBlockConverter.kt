package com.tangem.tangemtest.ucase.variants.personalize.converter

import com.tangem.tangemtest._arch.structure.*
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.ucase.variants.personalize.dto.TestJsonDto
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

/**
[REDACTED_AUTHOR]
 */
class JsonToBlockConverter(
        private val valuesHolder: IdToJsonValues
) : Converter<TestJsonDto, List<Block>> {

    override fun convert(from: TestJsonDto): List<Block> {
        valuesHolder.init(from)
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
        val block = createBlock(BlockId.CARD_NUMBER)
        mutableListOf(
                CardNumber.SERIES,
                CardNumber.NUMBER
        ).forEach { createItem(block, it as Id) }
        return block
    }

    private fun common(): Block {
        val block = createBlock(BlockId.COMMON)
        mutableListOf(
                Common.CURVE,
                Common.BLOCKCHAIN,
                Common.BLOCKCHAIN_CUSTOM,
                Common.MAX_SIGNATURES,
                Common.CREATE_WALLET
        ).forEach { createItem(block, it) }
        return block
    }

    private fun signingMethod(): Block {
        val block = createBlock(BlockId.SIGNING_METHOD)
        mutableListOf(
                SigningMethod.SIGN_TX,
                SigningMethod.SIGN_TX_RAW,
                SigningMethod.SIGN_VALIDATED_TX,
                SigningMethod.SIGN_VALIDATED_TX_RAW,
                SigningMethod.SIGN_VALIDATED_TX_ISSUER,
                SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER,
                SigningMethod.SIGN_EXTERNAL
        ).forEach { createItem(block, it) }
        return block
    }

    private fun signHashExProperties(): Block {
        val block = createBlock(BlockId.SIGN_HASH_EX_PROP)
        mutableListOf(
                SignHashExProp.PIN_LESS_FLOOR_LIMIT,
                SignHashExProp.CRYPTO_EXTRACT_KEY,
                SignHashExProp.REQUIRE_TERMINAL_CERT_SIG,
                SignHashExProp.REQUIRE_TERMINAL_TX_SIG,
                SignHashExProp.CHECK_PIN3
        ).forEach { createItem(block, it) }
        return block
    }

    private fun denomination(): Block {
        val block = createBlock(BlockId.DENOMINATION)
        mutableListOf(
                Denomination.WRITE_ON_PERSONALIZE,
                Denomination.DENOMINATION
        ).forEach { createItem(block, it) }
        return block
    }

    private fun token(): Block {
        val block = createBlock(BlockId.TOKEN)
        mutableListOf(
                Token.ITS_TOKEN,
                Token.SYMBOL,
                Token.CONTRACT_ADDRESS,
                Token.DECIMAL
        ).forEach { createItem(block, it) }
        return block
    }

    private fun productMask(): Block {
        val block = createBlock(BlockId.PROD_MASK)
        mutableListOf(
                ProductMask.NOTE,
                ProductMask.TAG,
                ProductMask.ID_CARD
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMask(): Block {
        val block = createBlock(BlockId.SETTINGS_MASK)
        mutableListOf(
                SettingsMask.IS_REUSABLE,
                SettingsMask.NEED_ACTIVATION,
                SettingsMask.FORBID_PURGE,
                SettingsMask.ALLOW_SELECT_BLOCKCHAIN,
                SettingsMask.USE_BLOCK,
                SettingsMask.ONE_APDU,
                SettingsMask.USE_CVC,
                SettingsMask.ALLOW_SWAP_PIN,
                SettingsMask.ALLOW_SWAP_PIN2,
                SettingsMask.FORBID_DEFAULT_PIN,
                SettingsMask.SMART_SECURITY_DELAY,
                SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY,
                SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED,
                SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED,
                SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL,
                SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA
        ).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskProtocolEnc(): Block {
        val block = createBlock(BlockId.SETTINGS_MASK_PROTOCOL_ENC)
        mutableListOf(
                SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED,
                SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION).forEach { createItem(block, it) }
        return block
    }

    private fun settingsMaskNdef(): Block {
        val block = createBlock(BlockId.SETTINGS_MASK_NDEF)
        mutableListOf(
                SettingsMaskNdef.USE_NDEF,
                SettingsMaskNdef.DYNAMIC_NDEF,
                SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF,
                SettingsMaskNdef.AAR
        ).forEach { createItem(block, it) }
        return block
    }

    private fun pins(): Block {
        val block = createBlock(BlockId.PINS)
        mutableListOf(Pins.PIN, Pins.PIN2, Pins.PIN3, Pins.CVC, Pins.PAUSE_BEFORE_PIN2)
                .forEach { createItem(block, it) }
        return block
    }

    private fun createBlock(id: Id): ListItemBlock {
        return ListItemBlock(id).apply { createItem(this, id) }
    }

    private fun addPayload(block: Block, from: TestJsonDto) {
        block.payload[Additional.JSON_INCOMING.name] = from
        block.payload[Additional.JSON_TAILS.name] = JsonTails(
                from.count,
                from.numberFormat,
                from.issuerData,
                from.releaseVersion,
                from.issuerName
        )
    }

    private fun createItem(block: ListItemBlock, id: Id) {
        val holder = valuesHolder.get(id) ?: return
        val item = when {
            IdItemHelper.blockIdList.contains(id) -> TextItem(id, holder.value as? String)
            IdItemHelper.listItemList.contains(id) -> ListItem(id, holder.list as List<KeyValue>, holder.value)
            IdItemHelper.boolList.contains(id) -> BoolItem(id, holder.value as? Boolean)
            IdItemHelper.editTextList.contains(id) -> EditTextItem(id, holder.value as? String)
            IdItemHelper.numberList.contains(id) -> NumberItem(id, holder.value as? Number)
            else -> ListItemBlock(Additional.UNDEFINED)
        }
        block.addItem(item)
    }
}

class IdItemHelper {
    companion object {
        val blockIdList = mutableListOf<BlockId>(
                BlockId.CARD_NUMBER, BlockId.COMMON, BlockId.SIGNING_METHOD, BlockId.SIGN_HASH_EX_PROP, BlockId.DENOMINATION,
                BlockId.TOKEN, BlockId.PROD_MASK, BlockId.SETTINGS_MASK, BlockId.SETTINGS_MASK_PROTOCOL_ENC,
                BlockId.SETTINGS_MASK_NDEF, BlockId.PINS
        )

        val listItemList = mutableListOf(Common.CURVE, Common.BLOCKCHAIN, SettingsMaskNdef.AAR, Pins.PAUSE_BEFORE_PIN2)

        val boolList = mutableListOf(
                Common.CREATE_WALLET, SigningMethod.SIGN_TX, SigningMethod.SIGN_TX_RAW, SigningMethod.SIGN_VALIDATED_TX,
                SigningMethod.SIGN_VALIDATED_TX_RAW, SigningMethod.SIGN_VALIDATED_TX_ISSUER,
                SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER, SigningMethod.SIGN_EXTERNAL, SignHashExProp.REQUIRE_TERMINAL_CERT_SIG,
                SignHashExProp.REQUIRE_TERMINAL_TX_SIG, SignHashExProp.CHECK_PIN3, Denomination.WRITE_ON_PERSONALIZE, Token.ITS_TOKEN,
                ProductMask.NOTE, ProductMask.TAG, ProductMask.ID_CARD, SettingsMask.IS_REUSABLE, SettingsMask.NEED_ACTIVATION,
                SettingsMask.FORBID_PURGE, SettingsMask.ALLOW_SELECT_BLOCKCHAIN, SettingsMask.USE_BLOCK, SettingsMask.ONE_APDU,
                SettingsMask.USE_CVC, SettingsMask.ALLOW_SWAP_PIN, SettingsMask.ALLOW_SWAP_PIN2, SettingsMask.FORBID_DEFAULT_PIN,
                SettingsMask.SMART_SECURITY_DELAY, SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY,
                SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED, SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED,
                SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL, SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA,
                SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED, SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION,
                SettingsMaskNdef.USE_NDEF, SettingsMaskNdef.DYNAMIC_NDEF, SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF
        )

        val editTextList = mutableListOf(
                CardNumber.SERIES, Common.BLOCKCHAIN_CUSTOM, SignHashExProp.CRYPTO_EXTRACT_KEY, Token.SYMBOL,
                Token.CONTRACT_ADDRESS, Pins.PIN, Pins.PIN2, Pins.PIN3, Pins.CVC
        )

        val numberList = mutableListOf(
                CardNumber.NUMBER, Common.MAX_SIGNATURES, SignHashExProp.PIN_LESS_FLOOR_LIMIT, Denomination.DENOMINATION, Token.DECIMAL
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