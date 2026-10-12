package com.tangem.spend.pushProvisioning.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.meawallet.mpp.GooglePayHasEligibleTokenizationTargetListener
import com.meawallet.mpp.GooglePayRegisteredTokensListener
import com.meawallet.mpp.GooglePayTokenizeListener
import com.meawallet.mpp.MppError
import com.meawallet.mpp.MppPaymentNetwork
import com.meawallet.mpp.MppPushCardToGooglePayListener
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult

internal fun registeredTokensListener(complete: (TokenLookup) -> Unit) = object : GooglePayRegisteredTokensListener {
    override fun onSuccess(tokens: List<TokenInfo>) {
        complete(tokens.right())
    }

    override fun onFailure(error: MppError) {
        complete(error.toTokenLookup())
    }
}

internal fun eligibilityListener(complete: (Either<String, Boolean>) -> Unit) =
    object : GooglePayHasEligibleTokenizationTargetListener {
        override fun onSuccess(hasEligibleTarget: Boolean) {
            complete(hasEligibleTarget.right())
        }

        override fun onFailure(error: MppError) {
            complete(error.describe().left())
        }
    }

internal fun pushListener(complete: (AddResult) -> Unit) = object : MppPushCardToGooglePayListener {
    override fun onSuccess(tokenReferenceId: String?, cardLastFourDigits: String?, cardNetwork: MppPaymentNetwork?) {
        complete(AddResult.Added)
    }

    override fun onFailure(error: MppError) {
        complete(error.toAddResult())
    }
}

internal fun tokenizeListener(complete: (AddResult) -> Unit) = object : GooglePayTokenizeListener {
    override fun onSuccess() {
        complete(AddResult.Added)
    }

    override fun onFailure(error: MppError) {
        complete(error.toAddResult())
    }
}