package com.tangem.tangemtest.cardUseCase.ui.personalize.converter

import com.tangem.tangemtest._arch.structure.*
import com.tangem.tangemtest._arch.structure.abstraction.Block
import com.tangem.tangemtest._arch.structure.abstraction.ListItemBlock
import com.tangem.tangemtest._arch.structure.impl.*
import com.tangem.tangemtest.cardUseCase.ui.personalize.dto.Ndef
import com.tangem.tangemtest.cardUseCase.ui.personalize.dto.TestJsonDto
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

class JsonToBlockConverter : Converter<TestJsonDto, List<Block>> {

    override fun convert(from: TestJsonDto): List<Block> {
        val blocList = mutableListOf<Block>()
        blocList.add(cardNumber(from))
        blocList.add(common(from))
        blocList.add(signingMethod(from))
        blocList.add(signHashExProperties(from))
        blocList.add(denomination(from))
        blocList.add(token(from))
        blocList.add(productMask(from))
        blocList.add(settingsMask(from))
        blocList.add(settingsMaskProtocolEnc(from))
        blocList.add(settingsMaskNdef(from))
        blocList.add(pins(from))
        val payloadBlock = ListItemBlock(Additional.JSON_TAILS)
        addPayload(payloadBlock, from)
        blocList.add(payloadBlock)
        return blocList
    }

