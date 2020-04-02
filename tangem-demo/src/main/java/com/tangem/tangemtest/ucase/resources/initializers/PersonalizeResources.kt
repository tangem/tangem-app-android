package com.tangem.tangemtest.ucase.resources.initializers

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest.ucase.resources.ResourceHolder
import com.tangem.tangemtest.ucase.resources.Resources
import com.tangem.tangemtest.ucase.variants.personalize.*
import ru.dev.gbixahue.eu4d.lib.kotlin.common.TypedHolder

/**
[REDACTED_AUTHOR]
 */
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
        holder.register(BlockId.CardNumber, Resources(R.string.pers_block_card_number, R.string.info_pers_block_card_number))
        holder.register(BlockId.Common, Resources(R.string.pers_block_common, R.string.info_pers_block_common))
        holder.register(BlockId.SigningMethod, Resources(R.string.pers_block_signing_method, R.string.info_pers_block_signing_method))
        holder.register(BlockId.SignHashExProp, Resources(R.string.pers_block_sign_hash_ex_prop, R.string.info_pers_block_sign_hash_ex_prop))
        holder.register(BlockId.Denomination, Resources(R.string.pers_block_denomination, R.string.info_pers_block_denomination))
        holder.register(BlockId.Token, Resources(R.string.pers_block_token, R.string.info_pers_block_token))
        holder.register(BlockId.ProdMask, Resources(R.string.pers_block_product_mask, R.string.info_pers_block_product_mask))
        holder.register(BlockId.SettingsMask, Resources(R.string.pers_block_settings_mask, R.string.info_pers_block_settings_mask))
        holder.register(BlockId.SettingsMaskProtocolEnc, Resources(R.string.pers_block_settings_mask_protocol_enc, R.string.info_pers_block_settings_mask_protocol_enc))
        holder.register(BlockId.SettingsMaskNdef, Resources(R.string.pers_block_settings_mask_ndef, R.string.info_pers_block_settings_mask_ndef))
        holder.register(BlockId.Pins, Resources(R.string.pers_block_pins, R.string.info_pers_block_pins))
    }

    private fun initCardNumber(holder: TypedHolder<Id, Resources>) {
        holder.register(CardNumber.Series, Resources(R.string.pers_item_series, R.string.info_pers_item_series))
        holder.register(CardNumber.Number, Resources(R.string.pers_item_number, R.string.info_pers_item_number))
        holder.register(CardNumber.BatchId, Resources(R.string.pers_item_batch_id, R.string.info_pers_item_batch_id))
    }

    private fun initCommon(holder: TypedHolder<Id, Resources>) {
        holder.register(Common.Curve, Resources(R.string.pers_item_curve, R.string.info_pers_item_curve))
        holder.register(Common.Blockchain, Resources(R.string.pers_item_blockchain, R.string.info_pers_item_blockchain))
        holder.register(Common.BlockchainCustom, Resources(R.string.pers_item_custom_blockchain, R.string.info_pers_item_custom_blockchain))
        holder.register(Common.MaxSignatures, Resources(R.string.pers_item_max_signatures, R.string.info_pers_item_max_signatures))
        holder.register(Common.CreateWallet, Resources(R.string.pers_item_create_wallet, R.string.info_pers_item_create_wallet))
    }

    private fun initSigningMethod(holder: TypedHolder<Id, Resources>) {
        holder.register(SigningMethod.SignTx, Resources(R.string.pers_item_sign_tx_hashes, R.string.info_pers_item_sign_tx_hashes))
        holder.register(SigningMethod.SignTxRaw, Resources(R.string.pers_item_sign_raw_tx, R.string.info_pers_item_sign_raw_tx))
        holder.register(SigningMethod.SignValidatedTx, Resources(R.string.pers_item_sign_validated_tx_hashes, R.string.info_pers_item_sign_validated_tx_hashes))
        holder.register(SigningMethod.SignValidatedTxRaw, Resources(R.string.pers_item_sign_validated_raw_tx, R.string.info_pers_item_sign_validated_raw_tx))
        holder.register(SigningMethod.SignValidatedTxIssuer, Resources(R.string.pers_item_sign_validated_tx_hashes_with_iss_data, R.string.info_pers_item_sign_validated_tx_hashes_with_iss_data))
        holder.register(SigningMethod.SignValidatedTxRawIssuer, Resources(R.string.pers_item_sign_validated_raw_tx_with_iss_data, R.string.info_pers_item_sign_validated_raw_tx_with_iss_data))
        holder.register(SigningMethod.SignExternal, Resources(R.string.pers_item_sign_hash_ex, R.string.info_pers_item_sign_hash_ex))
    }

    private fun initSignHashExProp(holder: TypedHolder<Id, Resources>) {
        holder.register(SignHashExProp.PinLessFloorLimit, Resources(R.string.pers_item_pin_less_floor_limit, R.string.info_pers_item_pin_less_floor_limit))
        holder.register(SignHashExProp.CryptoExKey, Resources(R.string.pers_item_cr_ex_key, R.string.info_pers_item_cr_ex_key))
        holder.register(SignHashExProp.RequireTerminalCertSig, Resources(R.string.pers_item_require_terminal_cert_sig, R.string.info_pers_item_require_terminal_cert_sig))
        holder.register(SignHashExProp.RequireTerminalTxSig, Resources(R.string.pers_item_require_terminal_tx_sig, R.string.info_pers_item_require_terminal_tx_sig))
        holder.register(SignHashExProp.CheckPin3, Resources(R.string.pers_item_check_pin3_on_card, R.string.info_pers_item_check_pin3_on_card))
    }

    private fun initDenomination(holder: TypedHolder<Id, Resources>) {
        holder.register(Denomination.WriteOnPersonalize, Resources(R.string.pers_item_write_on_personalize, R.string.info_pers_item_write_on_personalize))
        holder.register(Denomination.Denomination, Resources(R.string.pers_item_denomination, R.string.info_pers_item_denomination))
    }

    private fun initToken(holder: TypedHolder<Id, Resources>) {
        holder.register(Token.ItsToken, Resources(R.string.pers_item_its_token, R.string.info_pers_item_its_token))
        holder.register(Token.Symbol, Resources(R.string.pers_item_symbol, R.string.info_pers_item_symbol))
        holder.register(Token.ContractAddress, Resources(R.string.pers_item_contract_address, R.string.info_pers_item_contract_address))
        holder.register(Token.Decimal, Resources(R.string.pers_item_decimal, R.string.info_pers_item_decimal))
    }

    private fun initProductMask(holder: TypedHolder<Id, Resources>) {
        holder.register(ProductMask.Note, Resources(R.string.pers_item_note, R.string.info_pers_item_note))
        holder.register(ProductMask.Tag, Resources(R.string.pers_item_tag, R.string.info_pers_item_tag))
        holder.register(ProductMask.CardId, Resources(R.string.pers_item_id_card, R.string.info_pers_item_id_card))
    }

    private fun initSettingsMask(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMask.IsReusable, Resources(R.string.pers_item_is_reusable, R.string.info_pers_item_is_reusable))
        holder.register(SettingsMask.NeedActivation, Resources(R.string.pers_item_need_activation, R.string.info_pers_item_need_activation))
        holder.register(SettingsMask.ForbidPurge, Resources(R.string.pers_item_forbid_purge, R.string.info_pers_item_forbid_purge))
        holder.register(SettingsMask.AllowSelectBlockchain, Resources(R.string.pers_item_allow_select_blockchain, R.string.info_pers_item_allow_select_blockchain))
        holder.register(SettingsMask.UseBlock, Resources(R.string.pers_item_use_block, R.string.info_pers_item_use_block))
        holder.register(SettingsMask.OneApdu, Resources(R.string.pers_item_one_apdu_at_once, R.string.info_pers_item_one_apdu_at_once))
        holder.register(SettingsMask.UseCvc, Resources(R.string.pers_item_use_cvc, R.string.info_pers_item_use_cvc))
        holder.register(SettingsMask.AllowSwapPin, Resources(R.string.pers_item_allow_swap_pin, R.string.info_pers_item_allow_swap_pin))
        holder.register(SettingsMask.AllowSwapPin2, Resources(R.string.pers_item_allow_swap_pin2, R.string.info_pers_item_allow_swap_pin2))
        holder.register(SettingsMask.ForbidDefaultPin, Resources(R.string.pers_item_forbid_default_pin, R.string.info_pers_item_forbid_default_pin))
        holder.register(SettingsMask.SmartSecurityDelay, Resources(R.string.pers_item_smart_security_delay, R.string.info_pers_item_smart_security_delay))
        holder.register(SettingsMask.ProtectIssuerDataAgainstReplay, Resources(R.string.pers_item_protect_issuer_data_against_replay, R.string.info_pers_item_protect_issuer_data_against_replay))
        holder.register(SettingsMask.SkipSecurityDelayIfValidated, Resources(R.string.pers_item_skip_security_delay_if_validated, R.string.info_pers_item_skip_security_delay_if_validated))
        holder.register(SettingsMask.SkipPin2CvcIfValidated, Resources(R.string.pers_item_skip_pin2_and_cvc_if_validated, R.string.info_pers_item_skip_pin2_and_cvc_if_validated))
        holder.register(SettingsMask.SkipSecurityDelayOnLinkedTerminal, Resources(R.string.pers_item_skip_security_delay_on_linked_terminal, R.string.info_pers_item_skip_security_delay_on_linked_terminal))
        holder.register(SettingsMask.RestrictOverwriteExtraIssuerData, Resources(R.string.pers_item_restrict_overwrite_ex_issuer_data, R.string.info_pers_item_restrict_overwrite_ex_issuer_data))
    }

    private fun initSettingsMaskProtocolEnc(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskProtocolEnc.AllowUnencrypted, Resources(R.string.pers_item_allow_unencrypted, R.string.info_pers_item_allow_unencrypted))
        holder.register(SettingsMaskProtocolEnc.AlloFastEncryption, Resources(R.string.pers_item_allow_fast_encryption, R.string.info_pers_item_allow_fast_encryption))
    }

    private fun initSettingsMaskNde(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskNdef.UseNdef, Resources(R.string.pers_item_use_ndef, R.string.info_pers_item_use_ndef))
        holder.register(SettingsMaskNdef.DynamicNdef, Resources(R.string.pers_item_dynamic_ndef, R.string.info_pers_item_dynamic_ndef))
        holder.register(SettingsMaskNdef.DisablePrecomputedNdef, Resources(R.string.pers_item_disable_precomputed_ndef, R.string.info_pers_item_disable_precomputed_ndef))
        holder.register(SettingsMaskNdef.Aar, Resources(R.string.pers_item_aar, R.string.info_pers_item_aar))
    }

    private fun initPins(holder: TypedHolder<Id, Resources>) {
        holder.register(Pins.Pin, Resources(R.string.pers_item_pin, R.string.info_pers_item_pin))
        holder.register(Pins.Pin2, Resources(R.string.pers_item_pin2, R.string.info_pers_item_pin2))
        holder.register(Pins.Pin3, Resources(R.string.pers_item_pin3, R.string.info_pers_item_pin3))
        holder.register(Pins.Cvc, Resources(R.string.pers_item_cvc, R.string.info_pers_item_cvc))
        holder.register(Pins.PauseBeforePin2, Resources(R.string.pers_item_pause_before_pin2, R.string.info_pers_item_pause_before_pin2))
    }
}