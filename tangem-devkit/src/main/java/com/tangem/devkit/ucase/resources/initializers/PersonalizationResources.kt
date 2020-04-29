package com.tangem.devkit.ucase.resources.initializers

import com.tangem.devkit.R
import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit.ucase.resources.ResourceHolder
import com.tangem.devkit.ucase.resources.Resources
import com.tangem.devkit.ucase.variants.personalize.*
import ru.dev.gbixahue.eu4d.lib.kotlin.common.TypedHolder

/**
[REDACTED_AUTHOR]
 */
class PersonalizationResources() {
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
        initSettingsMaskNdef(holder)
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
        holder.register(CardNumberId.Series, Resources(R.string.pers_item_series, R.string.info_pers_item_series))
        holder.register(CardNumberId.Number, Resources(R.string.pers_item_number, R.string.info_pers_item_number))
        holder.register(CardNumberId.BatchId, Resources(R.string.pers_item_batch_id, R.string.info_pers_item_batch_id))
    }

    private fun initCommon(holder: TypedHolder<Id, Resources>) {
        holder.register(CommonId.Curve, Resources(R.string.pers_item_curve, R.string.info_pers_item_curve))
        holder.register(CommonId.Blockchain, Resources(R.string.pers_item_blockchain, R.string.info_pers_item_blockchain))
        holder.register(CommonId.BlockchainCustom, Resources(R.string.pers_item_custom_blockchain, R.string.info_pers_item_custom_blockchain))
        holder.register(CommonId.MaxSignatures, Resources(R.string.pers_item_max_signatures, R.string.info_pers_item_max_signatures))
        holder.register(CommonId.CreateWallet, Resources(R.string.pers_item_create_wallet, R.string.info_pers_item_create_wallet))
    }

    private fun initSigningMethod(holder: TypedHolder<Id, Resources>) {
        holder.register(SigningMethodId.SignTx, Resources(R.string.pers_item_sign_tx_hashes, R.string.info_pers_item_sign_tx_hashes))
        holder.register(SigningMethodId.SignTxRaw, Resources(R.string.pers_item_sign_raw_tx, R.string.info_pers_item_sign_raw_tx))
        holder.register(SigningMethodId.SignValidatedTx, Resources(R.string.pers_item_sign_validated_tx_hashes, R.string.info_pers_item_sign_validated_tx_hashes))
        holder.register(SigningMethodId.SignValidatedTxRaw, Resources(R.string.pers_item_sign_validated_raw_tx, R.string.info_pers_item_sign_validated_raw_tx))
        holder.register(SigningMethodId.SignValidatedTxIssuer, Resources(R.string.pers_item_sign_validated_tx_hashes_with_iss_data, R.string.info_pers_item_sign_validated_tx_hashes_with_iss_data))
        holder.register(SigningMethodId.SignValidatedTxRawIssuer, Resources(R.string.pers_item_sign_validated_raw_tx_with_iss_data, R.string.info_pers_item_sign_validated_raw_tx_with_iss_data))
        holder.register(SigningMethodId.SignExternal, Resources(R.string.pers_item_sign_hash_ex, R.string.info_pers_item_sign_hash_ex))
    }

    private fun initSignHashExProp(holder: TypedHolder<Id, Resources>) {
        holder.register(SignHashExPropId.PinLessFloorLimit, Resources(R.string.pers_item_pin_less_floor_limit, R.string.info_pers_item_pin_less_floor_limit))
        holder.register(SignHashExPropId.CryptoExKey, Resources(R.string.pers_item_cr_ex_key, R.string.info_pers_item_cr_ex_key))
        holder.register(SignHashExPropId.RequireTerminalCertSig, Resources(R.string.pers_item_require_terminal_cert_sig, R.string.info_pers_item_require_terminal_cert_sig))
        holder.register(SignHashExPropId.RequireTerminalTxSig, Resources(R.string.pers_item_require_terminal_tx_sig, R.string.info_pers_item_require_terminal_tx_sig))
        holder.register(SignHashExPropId.CheckPin3, Resources(R.string.pers_item_check_pin3_on_card, R.string.info_pers_item_check_pin3_on_card))
    }

    private fun initDenomination(holder: TypedHolder<Id, Resources>) {
        holder.register(DenominationId.WriteOnPersonalize, Resources(R.string.pers_item_write_on_personalize, R.string.info_pers_item_write_on_personalize))
        holder.register(DenominationId.Denomination, Resources(R.string.pers_item_denomination, R.string.info_pers_item_denomination))
    }

    private fun initToken(holder: TypedHolder<Id, Resources>) {
        holder.register(TokenId.ItsToken, Resources(R.string.pers_item_its_token, R.string.info_pers_item_its_token))
        holder.register(TokenId.Symbol, Resources(R.string.pers_item_symbol, R.string.info_pers_item_symbol))
        holder.register(TokenId.ContractAddress, Resources(R.string.pers_item_contract_address, R.string.info_pers_item_contract_address))
        holder.register(TokenId.Decimal, Resources(R.string.pers_item_decimal, R.string.info_pers_item_decimal))
    }

    private fun initProductMask(holder: TypedHolder<Id, Resources>) {
        holder.register(ProductMaskId.Note, Resources(R.string.pers_item_note, R.string.info_pers_item_note))
        holder.register(ProductMaskId.Tag, Resources(R.string.pers_item_tag, R.string.info_pers_item_tag))
        holder.register(ProductMaskId.IdCard, Resources(R.string.pers_item_id_card, R.string.info_pers_item_id_card))
        holder.register(ProductMaskId.IdIssuerCard, Resources(R.string.pers_item_id_issuer_card, R.string.info_pers_item_id_issuer_card))
    }

    private fun initSettingsMask(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskId.IsReusable, Resources(R.string.pers_item_is_reusable, R.string.info_pers_item_is_reusable))
        holder.register(SettingsMaskId.NeedActivation, Resources(R.string.pers_item_need_activation, R.string.info_pers_item_need_activation))
        holder.register(SettingsMaskId.ForbidPurge, Resources(R.string.pers_item_forbid_purge, R.string.info_pers_item_forbid_purge))
        holder.register(SettingsMaskId.AllowSelectBlockchain, Resources(R.string.pers_item_allow_select_blockchain, R.string.info_pers_item_allow_select_blockchain))
        holder.register(SettingsMaskId.UseBlock, Resources(R.string.pers_item_use_block, R.string.info_pers_item_use_block))
        holder.register(SettingsMaskId.OneApdu, Resources(R.string.pers_item_one_apdu_at_once, R.string.info_pers_item_one_apdu_at_once))
        holder.register(SettingsMaskId.UseCvc, Resources(R.string.pers_item_use_cvc, R.string.info_pers_item_use_cvc))
        holder.register(SettingsMaskId.AllowSwapPin, Resources(R.string.pers_item_allow_swap_pin, R.string.info_pers_item_allow_swap_pin))
        holder.register(SettingsMaskId.AllowSwapPin2, Resources(R.string.pers_item_allow_swap_pin2, R.string.info_pers_item_allow_swap_pin2))
        holder.register(SettingsMaskId.ForbidDefaultPin, Resources(R.string.pers_item_forbid_default_pin, R.string.info_pers_item_forbid_default_pin))
        holder.register(SettingsMaskId.SmartSecurityDelay, Resources(R.string.pers_item_smart_security_delay, R.string.info_pers_item_smart_security_delay))
        holder.register(SettingsMaskId.ProtectIssuerDataAgainstReplay, Resources(R.string.pers_item_protect_issuer_data_against_replay, R.string.info_pers_item_protect_issuer_data_against_replay))
        holder.register(SettingsMaskId.SkipSecurityDelayIfValidated, Resources(R.string.pers_item_skip_security_delay_if_validated, R.string.info_pers_item_skip_security_delay_if_validated))
        holder.register(SettingsMaskId.SkipPin2CvcIfValidated, Resources(R.string.pers_item_skip_pin2_and_cvc_if_validated, R.string.info_pers_item_skip_pin2_and_cvc_if_validated))
        holder.register(SettingsMaskId.SkipSecurityDelayOnLinkedTerminal, Resources(R.string.pers_item_skip_security_delay_on_linked_terminal, R.string.info_pers_item_skip_security_delay_on_linked_terminal))
        holder.register(SettingsMaskId.RestrictOverwriteExtraIssuerData, Resources(R.string.pers_item_restrict_overwrite_ex_issuer_data, R.string.info_pers_item_restrict_overwrite_ex_issuer_data))
    }

    private fun initSettingsMaskProtocolEnc(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskProtocolEncId.AllowUnencrypted, Resources(R.string.pers_item_allow_unencrypted, R.string.info_pers_item_allow_unencrypted))
        holder.register(SettingsMaskProtocolEncId.AllowStaticEncryption, Resources(R.string.pers_item_allow_fast_encryption, R.string.info_pers_item_allow_fast_encryption))
    }

    private fun initSettingsMaskNdef(holder: TypedHolder<Id, Resources>) {
        holder.register(SettingsMaskNdefId.UseNdef, Resources(R.string.pers_item_use_ndef, R.string.info_pers_item_use_ndef))
        holder.register(SettingsMaskNdefId.DynamicNdef, Resources(R.string.pers_item_dynamic_ndef, R.string.info_pers_item_dynamic_ndef))
        holder.register(SettingsMaskNdefId.DisablePrecomputedNdef, Resources(R.string.pers_item_disable_precomputed_ndef, R.string.info_pers_item_disable_precomputed_ndef))
        holder.register(SettingsMaskNdefId.Aar, Resources(R.string.pers_item_aar, R.string.info_pers_item_aar))
        holder.register(SettingsMaskNdefId.AarCustom, Resources(R.string.pers_item_custom_aar_package_name, R.string.info_pers_item_custom_aar_package_name))
        holder.register(SettingsMaskNdefId.Uri, Resources(R.string.pers_item_uri, R.string.info_pers_item_uri))
    }

    private fun initPins(holder: TypedHolder<Id, Resources>) {
        holder.register(PinsId.Pin, Resources(R.string.pers_item_pin, R.string.info_pers_item_pin))
        holder.register(PinsId.Pin2, Resources(R.string.pers_item_pin2, R.string.info_pers_item_pin2))
        holder.register(PinsId.Pin3, Resources(R.string.pers_item_pin3, R.string.info_pers_item_pin3))
        holder.register(PinsId.Cvc, Resources(R.string.pers_item_cvc, R.string.info_pers_item_cvc))
        holder.register(PinsId.PauseBeforePin2, Resources(R.string.pers_item_pause_before_pin2, R.string.info_pers_item_pause_before_pin2))
    }
}