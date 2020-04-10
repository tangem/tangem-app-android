package com.tangem.tangemtest.ucase.resources.initializers

import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.resources.MainResourceHolder
import com.tangem.tangemtest.ucase.resources.Resources
import com.tangem.tangemtest.ucase.variants.responses.CardDataId
import com.tangem.tangemtest.ucase.variants.responses.CardId

/**
[REDACTED_AUTHOR]
 */
class ResponseResources {
    fun init(holder: MainResourceHolder) {
        initCard(holder)
        initCardData(holder)
    }

    private fun initCard(holder: MainResourceHolder) {
        holder.register(CardId.cardId, Resources(R.string.response_card_cid, R.string.info_response_card_cid))
        holder.register(CardId.manufacturerName, Resources(R.string.response_card_manufacturer_name, R.string.info_response_card_manufacturer_name))
        holder.register(CardId.status, Resources(R.string.response_card_status, R.string.info_response_card_status))
        holder.register(CardId.firmwareVersion, Resources(R.string.response_card_firmware_version, R.string.info_response_card_firmware_version))
        holder.register(CardId.cardPublicKey, Resources(R.string.response_card_public_key, R.string.info_response_card_public_key))
        holder.register(CardId.settingsMask, Resources(R.string.response_card_settings_mask, R.string.info_response_card_settings_mask))
        holder.register(CardId.cardData, Resources(R.string.response_card_card_data, R.string.info_response_card_card_data))
        holder.register(CardId.issuerPublicKey, Resources(R.string.response_card_issuer_data_public_key, R.string.info_response_card_issuer_data_public_key))
        holder.register(CardId.curve, Resources(R.string.response_card_curve, R.string.info_response_card_curve))
        holder.register(CardId.maxSignatures, Resources(R.string.response_card_max_signatures, R.string.info_response_card_max_signatures))
        holder.register(CardId.signingMethod, Resources(R.string.response_card_signing_method, R.string.response_card_signing_method))
        holder.register(CardId.pauseBeforePin2, Resources(R.string.response_card_pause_before_pin2, R.string.info_response_card_allow_pin2))
        holder.register(CardId.walletPublicKey, Resources(R.string.response_card_wallet_public_key, R.string.info_response_card_wallet_public_key))
        holder.register(CardId.walletRemainingSignatures, Resources(R.string.response_card_wallet_remaining_signatures, R.string.info_response_card_wallet_remaining_signatures))
        holder.register(CardId.walletSignedHashes, Resources(R.string.response_card_wallet_signed_hashes, R.string.info_response_card_wallet_signed_hashes))
        holder.register(CardId.health, Resources(R.string.response_card_health, R.string.info_response_card_health))
        holder.register(CardId.isActivated, Resources(R.string.response_card_is_activated, R.string.info_response_card_is_activated))
        holder.register(CardId.activationSeed, Resources(R.string.response_card_activation_seed, R.string.info_response_card_activation_seed))
        holder.register(CardId.paymentFlowVersion, Resources(R.string.response_card_payment_flow_version, R.string.info_response_card_payment_flow_version))
        holder.register(CardId.userCounter, Resources(R.string.response_card_user_counter, R.string.info_response_card_user_counter))
        holder.register(CardId.userProtectedCounter, Resources(R.string.response_card_user_protected_counter, R.string.info_response_card_user_protected_counter))
    }

    private fun initCardData(holder: MainResourceHolder) {
        holder.register(CardDataId.batchId, Resources(R.string.response_card_card_data_batch_id, R.string.info_response_card_card_data_batch_id))
        holder.register(CardDataId.manufactureDateTime, Resources(R.string.response_card_card_data_manufacture_date_time, R.string.info_response_card_card_data_manufacture_date_time))
        holder.register(CardDataId.issuerName, Resources(R.string.response_card_card_data_issuer_name, R.string.info_response_card_card_data_issuer_name))
        holder.register(CardDataId.blockchainName, Resources(R.string.response_card_card_data_blockchain_name, R.string.info_response_card_card_data_blockchain_name))
        holder.register(CardDataId.manufacturerSignature, Resources(R.string.response_card_card_data_manufacturer_signature, R.string.info_response_card_card_data_manufacturer_signature))
        holder.register(CardDataId.productMask, Resources(R.string.response_card_card_data_product_mask, R.string.info_response_card_card_data_product_mask))
        holder.register(CardDataId.tokenSymbol, Resources(R.string.response_card_card_data_token_symbol, R.string.info_response_card_card_data_token_symbol))
        holder.register(CardDataId.tokenContractAddress, Resources(R.string.response_card_card_data_token_contract_address, R.string.info_response_card_card_data_token_contract_address))
        holder.register(CardDataId.tokenDecimal, Resources(R.string.response_card_card_data_token_decimal, R.string.info_response_card_card_data_token_decimal))
    }
}