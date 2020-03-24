package com.tangem.tangemtest.card_use_cases.ui.personalize

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.*

/**
[REDACTED_AUTHOR]
 */
data class UnitInfo(
        val resName: Int,
        val resDescription: Int? = null
)

object InfoHolder {
    private val map = mutableMapOf<Id, UnitInfo>()

    init {
        InfoInitializer().init(map)
    }

    fun getInfo(id: Id): UnitInfo = map[id] ?: UnitInfo(R.string.unknown, R.string.unknown)
}

internal class InfoInitializer() {
    fun init(map: MutableMap<Id, UnitInfo>) {
        initBlock(map)
        initCardNumber(map)
        initCommon(map)
        initSigningMethod(map)
        initSignHashExProp(map)
        initDenomination(map)
        initToken(map)
        initProductMask(map)
        initSettingsMask(map)
        initSettingsMaskProtocolEnc(map)
        initSettingsMaskNde(map)
        initPins(map)
    }

    private fun initBlock(map: MutableMap<Id, UnitInfo>) {
        map[BlockId.CARD_NUMBER] = UnitInfo(
                R.string.pers_block_card_number,
                R.string.info_
        )
        map[BlockId.COMMON] = UnitInfo(
                R.string.pers_block_common,
                R.string.info_
        )
        map[BlockId.SIGNING_METHOD] = UnitInfo(
                R.string.pers_block_signing_method,
                R.string.info_
        )
        map[BlockId.SIGN_HASH_EX_PROP] = UnitInfo(
                R.string.pers_block_sign_hash_ex_prop,
                R.string.info_
        )
        map[BlockId.DENOMINATION] = UnitInfo(
                R.string.pers_block_denomination,
                R.string.info_
        )
        map[BlockId.TOKEN] = UnitInfo(
                R.string.pers_block_token,
                R.string.info_
        )
        map[BlockId.PROD_MASK] = UnitInfo(
                R.string.pers_block_product_mask,
                R.string.info_
        )
        map[BlockId.SETTINGS_MASK] = UnitInfo(
                R.string.pers_block_settings_mask,
                R.string.info_
        )
        map[BlockId.SETTINGS_MASK_PROTOCOL_ENC] = UnitInfo(
                R.string.pers_block_settings_mask_protocol_enc,
                R.string.info_
        )
        map[BlockId.SETTINGS_MASK_NDEF] = UnitInfo(
                R.string.pers_block_settings_mask_ndef,
                R.string.info_
        )
        map[BlockId.PINS] = UnitInfo(
                R.string.pers_block_pins,
                R.string.info_
        )
    }

    private fun initCardNumber(map: MutableMap<Id, UnitInfo>) {
        map[CardNumber.SERIES] = UnitInfo(
                R.string.pers_item_series,
                R.string.info_
        )
        map[CardNumber.NUMBER] = UnitInfo(
                R.string.pers_item_number,
                R.string.info_
        )
    }

    private fun initCommon(map: MutableMap<Id, UnitInfo>) {
        map[Common.CURVE] = UnitInfo(
                R.string.pers_item_curve,
                R.string.info_
        )
        map[Common.BLOCKCHAIN] = UnitInfo(
                R.string.pers_item_blockchain,
                R.string.info_
        )
        map[Common.BLOCKCHAIN_CUSTOM] = UnitInfo(
                R.string.pers_item_custom_blockchain,
                R.string.info_
        )
        map[Common.MAX_SIGNATURES] = UnitInfo(
                R.string.pers_item_max_signatures,
                R.string.info_
        )
        map[Common.CREATE_WALLET] = UnitInfo(
                R.string.pers_item_create_wallet,
                R.string.info_
        )
    }

    private fun initSigningMethod(map: MutableMap<Id, UnitInfo>) {
        map[SigningMethod.SIGN_TX] = UnitInfo(
                R.string.pers_item_sign_tx_hashes,
                R.string.info_
        )
        map[SigningMethod.SIGN_TX_RAW] = UnitInfo(
                R.string.pers_item_sign_raw_tx,
                R.string.info_
        )
        map[SigningMethod.SIGN_VALIDATED_TX] = UnitInfo(
                R.string.pers_item_sign_validated_tx_hashes,
                R.string.info_
        )
        map[SigningMethod.SIGN_VALIDATED_TX_RAW] = UnitInfo(
                R.string.pers_item_sign_validated_raw_tx,
                R.string.info_
        )
        map[SigningMethod.SIGN_VALIDATED_TX_ISSUER] = UnitInfo(
                R.string.pers_item_sign_validated_tx_hashes_with_iss_data,
                R.string.info_
        )
        map[SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER] = UnitInfo(
                R.string.pers_item_sign_validated_raw_tx_with_iss_data,
                R.string.info_
        )
        map[SigningMethod.SIGN_EXTERNAL] = UnitInfo(
                R.string.pers_item_sign_hash_ex,
                R.string.info_
        )
    }

    private fun initSignHashExProp(map: MutableMap<Id, UnitInfo>) {
        map[SignHashExProp.PIN_LESS_FLOOR_LIMIT] = UnitInfo(
                R.string.pers_item_pin_less_floor_limit,
                R.string.info_
        )
        map[SignHashExProp.CRYPTO_EXTRACT_KEY] = UnitInfo(
                R.string.pers_item_cr_ex_key,
                R.string.info_
        )
        map[SignHashExProp.REQUIRE_TERMINAL_CERT_SIG] = UnitInfo(
                R.string.pers_item_require_terminal_cert_sig,
                R.string.info_
        )
        map[SignHashExProp.REQUIRE_TERMINAL_TX_SIG] = UnitInfo(
                R.string.pers_item_require_terminal_tx_sig,
                R.string.info_
        )
        map[SignHashExProp.CHECK_PIN3] = UnitInfo(
                R.string.pers_item_pin3,
                R.string.info_
        )
    }

