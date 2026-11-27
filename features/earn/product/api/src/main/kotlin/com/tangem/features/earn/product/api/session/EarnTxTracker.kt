package com.tangem.features.earn.product.api.session

interface EarnTxTracker {

    fun onSent(ref: EarnTxRef)

    fun resume()
}