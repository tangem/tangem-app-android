package com.tangem.tangemtest.cardUseCase.resources.initializers

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.*
import com.tangem.tangemtest.cardUseCase.resources.ResourceHolder
import com.tangem.tangemtest.cardUseCase.resources.Resources
import ru.dev.gbixahue.eu4d.lib.kotlin.common.TypedHolder

class PersonalizeResources() {
    fun init(holder: ResourceHolder<Id>) {
        initBlock(holder)
        initCardNumber(holder)
        initCommon(holder)
        initSigningMethod(holder)
        initSignHashExProp(holder)
        initDenomination(holder)
        initToken(holder)
        initProductMask(holder)
        initSettingsMask(holder)
        initSettingsMaskProtocolEnc(holder)
        initSettingsMaskNde(holder)
        initPins(holder)
    }

    private fun initBlock(holder: TypedHolder<Id, Resources>) {
        holder.register(BlockId.CARD_NUMBER, Resources(R.string.pers_block_card_number, R.string.info_pers_block_card_number))
        holder.register(BlockId.COMMON, Resources(R.string.pers_block_common, R.string.info_pers_block_common))
        holder.register(BlockId.SIGNING_METHOD, Resources(R.string.pers_block_signing_method, R.string.info_pers_block_signing_method))
        holder.register(BlockId.SIGN_HASH_EX_PROP, Resources(R.string.pers_block_sign_hash_ex_prop, R.string.info_pers_block_sign_hash_ex_prop))
        holder.register(BlockId.DENOMINATION, Resources(R.string.pers_block_denomination, R.string.info_pers_block_denomination))
        holder.register(BlockId.TOKEN, Resources(R.string.pers_block_token, R.string.info_pers_block_token))
        holder.register(BlockId.PROD_MASK, Resources(R.string.pers_block_product_mask, R.string.info_pers_block_product_mask))
        holder.register(BlockId.SETTINGS_MASK, Resources(R.string.pers_block_settings_mask, R.string.info_pers_block_settings_mask))
        holder.register(BlockId.SETTINGS_MASK_PROTOCOL_ENC, Resources(R.string.pers_block_settings_mask_protocol_enc, R.string.info_pers_block_settings_mask_protocol_enc))
        holder.register(BlockId.SETTINGS_MASK_NDEF, Resources(R.string.pers_block_settings_mask_ndef, R.string.info_pers_block_settings_mask_ndef))
        holder.register(BlockId.PINS, Resources(R.string.pers_block_pins, R.string.info_pers_block_pins))
    }

    private fun initCardNumber(holder: TypedHolder<Id, Resources>) {
        holder.register(CardNumber.SERIES, Resources(R.string.pers_item_series, R.string.info_pers_item_series))
        holder.register(CardNumber.NUMBER, Resources(R.string.pers_item_number, R.string.info_pers_item_number))
    }

    private fun initCommon(holder: TypedHolder<Id, Resources>) {
        holder.register(Common.CURVE, Resources(R.string.pers_item_curve, R.string.info_pers_item_curve))
        holder.register(Common.BLOCKCHAIN, Resources(R.string.pers_item_blockchain, R.string.info_pers_item_blockchain))
        holder.register(Common.BLOCKCHAIN_CUSTOM, Resources(R.string.pers_item_custom_blockchain, R.string.info_pers_item_custom_blockchain))
        holder.register(Common.MAX_SIGNATURES, Resources(R.string.pers_item_max_signatures, R.string.info_pers_item_max_signatures))
        holder.register(Common.CREATE_WALLET, Resources(R.string.pers_item_create_wallet, R.string.info_pers_item_create_wallet))
    }

