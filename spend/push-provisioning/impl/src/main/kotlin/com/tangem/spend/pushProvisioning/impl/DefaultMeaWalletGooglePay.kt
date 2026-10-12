package com.tangem.spend.pushProvisioning.impl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayClient
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.google.android.gms.tasks.Task
import com.meawallet.mpp.GooglePayDataChangedListener
import com.meawallet.mpp.GooglePayHasEligibleTokenizationTargetListener
import com.meawallet.mpp.GooglePayRegisteredTokensListener
import com.meawallet.mpp.GooglePayTokenInfo
import com.meawallet.mpp.GooglePayTokenizeListener
import com.meawallet.mpp.MeaPushProvisioning
import com.meawallet.mpp.MppCardDataParameters
import com.meawallet.mpp.MppPaymentNetwork
import com.meawallet.mpp.MppPushCardToGooglePayListener
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DefaultMeaWalletGooglePay @Inject constructor(
    @ApplicationContext private val context: Context,
) : MeaWalletGooglePay {

    private val tapAndPay: TapAndPayClient by lazy { TapAndPay.getClient(context) }

    override val isOsSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    override val isProdSdk: Boolean
        get() = MeaPushProvisioning.Configuration.versionName().startsWith(PROD_SDK_VERSION_PREFIX)

    override fun initialize() {
        MeaPushProvisioning.initialize(context)
    }

    override fun isWalletAvailable(): Boolean = MeaPushProvisioning.GooglePay.isWalletAvailable(context)

    override fun checkWalletForCardSuffix(lastFourDigits: String, listener: GooglePayRegisteredTokensListener) {
        MeaPushProvisioning.GooglePay.checkWalletForCardSuffix(lastFourDigits, listener)
    }

    override fun hasEligibleTokenizationTarget(
        lastFourDigits: String,
        listener: GooglePayHasEligibleTokenizationTargetListener,
    ) {
        MeaPushProvisioning.GooglePay.hasEligibleTokenizationTarget(
            lastFourDigits,
            MppPaymentNetwork.VISA,
            null,
            listener,
        )
    }

    override fun checkStableHardwareId(onComplete: (errorCode: Int?) -> Unit) {
        tapAndPay.stableHardwareId.addOnCompleteListener { onComplete(it.errorCode) }
    }

    override fun checkActiveWallet(onComplete: (errorCode: Int?) -> Unit) {
        tapAndPay.activeWalletId.addOnCompleteListener { onComplete(it.errorCode) }
    }

    override fun getEnvironment(onComplete: (environment: Either<Int, String>) -> Unit) {
        tapAndPay.environment.addOnCompleteListener { task ->
            onComplete(task.errorCode?.left() ?: task.result.orEmpty().right())
        }
    }

    override fun push(
        card: GoogleWalletProvisioner.Card,
        activity: Activity,
        listener: MppPushCardToGooglePayListener,
    ) {
        MeaPushProvisioning.GooglePay.push(
            MppCardDataParameters.withCardSecret(card.processorCardId, card.cardSecret),
            card.displayName,
            null,
            activity,
            listener,
        )
    }

    override fun tokenize(
        token: TokenInfo,
        displayName: String,
        activity: Activity,
        listener: GooglePayTokenizeListener,
    ) {
        val googlePayToken = GooglePayTokenInfo(
            token.issuerTokenId,
            token.state,
            MppPaymentNetwork.getPaymentNetwork(token.network),
            token.isDefaultToken,
        )
        MeaPushProvisioning.GooglePay.tokenize(googlePayToken, displayName, activity, listener)
    }

    override fun handleOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?, activity: Activity) {
        MeaPushProvisioning.GooglePay.handleOnActivityResult(requestCode, resultCode, data, activity)
    }

    override fun registerDataChangedListener(listener: GooglePayDataChangedListener) {
        MeaPushProvisioning.GooglePay.registerDataChangedListener(listener)
    }

    override fun removeDataChangedListener(listener: GooglePayDataChangedListener) {
        MeaPushProvisioning.GooglePay.removeDataChangedListener(listener)
    }

    private companion object {
        const val PROD_SDK_VERSION_PREFIX = "mpp-prod"
    }
}

private val Task<*>.errorCode: Int?
    get() = if (isSuccessful) null else (exception as? ApiException)?.statusCode ?: CommonStatusCodes.ERROR