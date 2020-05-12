package com.tangem.devkit.ucase.resources.initializers

import com.tangem.devkit.R
import com.tangem.devkit.ucase.resources.MainResourceHolder
import com.tangem.devkit.ucase.resources.Resources
import com.tangem.devkit.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class TlvResources {
    fun init(holder: MainResourceHolder) {
        initScan(holder)
    }

    private fun initScan(holder: MainResourceHolder) {
        holder.register(TlvId.CardId, Resources(R.string.tlv_card_id, R.string.info_tlv_card_id))
        holder.register(TlvId.TransactionOutHash, Resources(R.string.tlv_transaction_out_hash, R.string.info_tlv_transaction_out_hash))
        holder.register(TlvId.Counter, Resources(R.string.tlv_counter, R.string.info_tlv_counter))
        holder.register(TlvId.IssuerData, Resources(R.string.tlv_issuer_data, R.string.info_tlv_issuer_data))
        holder.register(TlvId.UserData, Resources(R.string.tlv_user_data, R.string.info_tlv_user_data))
        holder.register(TlvId.ProtectedUserData, Resources(R.string.tlv_user_protected_data, R.string.info_tlv_protected_user_data))
    }
}