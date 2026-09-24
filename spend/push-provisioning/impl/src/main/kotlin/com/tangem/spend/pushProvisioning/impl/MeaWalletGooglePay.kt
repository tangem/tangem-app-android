package com.tangem.spend.pushProvisioning.impl

import android.app.Activity
import android.content.Intent
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.meawallet.mpp.GooglePayDataChangedListener
import com.meawallet.mpp.GooglePayHasEligibleTokenizationTargetListener
import com.meawallet.mpp.GooglePayRegisteredTokensListener
import com.meawallet.mpp.GooglePayTokenizeListener
import com.meawallet.mpp.MppPushCardToGooglePayListener
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner

internal interface MeaWalletGooglePay {

    val isOsSupported: Boolean

    val isProdSdk: Boolean

    fun initialize()

    fun isWalletAvailable(): Boolean

    fun checkWalletForCardSuffix(lastFourDigits: String, listener: GooglePayRegisteredTokensListener)

    fun hasEligibleTokenizationTarget(lastFourDigits: String, listener: GooglePayHasEligibleTokenizationTargetListener)

    fun push(card: GoogleWalletProvisioner.Card, activity: Activity, listener: MppPushCardToGooglePayListener)

    fun tokenize(token: TokenInfo, displayName: String, activity: Activity, listener: GooglePayTokenizeListener)

    fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity)

    fun registerDataChangedListener(listener: GooglePayDataChangedListener)

    fun removeDataChangedListener(listener: GooglePayDataChangedListener)
}