    private fun initDenomination(map: MutableMap<Id, UnitInfo>) {
        map[Denomination.WRITE_ON_PERSONALIZE] = UnitInfo(
                R.string.pers_item_write_on_personalize,
                R.string.info_
        )
        map[Denomination.DENOMINATION] = UnitInfo(
                R.string.pers_item_denomination,
                R.string.info_
        )
    }

    private fun initToken(map: MutableMap<Id, UnitInfo>) {
        map[Token.ITS_TOKEN] = UnitInfo(
                R.string.pers_item_its_token,
                R.string.info_
        )
        map[Token.SYMBOL] = UnitInfo(
                R.string.pers_item_symbol,
                R.string.info_
        )
        map[Token.CONTRACT_ADDRESS] = UnitInfo(
                R.string.pers_item_contract_address,
                R.string.info_
        )
        map[Token.DECIMAL] = UnitInfo(
                R.string.pers_item_decimal,
                R.string.info_
        )
    }

    private fun initProductMask(map: MutableMap<Id, UnitInfo>) {
        map[ProductMask.NOTE] = UnitInfo(
                R.string.pers_item_note,
                R.string.info_
        )
        map[ProductMask.TAG] = UnitInfo(
                R.string.pers_item_tag,
                R.string.info_
        )
        map[ProductMask.ID_CARD] = UnitInfo(
                R.string.pers_item_id_card,
                R.string.info_
        )
    }

    private fun initSettingsMask(map: MutableMap<Id, UnitInfo>) {
        map[SettingsMask.IS_REUSABLE] = UnitInfo(
                R.string.pers_item_is_reusable,
                R.string.info_
        )
        map[SettingsMask.NEED_ACTIVATION] = UnitInfo(
                R.string.pers_item_need_activation,
                R.string.info_
        )
        map[SettingsMask.FORBID_PURGE] = UnitInfo(
                R.string.pers_item_forbid_purge,
                R.string.info_
        )
        map[SettingsMask.ALLOW_SELECT_BLOCKCHAIN] = UnitInfo(
                R.string.pers_item_allow_select_blockchain,
                R.string.info_
        )
        map[SettingsMask.USE_BLOCK] = UnitInfo(
                R.string.pers_item_use_block,
                R.string.info_
        )
        map[SettingsMask.ONE_APDU] = UnitInfo(
                R.string.pers_item_one_apdu_at_once,
                R.string.info_
        )
        map[SettingsMask.USE_CVC] = UnitInfo(
                R.string.pers_item_use_cvc,
                R.string.info_
        )
        map[SettingsMask.ALLOW_SWAP_PIN] = UnitInfo(
                R.string.pers_item_allow_swap_pin,
                R.string.info_
        )
        map[SettingsMask.ALLOW_SWAP_PIN2] = UnitInfo(
                R.string.pers_item_allow_swap_pin2,
                R.string.info_
        )
        map[SettingsMask.FORBID_DEFAULT_PIN] = UnitInfo(
                R.string.pers_item_forbid_default_pin,
                R.string.info_
        )
        map[SettingsMask.SMART_SECURITY_DELAY] = UnitInfo(
                R.string.pers_item_smart_security_delay,
                R.string.info_
        )
        map[SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY] = UnitInfo(
                R.string.pers_item_protect_issuer_data_against_replay,
                R.string.info_
        )
        map[SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED] = UnitInfo(
                R.string.pers_item_skip_security_delay_if_validated,
                R.string.info_
        )
        map[SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED] = UnitInfo(
                R.string.pers_item_skip_pin2_and_cvc_if_validated,
                R.string.info_
        )
        map[SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL] = UnitInfo(
                R.string.pers_item_skip_security_delay_on_linked_terminal,
                R.string.info_
        )
        map[SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA] = UnitInfo(
                R.string.pers_item_restrict_overwrite_ex_issuer_data,
                R.string.info_
        )
    }

    private fun initSettingsMaskProtocolEnc(map: MutableMap<Id, UnitInfo>) {
        map[SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED] = UnitInfo(
                R.string.pers_item_allow_unencrypted,
                R.string.info_
        )
        map[SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION] = UnitInfo(
                R.string.pers_item_allow_fast_encryption,
                R.string.info_
        )
    }

    private fun initSettingsMaskNde(map: MutableMap<Id, UnitInfo>) {
        map[SettingsMaskNdef.USE_NDEF] = UnitInfo(
                R.string.pers_item_use_ndef,
                R.string.info_
        )
        map[SettingsMaskNdef.DYNAMIC_NDEF] = UnitInfo(
                R.string.pers_item_dynamic_ndef,
                R.string.info_
        )
        map[SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF] = UnitInfo(
                R.string.pers_item_disable_precomputed_ndef,
                R.string.info_
        )
        map[SettingsMaskNdef.AAR] = UnitInfo(
                R.string.pers_item_aar,
                R.string.info_
        )
    }

    private fun initPins(map: MutableMap<Id, UnitInfo>) {
        map[Pins.PIN] = UnitInfo(
                R.string.pers_item_pin,
                R.string.info_
        )
        map[Pins.PIN2] = UnitInfo(
                R.string.pers_item_pin2,
                R.string.info_
        )
        map[Pins.PIN3] = UnitInfo(
                R.string.pers_item_pin3,
                R.string.info_
        )
        map[Pins.CVC] = UnitInfo(
                R.string.pers_item_cvc,
                R.string.info_
        )
        map[Pins.PAUSE_BEFORE_PIN2] = UnitInfo(
                R.string.pers_item_pause_before_pin2,
                R.string.info_
        )
    }
}