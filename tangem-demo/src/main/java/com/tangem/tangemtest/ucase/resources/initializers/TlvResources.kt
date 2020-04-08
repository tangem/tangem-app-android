package com.tangem.tangemtest.ucase.resources.initializers

import com.tangem.tangemtest.R
import com.tangem.tangemtest.ucase.resources.MainResourceHolder
import com.tangem.tangemtest.ucase.resources.Resources
import com.tangem.tangemtest.ucase.variants.TlvId

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
    }
}