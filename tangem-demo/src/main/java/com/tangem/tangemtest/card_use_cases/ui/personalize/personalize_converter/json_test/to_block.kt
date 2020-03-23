package com.tangem.tangemtest.card_use_cases.ui.personalize.personalize_converter.json_test

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.Block
import com.tangem.tangemtest._arch.structure.impl.*
import ru.dev.gbixahue.eu4d.lib.kotlin.common.Converter

class JsonToBlockConverter : Converter<TestJsonDto, Block> {

    override fun convert(from: TestJsonDto): Block {
        val initialBlock = LinearBlock()
        initialBlock.addItem(cardNumber(from))
        initialBlock.addItem(common(from))
        initialBlock.addItem(signingMethod(from))
        initialBlock.addItem(signHashExProperties(from))
        initialBlock.addItem(denomination(from))
        initialBlock.addItem(token(from))
        initialBlock.addItem(productMask(from))
        initialBlock.addItem(settingsMask(from))
        initialBlock.addItem(settingsMaskProtocolEnc(from))
        initialBlock.addItem(cardNumber(from))
        initialBlock.addItem(settingsMaskNdef(from))
        initialBlock.addItem(pins(from))
        addPayload(initialBlock, from)
        return initialBlock
    }

    private fun cardNumber(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_card_number))
        block.addItem(EditTextUnit(from.series).resName(R.string.pers_item_series))
        block.addItem(NumberUnit(from.startNumber).resName(R.string.pers_item_number))
        return block
    }

    private fun common(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_common))
        block.addItem(ListUnit(Helper.listOfCurves(), from.curveID).resName(R.string.pers_item_curve))
        block.addItem(ListUnit(Helper.listOfBlockchain(), from.blockchain.name).resName(R.string.pers_item_blockchain))
        block.addItem(EditTextUnit(from.blockchain.customName).resName(R.string.pers_item_custom_blockchain))
        block.addItem(NumberUnit(from.MaxSignatures).resName(R.string.pers_item_max_signatures))
        block.addItem(BoolUnit(from.createWalletB).resName(R.string.action_wallet_create))
        return block
    }

    private fun signingMethod(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_signing_method))
        block.addItem(BoolUnit(from.SigningMethod0).resName(R.string.pers_item_sign_tx_hashes))
        block.addItem(BoolUnit(from.SigningMethod1).resName(R.string.pers_item_sign_raw_tx))
        block.addItem(BoolUnit(from.SigningMethod2).resName(R.string.pers_item_sign_validated_tx_hashes))
        block.addItem(BoolUnit(from.SigningMethod3).resName(R.string.pers_item_sign_validated_raw_tx))
        block.addItem(BoolUnit(from.SigningMethod4).resName(R.string.pers_item_sign_validated_tx_hashes_with_iss_data))
        block.addItem(BoolUnit(from.SigningMethod5).resName(R.string.pers_item_sign_validated_raw_tx_with_iss_data))
        block.addItem(BoolUnit(from.SigningMethod6).resName(R.string.pers_item_sign_hash_ex))
        return block
    }

    private fun signHashExProperties(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_sign_hash_ex_prop))
        block.addItem(NumberUnit(from.pinLessFloorLimit).resName(R.string.pers_item_pin_less_floor_limit))
        block.addItem(EditTextUnit(from.hexCrExKey).resName(R.string.pers_item_cr_ex_key))
        block.addItem(BoolUnit(from.requireTerminalCertSignature).resName(R.string.pers_item_require_terminal_cert_sig))
        block.addItem(BoolUnit(from.requireTerminalTxSignature).resName(R.string.pers_item_require_terminal_tx_sig))
        block.addItem(BoolUnit(from.checkPIN3onCard).resName(R.string.pers_item_check_pin3_on_card))
        return block
    }

    private fun denomination(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_denomination))
        block.addItem(BoolUnit(from.writeOnPersonalization).resName(R.string.pers_item_write_on_personalize))
        block.addItem(NumberUnit(from.denomination).resName(R.string.pers_item_denomination))
        return block
    }

    private fun token(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_token))
        block.addItem(BoolUnit(from.itsToken).resName(R.string.pers_item_its_token))
        block.addItem(EditTextUnit(from.symbol).resName(R.string.pers_item_symbol))
        block.addItem(EditTextUnit(from.contractAddress).resName(R.string.pers_item_contract_address))
        block.addItem(NumberUnit(from.decimal).resName(R.string.pers_item_decimal))
        return block
    }

    private fun productMask(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_product_mask))
        block.addItem(BoolUnit(from.cardData.product_note).resName(R.string.pers_item_note))
        block.addItem(BoolUnit(from.cardData.product_tag).resName(R.string.pers_item_tag))
        block.addItem(BoolUnit(from.cardData.product_id_card).resName(R.string.pers_item_id_card))
        return block
    }

    private fun settingsMask(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_settings_mask))
        block.addItem(BoolUnit(from.isReusable).resName(R.string.pers_item_is_reusable))
        block.addItem(BoolUnit(from.useActivation).resName(R.string.pers_item_need_activation))
        block.addItem(BoolUnit(from.forbidPurgeWallet).resName(R.string.pers_item_forbid_purge))
        block.addItem(BoolUnit(from.allowSelectBlockchain).resName(R.string.pers_item_allow_select_blockchain))
        block.addItem(BoolUnit(from.useBlock).resName(R.string.pers_item_use_block))
        block.addItem(BoolUnit(from.useOneCommandAtTime).resName(R.string.pers_item_one_apdu_at_once))
        block.addItem(BoolUnit(from.useCVC).resName(R.string.pers_item_use_cvc))
        block.addItem(BoolUnit(from.allowSwapPIN).resName(R.string.pers_item_allow_swap_pin))
        block.addItem(BoolUnit(from.allowSwapPIN2).resName(R.string.pers_item_allow_swap_pin2))
        block.addItem(BoolUnit(from.forbidDefaultPIN).resName(R.string.pers_item_forbid_default_pin))
        block.addItem(BoolUnit(from.smartSecurityDelay).resName(R.string.pers_item_smart_security_delay))
        block.addItem(BoolUnit(from.protectIssuerDataAgainstReplay).resName(R.string.pers_item_protect_issuer_data_against_replay))
        block.addItem(BoolUnit(from.skipSecurityDelayIfValidatedByIssuer).resName(R.string.pers_item_skip_security_delay_if_validated))
        block.addItem(BoolUnit(from.skipCheckPIN2andCVCIfValidatedByIssuer).resName(R.string.pers_item_skip_pin2_and_cvc_if_validated))
        block.addItem(BoolUnit(from.skipSecurityDelayIfValidatedByLinkedTerminal).resName(R.string.pers_item_skip_security_delay_on_linked_terminal))
        block.addItem(BoolUnit(from.restrictOverwriteIssuerDataEx).resName(R.string.pers_item_restrict_overwrite_ex_issuer_data))
        return block
    }

    private fun settingsMaskProtocolEnc(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_settings_mask_protocol_enc))
        block.addItem(BoolUnit(from.protocolAllowUnencrypted).resName(R.string.pers_item_allow_unencrypted))
        block.addItem(BoolUnit(false).resName(R.string.pers_item_allow_fast_encryption))
        return block
    }

    private fun settingsMaskNdef(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_settings_mask_ndef))
        block.addItem(BoolUnit(from.useNDEF).resName(R.string.pers_item_use_ndef))
        block.addItem(BoolUnit(from.useDynamicNDEF).resName(R.string.pers_item_dynamic_ndef))
        block.addItem(BoolUnit(from.disablePrecomputedNDEF).resName(R.string.pers_item_disable_precomputed_ndef))
        block.addItem(ListUnit(Helper.aarList(), from.NDEF[0].type).resName(R.string.pers_item_aar))
//        block.addItem(TextItem(R.string.pers_item_custom_aar_package_name, from.ndef[0].value))
        return block
    }

    private fun pins(from: TestJsonDto): Block {
        val block = LinearBlock()
        block.addItem(TextUnit().resName(R.string.pers_block_pins))
        block.addItem(EditTextUnit(from.PIN).resName(R.string.pers_item_pin))
        block.addItem(EditTextUnit(from.PIN2).resName(R.string.pers_item_pin2))
        block.addItem(EditTextUnit(from.PIN3).resName(R.string.pers_item_pin3))
        block.addItem(EditTextUnit(from.CVC).resName(R.string.pers_item_cvc))
        block.addItem(ListUnit(Helper.pauseBeforePin(), from.pauseBeforePIN2).resName(R.string.pers_item_pause_before_pin2))
        return block
    }

    private fun addPayload(block: Block, from: TestJsonDto) {
        block.payload["count"] = from.count
        block.payload["numberFormat"] = from.numberFormat
        block.payload["issuerData"] = from.issuerData
        block.payload["releaseVersion"] = from.releaseVersion
        block.payload["issuerName"] = from.issuerName
    }
}

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