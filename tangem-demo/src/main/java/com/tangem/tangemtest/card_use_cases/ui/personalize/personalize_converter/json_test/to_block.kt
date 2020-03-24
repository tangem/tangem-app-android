package com.tangem.tangemtest.card_use_cases.ui.personalize.personalize_converter.json_test

import com.tangem.tangemtest._arch.structure.base.*
import com.tangem.tangemtest._arch.structure.impl.*
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
        val payloadBlock = LinearBlock(Additional.JSON_TAILS)
        addPayload(payloadBlock, from)
        blocList.add(payloadBlock)
        return blocList
    }

    private fun cardNumber(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.CARD_NUMBER)
        block.addItem(TextUnit(BlockId.CARD_NUMBER))
        block.addItem(EditTextUnit(CardNumber.SERIES, from.series))
        block.addItem(NumberUnit(CardNumber.NUMBER, from.startNumber))
        return block
    }

    private fun common(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.COMMON)
        block.addItem(TextUnit(BlockId.COMMON))
        block.addItem(ListUnit(Common.CURVE, Helper.listOfCurves(), from.curveID))
        block.addItem(ListUnit(Common.BLOCKCHAIN, Helper.listOfBlockchain(), from.blockchain.name))
        block.addItem(EditTextUnit(Common.BLOCKCHAIN_CUSTOM, from.blockchain.customName))
        block.addItem(NumberUnit(Common.MAX_SIGNATURES, from.MaxSignatures))
        block.addItem(BoolUnit(Common.CREATE_WALLET, from.createWalletB))
        return block
    }

    private fun signingMethod(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.SIGNING_METHOD)
        block.addItem(TextUnit(BlockId.SIGNING_METHOD))
        block.addItem(BoolUnit(SigningMethod.SIGN_TX, from.SigningMethod0))
        block.addItem(BoolUnit(SigningMethod.SIGN_TX_RAW, from.SigningMethod1))
        block.addItem(BoolUnit(SigningMethod.SIGN_VALIDATED_TX, from.SigningMethod2))
        block.addItem(BoolUnit(SigningMethod.SIGN_VALIDATED_TX_RAW, from.SigningMethod3))
        block.addItem(BoolUnit(SigningMethod.SIGN_VALIDATED_TX_ISSUER, from.SigningMethod4))
        block.addItem(BoolUnit(SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER, from.SigningMethod5))
        block.addItem(BoolUnit(SigningMethod.SIGN_EXTERNAL, from.SigningMethod6))
        return block
    }

    private fun signHashExProperties(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.SIGN_HASH_EX_PROP)
        block.addItem(TextUnit(BlockId.SIGN_HASH_EX_PROP))
        block.addItem(NumberUnit(SignHashExProp.PIN_LESS_FLOOR_LIMIT, from.pinLessFloorLimit))
        block.addItem(EditTextUnit(SignHashExProp.CRYPTO_EXTRACT_KEY, from.hexCrExKey))
        block.addItem(BoolUnit(SignHashExProp.REQUIRE_TERMINAL_CERT_SIG, from.requireTerminalCertSignature))
        block.addItem(BoolUnit(SignHashExProp.REQUIRE_TERMINAL_TX_SIG, from.requireTerminalTxSignature))
        block.addItem(BoolUnit(SignHashExProp.CHECK_PIN3, from.checkPIN3onCard))
        return block
    }

    private fun denomination(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.DENOMINATION)
        block.addItem(TextUnit(BlockId.DENOMINATION))
        block.addItem(BoolUnit(Denomination.WRITE_ON_PERSONALIZE, from.writeOnPersonalization))
        block.addItem(NumberUnit(Denomination.DENOMINATION, from.denomination))
        return block
    }

    private fun token(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.TOKEN)
        block.addItem(TextUnit(BlockId.TOKEN))
        block.addItem(BoolUnit(Token.ITS_TOKEN, from.itsToken))
        block.addItem(EditTextUnit(Token.SYMBOL, from.symbol))
        block.addItem(EditTextUnit(Token.CONTRACT_ADDRESS, from.contractAddress))
        block.addItem(NumberUnit(Token.DECIMAL, from.decimal))
        return block
    }

    private fun productMask(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.PROD_MASK)
        block.addItem(TextUnit(BlockId.PROD_MASK))
        block.addItem(BoolUnit(ProductMask.NOTE, from.cardData.product_note))
        block.addItem(BoolUnit(ProductMask.TAG, from.cardData.product_tag))
        block.addItem(BoolUnit(ProductMask.ID_CARD, from.cardData.product_id_card))
        return block
    }

    private fun settingsMask(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.SETTINGS_MASK)
        block.addItem(TextUnit(BlockId.SETTINGS_MASK))
        block.addItem(BoolUnit(SettingsMask.IS_REUSABLE, from.isReusable))
        block.addItem(BoolUnit(SettingsMask.NEED_ACTIVATION, from.useActivation))
        block.addItem(BoolUnit(SettingsMask.FORBID_PURGE, from.forbidPurgeWallet))
        block.addItem(BoolUnit(SettingsMask.ALLOW_SELECT_BLOCKCHAIN, from.allowSelectBlockchain))
        block.addItem(BoolUnit(SettingsMask.USE_BLOCK, from.useBlock))
        block.addItem(BoolUnit(SettingsMask.ONE_APDU, from.useOneCommandAtTime))
        block.addItem(BoolUnit(SettingsMask.USE_CVC, from.useCVC))
        block.addItem(BoolUnit(SettingsMask.ALLOW_SWAP_PIN, from.allowSwapPIN))
        block.addItem(BoolUnit(SettingsMask.ALLOW_SWAP_PIN2, from.allowSwapPIN2))
        block.addItem(BoolUnit(SettingsMask.FORBID_DEFAULT_PIN, from.forbidDefaultPIN))
        block.addItem(BoolUnit(SettingsMask.SMART_SECURITY_DELAY, from.smartSecurityDelay))
        block.addItem(BoolUnit(SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY, from.protectIssuerDataAgainstReplay))
        block.addItem(BoolUnit(SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED, from.skipSecurityDelayIfValidatedByIssuer))
        block.addItem(BoolUnit(SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED, from.skipCheckPIN2andCVCIfValidatedByIssuer))
        block.addItem(BoolUnit(SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL, from.skipSecurityDelayIfValidatedByLinkedTerminal))
        block.addItem(BoolUnit(SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA, from.restrictOverwriteIssuerDataEx))
        return block
    }

    private fun settingsMaskProtocolEnc(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.SETTINGS_MASK_PROTOCOL_ENC)
        block.addItem(TextUnit(BlockId.SETTINGS_MASK_PROTOCOL_ENC))
        block.addItem(BoolUnit(SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED, from.protocolAllowUnencrypted))
        block.addItem(BoolUnit(SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION, false))
        return block
    }

    private fun settingsMaskNdef(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.SETTINGS_MASK_NDEF)
        block.addItem(TextUnit(BlockId.SETTINGS_MASK_NDEF))
        block.addItem(BoolUnit(SettingsMaskNdef.USE_NDEF, from.useNDEF))
        block.addItem(BoolUnit(SettingsMaskNdef.DYNAMIC_NDEF, from.useDynamicNDEF))
        block.addItem(BoolUnit(SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF, from.disablePrecomputedNDEF))
        block.addItem(ListUnit(SettingsMaskNdef.AAR, Helper.aarList(), from.NDEF[0].type))
        return block
    }

    private fun pins(from: TestJsonDto): Block {
        val block = LinearBlock(BlockId.PINS)
        block.addItem(TextUnit(BlockId.PINS))
        block.addItem(EditTextUnit(Pins.PIN, from.PIN))
        block.addItem(EditTextUnit(Pins.PIN2, from.PIN2))
        block.addItem(EditTextUnit(Pins.PIN3, from.PIN3))
        block.addItem(EditTextUnit(Pins.CVC, from.CVC))
        block.addItem(ListUnit(Pins.PAUSE_BEFORE_PIN2, Helper.pauseBeforePin(), from.pauseBeforePIN2))
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