    private fun initSigningMethod(holder: TypedHolder<Id, Resources>) {
        holder.register(SigningMethod.SIGN_TX, Resources(R.string.pers_item_sign_tx_hashes, R.string.info_pers_item_sign_tx_hashes))
        holder.register(SigningMethod.SIGN_TX_RAW, Resources(R.string.pers_item_sign_raw_tx, R.string.info_pers_item_sign_raw_tx))
        holder.register(SigningMethod.SIGN_VALIDATED_TX, Resources(R.string.pers_item_sign_validated_tx_hashes, R.string.info_pers_item_sign_validated_tx_hashes))
        holder.register(SigningMethod.SIGN_VALIDATED_TX_RAW, Resources(R.string.pers_item_sign_validated_raw_tx, R.string.info_pers_item_sign_validated_raw_tx))
        holder.register(SigningMethod.SIGN_VALIDATED_TX_ISSUER, Resources(R.string.pers_item_sign_validated_tx_hashes_with_iss_data, R.string.info_pers_item_sign_validated_tx_hashes_with_iss_data))
        holder.register(SigningMethod.SIGN_VALIDATED_TX_RAW_ISSUER, Resources(R.string.pers_item_sign_validated_raw_tx_with_iss_data, R.string.info_pers_item_sign_validated_raw_tx_with_iss_data))
        holder.register(SigningMethod.SIGN_EXTERNAL, Resources(R.string.pers_item_sign_hash_ex, R.string.info_pers_item_sign_hash_ex))
    }

    private fun initSignHashExProp(holder: TypedHolder<Id, Resources>) {
        holder.register(SignHashExProp.PIN_LESS_FLOOR_LIMIT, Resources(R.string.pers_item_pin_less_floor_limit, R.string.info_pers_item_pin_less_floor_limit))
        holder.register(SignHashExProp.CRYPTO_EXTRACT_KEY, Resources(R.string.pers_item_cr_ex_key, R.string.info_pers_item_cr_ex_key))
        holder.register(SignHashExProp.REQUIRE_TERMINAL_CERT_SIG, Resources(R.string.pers_item_require_terminal_cert_sig, R.string.info_pers_item_require_terminal_cert_sig))
        holder.register(SignHashExProp.REQUIRE_TERMINAL_TX_SIG, Resources(R.string.pers_item_require_terminal_tx_sig, R.string.info_pers_item_require_terminal_tx_sig))
        holder.register(SignHashExProp.CHECK_PIN3, Resources(R.string.pers_item_pin3, R.string.info_pers_item_pin3))
    }

    private fun initDenomination(holder: TypedHolder<Id, Resources>) {
        holder.register(Denomination.WRITE_ON_PERSONALIZE, Resources(R.string.pers_item_write_on_personalize, R.string.info_pers_item_write_on_personalize))
        holder.register(Denomination.DENOMINATION, Resources(R.string.pers_item_denomination, R.string.info_pers_item_denomination))
    }

    private fun initToken(holder: TypedHolder<Id, Resources>) {
        holder.register(Token.ITS_TOKEN, Resources(R.string.pers_item_its_token, R.string.info_pers_item_its_token))
        holder.register(Token.SYMBOL, Resources(R.string.pers_item_symbol, R.string.info_pers_item_symbol))
        holder.register(Token.CONTRACT_ADDRESS, Resources(R.string.pers_item_contract_address, R.string.info_pers_item_contract_address))
        holder.register(Token.DECIMAL, Resources(R.string.pers_item_decimal, R.string.info_pers_item_decimal))
    }

    private fun initProductMask(holder: TypedHolder<Id, Resources>) {
        holder.register(ProductMask.NOTE, Resources(R.string.pers_item_note, R.string.info_pers_item_note))
        holder.register(ProductMask.TAG, Resources(R.string.pers_item_tag, R.string.info_pers_item_tag))
        holder.register(ProductMask.ID_CARD, Resources(R.string.pers_item_id_card, R.string.info_pers_item_id_card))
    }

