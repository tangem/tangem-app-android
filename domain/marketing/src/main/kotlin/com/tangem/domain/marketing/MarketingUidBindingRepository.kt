package com.tangem.domain.marketing

/** Fire and forget: never retried, never throws */
interface MarketingUidBindingRepository {

    suspend fun bind(uid: String)

    suspend fun bindPending()
}