    private fun cardNumber(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.CARD_NUMBER)
        block.addItem(TextItem(BlockId.CARD_NUMBER))
        block.addItem(EditTextItem(CardNumber.SERIES, from.series))
        block.addItem(NumberItem(CardNumber.NUMBER, from.startNumber))
        return block
    }

    private fun common(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.COMMON)
        block.addItem(TextItem(BlockId.COMMON))
        block.addItem(ListItem(Common.CURVE, Helper.listOfCurves(), from.curveID))
        block.addItem(ListItem(Common.BLOCKCHAIN, Helper.listOfBlockchain(), from.blockchain.name))
        block.addItem(EditTextItem(Common.BLOCKCHAIN_CUSTOM, from.blockchain.customName))
        block.addItem(NumberItem(Common.MAX_SIGNATURES, from.MaxSignatures))
        block.addItem(BoolItem(Common.CREATE_WALLET, from.createWalletB))
        return block
    }

    private fun signingMethod(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.SIGNING_METHOD)
        block.addItem(TextItem(BlockId.SIGNING_METHOD))
        block.addItem(BoolItem(SigningMethod.SIGN_TX, from.SigningMethod0))
        block.addItem(BoolItem(SigningMethod.SIGN_TX_RAW, from.SigningMethod1))
        block.addItem(BoolItem(SigningMethod.SIGN_VALIDATED_TX, from.SigningMethod2))
        block.addItem(BoolItem(SigningMethod.SIGN_VALIDATED_TX_RAW, from.SigningMethod3))
        block.addItem(BoolItem(SigningMethod.SIGN_VALIDATED_TX_ISSUER, from.SigningMethod4))
        block.addItem(BoolItem(SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER, from.SigningMethod5))
        block.addItem(BoolItem(SigningMethod.SIGN_EXTERNAL, from.SigningMethod6))
        return block
    }

    private fun signHashExProperties(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.SIGN_HASH_EX_PROP)
        block.addItem(TextItem(BlockId.SIGN_HASH_EX_PROP))
        block.addItem(NumberItem(SignHashExProp.PIN_LESS_FLOOR_LIMIT, from.pinLessFloorLimit))
        block.addItem(EditTextItem(SignHashExProp.CRYPTO_EXTRACT_KEY, from.hexCrExKey))
        block.addItem(BoolItem(SignHashExProp.REQUIRE_TERMINAL_CERT_SIG, from.requireTerminalCertSignature))
        block.addItem(BoolItem(SignHashExProp.REQUIRE_TERMINAL_TX_SIG, from.requireTerminalTxSignature))
        block.addItem(BoolItem(SignHashExProp.CHECK_PIN3, from.checkPIN3onCard))
        return block
    }

    private fun denomination(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.DENOMINATION)
        block.addItem(TextItem(BlockId.DENOMINATION))
        block.addItem(BoolItem(Denomination.WRITE_ON_PERSONALIZE, from.writeOnPersonalization))
        block.addItem(NumberItem(Denomination.DENOMINATION, from.denomination))
        return block
    }

    private fun token(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.TOKEN)
        block.addItem(TextItem(BlockId.TOKEN))
        block.addItem(BoolItem(Token.ITS_TOKEN, from.itsToken))
        block.addItem(EditTextItem(Token.SYMBOL, from.symbol))
        block.addItem(EditTextItem(Token.CONTRACT_ADDRESS, from.contractAddress))
        block.addItem(NumberItem(Token.DECIMAL, from.decimal))
        return block
    }

    private fun productMask(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.PROD_MASK)
        block.addItem(TextItem(BlockId.PROD_MASK))
        block.addItem(BoolItem(ProductMask.NOTE, from.cardData.product_note))
        block.addItem(BoolItem(ProductMask.TAG, from.cardData.product_tag))
        block.addItem(BoolItem(ProductMask.ID_CARD, from.cardData.product_id_card))
        return block
    }

    private fun settingsMask(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.SETTINGS_MASK)
        block.addItem(TextItem(BlockId.SETTINGS_MASK))
        block.addItem(BoolItem(SettingsMask.IS_REUSABLE, from.isReusable))
        block.addItem(BoolItem(SettingsMask.NEED_ACTIVATION, from.useActivation))
        block.addItem(BoolItem(SettingsMask.FORBID_PURGE, from.forbidPurgeWallet))
        block.addItem(BoolItem(SettingsMask.ALLOW_SELECT_BLOCKCHAIN, from.allowSelectBlockchain))
        block.addItem(BoolItem(SettingsMask.USE_BLOCK, from.useBlock))
        block.addItem(BoolItem(SettingsMask.ONE_APDU, from.useOneCommandAtTime))
        block.addItem(BoolItem(SettingsMask.USE_CVC, from.useCVC))
        block.addItem(BoolItem(SettingsMask.ALLOW_SWAP_PIN, from.allowSwapPIN))
        block.addItem(BoolItem(SettingsMask.ALLOW_SWAP_PIN2, from.allowSwapPIN2))
        block.addItem(BoolItem(SettingsMask.FORBID_DEFAULT_PIN, from.forbidDefaultPIN))
        block.addItem(BoolItem(SettingsMask.SMART_SECURITY_DELAY, from.smartSecurityDelay))
        block.addItem(BoolItem(SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY, from.protectIssuerDataAgainstReplay))
        block.addItem(BoolItem(SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED, from.skipSecurityDelayIfValidatedByIssuer))
        block.addItem(BoolItem(SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED, from.skipCheckPIN2andCVCIfValidatedByIssuer))
        block.addItem(BoolItem(SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL, from.skipSecurityDelayIfValidatedByLinkedTerminal))
        block.addItem(BoolItem(SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA, from.restrictOverwriteIssuerDataEx))
        return block
    }

    private fun settingsMaskProtocolEnc(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.SETTINGS_MASK_PROTOCOL_ENC)
        block.addItem(TextItem(BlockId.SETTINGS_MASK_PROTOCOL_ENC))
        block.addItem(BoolItem(SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED, from.protocolAllowUnencrypted))
        block.addItem(BoolItem(SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION, false))
        return block
    }

    private fun settingsMaskNdef(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.SETTINGS_MASK_NDEF)
        block.addItem(TextItem(BlockId.SETTINGS_MASK_NDEF))
        block.addItem(BoolItem(SettingsMaskNdef.USE_NDEF, from.useNDEF))
        block.addItem(BoolItem(SettingsMaskNdef.DYNAMIC_NDEF, from.useDynamicNDEF))
        block.addItem(BoolItem(SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF, from.disablePrecomputedNDEF))
        block.addItem(ListItem(SettingsMaskNdef.AAR, Helper.aarList(), from.NDEF[0].type))
        return block
    }

    private fun pins(from: TestJsonDto): Block {
        val block = ListItemBlock(BlockId.PINS)
        block.addItem(TextItem(BlockId.PINS))
        block.addItem(EditTextItem(Pins.PIN, from.PIN))
        block.addItem(EditTextItem(Pins.PIN2, from.PIN2))
        block.addItem(EditTextItem(Pins.PIN3, from.PIN3))
        block.addItem(EditTextItem(Pins.CVC, from.CVC))
        block.addItem(ListItem(Pins.PAUSE_BEFORE_PIN2, Helper.pauseBeforePin(), from.pauseBeforePIN2))
        return block
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
}

class JsonTails(
        val count: Int,
        val numberFormat: String,
        val issuerData: Any?,
        val releaseVersion: Boolean,
        val issuerName: String
)

internal class Helper {
    companion object {
        fun listOfCurves(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("secp256k1", "secp256k1"),
                    KeyValue("ed25519", "ed25519")
            )
        }

        fun listOfBlockchain(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("--- CUSTOM ---", "BTC/test"),
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
                    KeyValue("Release APP", "com.some.release"),
                    KeyValue("Debug APP", "com.some.debug"),
                    KeyValue("None", "")
            )
        }

        fun pauseBeforePin(): List<KeyValue> {
            return mutableListOf(
                    KeyValue("immediately", 0),
                    KeyValue("2 seconds", 2000),
                    KeyValue("5 seconds", 5000),
                    KeyValue("15 seconds", 15000),
                    KeyValue("30 seconds", 30000),
                    KeyValue("1 minute", 60000),
                    KeyValue("2 minute", 120000)
            )
        }

        fun ndef(ndef: MutableList<Ndef>): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            ndef.forEach { map[it.type] = it.value }
            return map
        }
    }
}