    private fun initSettingsMask(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMask.IS_REUSABLE, Resources(R.string.pers_item_is_reusable, R.string.info_pers_item_is_reusable))
        holder.register(SettingsMask.NEED_ACTIVATION, Resources(R.string.pers_item_need_activation, R.string.info_pers_item_need_activation))
        holder.register(SettingsMask.FORBID_PURGE, Resources(R.string.pers_item_forbid_purge, R.string.info_pers_item_forbid_purge))
        holder.register(SettingsMask.ALLOW_SELECT_BLOCKCHAIN, Resources(R.string.pers_item_allow_select_blockchain, R.string.info_pers_item_allow_select_blockchain))
        holder.register(SettingsMask.USE_BLOCK, Resources(R.string.pers_item_use_block, R.string.info_pers_item_use_block))
        holder.register(SettingsMask.ONE_APDU, Resources(R.string.pers_item_one_apdu_at_once, R.string.info_pers_item_one_apdu_at_once))
        holder.register(SettingsMask.USE_CVC, Resources(R.string.pers_item_use_cvc, R.string.info_pers_item_use_cvc))
        holder.register(SettingsMask.ALLOW_SWAP_PIN, Resources(R.string.pers_item_allow_swap_pin, R.string.info_pers_item_allow_swap_pin))
        holder.register(SettingsMask.ALLOW_SWAP_PIN2, Resources(R.string.pers_item_allow_swap_pin2, R.string.info_pers_item_allow_swap_pin2))
        holder.register(SettingsMask.FORBID_DEFAULT_PIN, Resources(R.string.pers_item_forbid_default_pin, R.string.info_pers_item_forbid_default_pin))
        holder.register(SettingsMask.SMART_SECURITY_DELAY, Resources(R.string.pers_item_smart_security_delay, R.string.info_pers_item_smart_security_delay))
        holder.register(SettingsMask.PROTECT_ISSUER_DATA_AGAINST_REPLAY, Resources(R.string.pers_item_protect_issuer_data_against_replay, R.string.info_pers_item_protect_issuer_data_against_replay))
        holder.register(SettingsMask.SKIP_SECURITY_DELAY_IF_VALIDATED, Resources(R.string.pers_item_skip_security_delay_if_validated, R.string.info_pers_item_skip_security_delay_if_validated))
        holder.register(SettingsMask.SKIP_PIN2_CVC_IF_VALIDATED, Resources(R.string.pers_item_skip_pin2_and_cvc_if_validated, R.string.info_pers_item_skip_pin2_and_cvc_if_validated))
        holder.register(SettingsMask.SKIP_SECURITY_DELAY_ON_LINKED_TERMINAL, Resources(R.string.pers_item_skip_security_delay_on_linked_terminal, R.string.info_pers_item_skip_security_delay_on_linked_terminal))
        holder.register(SettingsMask.RESTRICT_OVERWRITE_EXTRA_ISSUER_DATA, Resources(R.string.pers_item_restrict_overwrite_ex_issuer_data, R.string.info_pers_item_restrict_overwrite_ex_issuer_data))
    }

    private fun initSettingsMaskProtocolEnc(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskProtocolEnc.ALLOW_UNENCRYPTED, Resources(R.string.pers_item_allow_unencrypted, R.string.info_pers_item_allow_unencrypted))
        holder.register(SettingsMaskProtocolEnc.ALLOW_FAST_ENCRYPTION, Resources(R.string.pers_item_allow_fast_encryption, R.string.info_pers_item_allow_fast_encryption))
    }

    private fun initSettingsMaskNde(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskNdef.USE_NDEF, Resources(R.string.pers_item_use_ndef, R.string.info_pers_item_use_ndef))
        holder.register(SettingsMaskNdef.DYNAMIC_NDEF, Resources(R.string.pers_item_dynamic_ndef, R.string.info_pers_item_dynamic_ndef))
        holder.register(SettingsMaskNdef.DISABLE_PRECOMPUTED_NDEF, Resources(R.string.pers_item_disable_precomputed_ndef, R.string.info_pers_item_disable_precomputed_ndef))
        holder.register(SettingsMaskNdef.AAR, Resources(R.string.pers_item_aar, R.string.info_pers_item_aar))
    }

    private fun initPins(holder: TypedHolder<Id, Resources>) {
        holder.register(Pins.PIN, Resources(R.string.pers_item_pin, R.string.info_pers_item_pin))
        holder.register(Pins.PIN2, Resources(R.string.pers_item_pin2, R.string.info_pers_item_pin2))
        holder.register(Pins.PIN3, Resources(R.string.pers_item_pin3, R.string.info_pers_item_pin3))
        holder.register(Pins.CVC, Resources(R.string.pers_item_cvc, R.string.info_pers_item_cvc))
        holder.register(Pins.PAUSE_BEFORE_PIN2, Resources(R.string.pers_item_pause_before_pin2, R.string.info_pers_item_pause_before_pin2))
    }
}