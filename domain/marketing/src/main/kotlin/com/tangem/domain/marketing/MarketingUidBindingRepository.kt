package com.tangem.domain.marketing

/** Fire and forget: never retried, never throws. Suspends until a wallet exists, on the server too, so launch it */
interface MarketingUidBindingRepository {

    suspend fun bind(uid: String)
}