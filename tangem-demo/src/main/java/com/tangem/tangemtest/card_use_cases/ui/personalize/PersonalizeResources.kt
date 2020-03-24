package com.tangem.tangemtest.card_use_cases.ui.personalize

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.*

/**
[REDACTED_AUTHOR]
 */
data class UnitResource(
        val resName: Int,
        val resDescription: Int? = null
)

object PersonalizeResources {
    private val map = mutableMapOf<Id, UnitResource>()

    init {
        InfoInitializer().init(map)
    }

    fun get(id: Id): UnitResource = map[id] ?: UnitResource(R.string.unknown, R.string.unknown)
}

internal class InfoInitializer() {
    fun init(map: MutableMap<Id, UnitResource>) {
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

    private fun initBlock(map: MutableMap<Id, UnitResource>) {
        map[BlockId.CARD_NUMBER] = UnitResource(
                R.string.pers_block_card_number,
                R.string.info_pers_block_card_number
        )
        map[BlockId.COMMON] = UnitResource(
                R.string.pers_block_common,
                R.string.info_pers_block_common
        )
        map[BlockId.SIGNING_METHOD] = UnitResource(
                R.string.pers_block_signing_method,
                R.string.info_pers_block_signing_method
        )
        map[BlockId.SIGN_HASH_EX_PROP] = UnitResource(
                R.string.pers_block_sign_hash_ex_prop,
                R.string.info_pers_block_sign_hash_ex_prop
        )
        map[BlockId.DENOMINATION] = UnitResource(
                R.string.pers_block_denomination,
                R.string.info_pers_block_denomination
        )
        map[BlockId.TOKEN] = UnitResource(
                R.string.pers_block_token,
                R.string.info_pers_block_token
        )
        map[BlockId.PROD_MASK] = UnitResource(
                R.string.pers_block_product_mask,
                R.string.info_pers_block_product_mask
        )
        map[BlockId.SETTINGS_MASK] = UnitResource(
                R.string.pers_block_settings_mask,
                R.string.info_pers_block_settings_mask
        )
        map[BlockId.SETTINGS_MASK_PROTOCOL_ENC] = UnitResource(
                R.string.pers_block_settings_mask_protocol_enc,
                R.string.info_pers_block_settings_mask_protocol_enc
        )
        map[BlockId.SETTINGS_MASK_NDEF] = UnitResource(
                R.string.pers_block_settings_mask_ndef,
                R.string.info_pers_block_settings_mask_ndef
        )
        map[BlockId.PINS] = UnitResource(
                R.string.pers_block_pins,
                R.string.info_pers_block_pins
        )
    }

    private fun initCardNumber(map: MutableMap<Id, UnitResource>) {
        map[CardNumber.SERIES] = UnitResource(
                R.string.pers_item_series,
                R.string.info_pers_item_series
        )
        map[CardNumber.NUMBER] = UnitResource(
                R.string.pers_item_number,
                R.string.info_pers_item_number
        )
    }

    private fun initCommon(map: MutableMap<Id, UnitResource>) {
        map[Common.CURVE] = UnitResource(
                R.string.pers_item_curve,
                R.string.info_pers_item_curve
        )
        map[Common.BLOCKCHAIN] = UnitResource(
                R.string.pers_item_blockchain,
                R.string.info_pers_item_blockchain
        )
        map[Common.BLOCKCHAIN_CUSTOM] = UnitResource(
                R.string.pers_item_custom_blockchain,
                R.string.info_pers_item_custom_blockchain
        )
        map[Common.MAX_SIGNATURES] = UnitResource(
                R.string.pers_item_max_signatures,
                R.string.info_pers_item_max_signatures
        )
        map[Common.CREATE_WALLET] = UnitResource(
                R.string.pers_item_create_wallet,
                R.string.info_pers_item_create_wallet
        )
    }

    private fun initSigningMethod(map: MutableMap<Id, UnitResource>) {
        map[SigningMethod.SIGN_TX] = UnitResource(
                R.string.pers_item_sign_tx_hashes,
                R.string.info_pers_item_sign_tx_hashes
        )
        map[SigningMethod.SIGN_TX_RAW] = UnitResource(
                R.string.pers_item_sign_raw_tx,
                R.string.info_pers_item_sign_raw_tx
        )
        map[SigningMethod.SIGN_VALIDATED_TX] = UnitResource(
                R.string.pers_item_sign_validated_tx_hashes,
                R.string.info_pers_item_sign_validated_tx_hashes
        )
        map[SigningMethod.SIGN_VALIDATED_TX_RAW] = UnitResource(
                R.string.pers_item_sign_validated_raw_tx,
                R.string.info_pers_item_sign_validated_raw_tx
        )
        map[SigningMethod.SIGN_VALIDATED_TX_ISSUER] = UnitResource(
                R.string.pers_item_sign_validated_tx_hashes_with_iss_data,
                R.string.info_pers_item_sign_validated_tx_hashes_with_iss_data
        )
        map[SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER] = UnitResource(
                R.string.pers_item_sign_validated_raw_tx_with_iss_data,
                R.string.info_pers_item_sign_validated_raw_tx_with_iss_data
        )
        map[SigningMethod.SIGN_EXTERNAL] = UnitResource(
                R.string.pers_item_sign_hash_ex,
                R.string.info_pers_item_sign_hash_ex
        )
    }

    private fun initSignHashExProp(map: MutableMap<Id, UnitResource>) {
        map[SignHashExProp.PIN_LESS_FLOOR_LIMIT] = UnitResource(
                R.string.pers_item_pin_less_floor_limit,
                R.string.info_pers_item_pin_less_floor_limit
        )
        map[SignHashExProp.CRYPTO_EXTRACT_KEY] = UnitResource(
                R.string.pers_item_cr_ex_key,
                R.string.info_pers_item_cr_ex_key
        )
        map[SignHashExProp.REQUIRE_TERMINAL_CERT_SIG] = UnitResource(
                R.string.pers_item_require_terminal_cert_sig,
                R.string.info_pers_item_require_terminal_cert_sig
        )
        map[SignHashExProp.REQUIRE_TERMINAL_TX_SIG] = UnitResource(
                R.string.pers_item_require_terminal_tx_sig,
                R.string.info_pers_item_require_terminal_tx_sig
        )
        map[SignHashExProp.CHECK_PIN3] = UnitResource(
                R.string.pers_item_pin3,
                R.string.info_pers_item_pin3
        )
    }

    private fun initDenomination(map: MutableMap<Id, UnitResource>) {
        map[Denomination.WRITE_ON_PERSONALIZE] = UnitResource(
                R.string.pers_item_write_on_personalize,
                R.string.info_pers_item_write_on_personalize
        )
        map[Denomination.DENOMINATION] = UnitResource(
                R.string.pers_item_denomination,
                R.string.info_pers_item_denomination
        )
    }

    private fun initToken(map: MutableMap<Id, UnitResource>) {
        map[Token.ITS_TOKEN] = UnitResource(
                R.string.pers_item_its_token,
                R.string.info_pers_item_its_token
        )
        map[Token.SYMBOL] = UnitResource(
                R.string.pers_item_symbol,
                R.string.info_pers_item_symbol
        )
        map[Token.CONTRACT_ADDRESS] = UnitResource(
                R.string.pers_item_contract_address,
                R.string.info_pers_item_contract_address
        )
        map[Token.DECIMAL] = UnitResource(
                R.string.pers_item_decimal,
                R.string.info_pers_item_decimal
        )
    }

    private fun initProductMask(map: MutableMap<Id, UnitResource>) {
        map[ProductMask.NOTE] = UnitResource(
                R.string.pers_item_note,
                R.string.info_pers_item_note
        )
        map[ProductMask.TAG] = UnitResource(
                R.string.pers_item_tag,
                R.string.info_pers_item_tag
        )
        map[ProductMask.ID_CARD] = UnitResource(
                R.string.pers_item_id_card,
                R.string.info_pers_item_id_card
        )
    }

    private fun initSettingsMask(map: MutableMap<Id, UnitResource>) {
        map[SettingsMask.IS_REUSABLE] = UnitResource(
                R.string.pers_item_is_reusable,
                R.string.info_pers_item_is_reusable
        )
        map[SettingsMask.NEED_ACTIVATION] = UnitResource(
                R.string.pers_item_need_activation,
                R.string.info_pers_item_need_activation
        )
        map[SettingsMask.FORBID_PURGE] = UnitResource(
                R.string.pers_item_forbid_purge,
                R.string.info_pers_item_forbid_purge
        )
        map[SettingsMask.ALLOW_SELECT_BLOCKCHAIN] = UnitResource(
                R.string.pers_item_allow_select_blockchain,
                R.string.info_pers_item_allow_select_blockchain
        )
        map[SettingsMask.USE_BLOCK] = UnitResource(
                R.string.pers_item_use_block,
                R.string.info_pers_item_use_block
        )
        map[SettingsMask.ONE_APDU] = UnitResource(
                R.string.pers_item_one_apdu_at_once,
                R.string.info_pers_item_one_apdu_at_once
        )
        map[SettingsMask.USE_CVC] = UnitResource(
                R.string.pers_item_use_cvc,
                R.string.info_pers_item_use_cvc
        )
        map[SettingsMask.ALLOW_SWAP_PIN] = UnitResource(
                R.string.pers_item_allow_swap_pin,
                R.string.info_pers_item_allow_swap_pin
        )
        map[SettingsMask.ALLOW_SWAP_PIN2] = UnitResource(
                R.string.pers_item_allow_swap_pin2,
                R.string.info_pers_item_allow_swap_pin2
        )
        map[SettingsMask.FORBID_DEFAULT_PIN] = UnitResource(
                R.string.pers_item_forbid_default_pin,
                R.string.info_pers_item_forbid_default_pin
        )
        map[SettingsMask.SMART_SECURITY_DELAY] = UnitResource(
                R.string.pers_item_smart_security_delay,
                R.string.info_pers_item_smart_security_delay
        )
        map[SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY] = UnitResource(
                R.string.pers_item_protect_issuer_data_against_replay,
                R.string.info_pers_item_protect_issuer_data_against_replay
        )
        map[SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED] = UnitResource(
                R.string.pers_item_skip_security_delay_if_validated,
                R.string.info_pers_item_skip_security_delay_if_validated
        )
        map[SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED] = UnitResource(
                R.string.pers_item_skip_pin2_and_cvc_if_validated,
                R.string.info_pers_item_skip_pin2_and_cvc_if_validated
        )
        map[SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL] = UnitResource(
                R.string.pers_item_skip_security_delay_on_linked_terminal,
                R.string.info_pers_item_skip_security_delay_on_linked_terminal
        )
        map[SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA] = UnitResource(
                R.string.pers_item_restrict_overwrite_ex_issuer_data,
                R.string.info_pers_item_restrict_overwrite_ex_issuer_data
        )
    }

    private fun initSettingsMaskProtocolEnc(map: MutableMap<Id, UnitResource>) {
        map[SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED] = UnitResource(
                R.string.pers_item_allow_unencrypted,
                R.string.info_pers_item_allow_unencrypted
        )
        map[SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION] = UnitResource(
                R.string.pers_item_allow_fast_encryption,
                R.string.info_pers_item_allow_fast_encryption
        )
    }

    private fun initSettingsMaskNde(map: MutableMap<Id, UnitResource>) {
        map[SettingsMaskNdef.USE_NDEF] = UnitResource(
                R.string.pers_item_use_ndef,
                R.string.info_pers_item_use_ndef
        )
        map[SettingsMaskNdef.DYNAMIC_NDEF] = UnitResource(
                R.string.pers_item_dynamic_ndef,
                R.string.info_pers_item_dynamic_ndef
        )
        map[SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF] = UnitResource(
                R.string.pers_item_disable_precomputed_ndef,
                R.string.info_pers_item_disable_precomputed_ndef
        )
        map[SettingsMaskNdef.AAR] = UnitResource(
                R.string.pers_item_aar,
                R.string.info_pers_item_aar
        )
    }

    private fun initPins(map: MutableMap<Id, UnitResource>) {
        map[Pins.PIN] = UnitResource(
                R.string.pers_item_pin,
                R.string.info_pers_item_pin
        )
        map[Pins.PIN2] = UnitResource(
                R.string.pers_item_pin2,
                R.string.info_pers_item_pin2
        )
        map[Pins.PIN3] = UnitResource(
                R.string.pers_item_pin3,
                R.string.info_pers_item_pin3
        )
        map[Pins.CVC] = UnitResource(
                R.string.pers_item_cvc,
                R.string.info_pers_item_cvc
        )
        map[Pins.PAUSE_BEFORE_PIN2] = UnitResource(
                R.string.pers_item_pause_before_pin2,
                R.string.info_pers_item_pause_before_pin2
        )
    }
}