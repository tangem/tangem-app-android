package com.tangem

interface CardManagerDelegate {

    fun showSecurityDelay(seconds: Int)
    fun hideSecurityDelay()
    fun requestPin(success: () -> String, error: (cardError: CardError) -> CardError)

    fun openNfcPopup()
    fun closeNfcPopup()

}