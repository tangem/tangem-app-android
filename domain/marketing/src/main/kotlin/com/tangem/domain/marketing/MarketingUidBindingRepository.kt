package com.tangem.domain.marketing

/** Fire and forget: never retried, never throws. Suspends until the wallet list has a wallet, so launch it */
interface MarketingUidBindingRepository {

    suspend fun bind(uid: String)
}