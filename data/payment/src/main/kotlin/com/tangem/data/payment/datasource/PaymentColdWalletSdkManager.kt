package com.tangem.data.payment.datasource

import arrow.core.*
import com.tangem.core.error.ext.tangemError
import com.tangem.data.payment.DefaultPaymentGenerateChallengeHelper
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.payment.auth.PaymentRemoteDataSource
import com.tangem.domain.payment.models.auth.PaymentAuthConfig
import com.tangem.domain.payment.models.auth.PaymentInitialCredentials
import com.tangem.sdk.api.TangemSdkManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class PaymentColdWalletSdkManager @AssistedInject constructor(
    @Assisted private val remoteDataSource: PaymentRemoteDataSource,
    private val tangemSdkManager: TangemSdkManager,
) {

    private val generateChallengeHelper by lazy(mode = LazyThreadSafetyMode.NONE) {
        DefaultPaymentGenerateChallengeHelper(remoteDataSource)
    }

    suspend fun produceInitialCredentials(
        coldWallet: UserWallet.Cold,
        config: PaymentAuthConfig,
    ): Either<Throwable, PaymentInitialCredentials> {
        val preflightReadFilter = UserWalletIdPreflightReadFilter(expectedUserWalletId = coldWallet.walletId)
        return tangemSdkManager.paymentGenerateAddressAndSignChallenge(
            preflightReadFilter = preflightReadFilter,
            config = config,
            userWalletId = coldWallet.walletId.stringValue,
            generateChallengeHelper = generateChallengeHelper,
        ).flatMap { data ->
            val authTokens = remoteDataSource.getTokenWithCustomerWallet(
                sessionId = data.challenge.session.sessionId,
                signature = data.signature,
                nonce = data.challenge.challenge,
            ).getOrElse { return IllegalStateException(it.tangemError.message).left() }

            PaymentInitialCredentials(customerWalletAddress = data.address, authTokens = authTokens).right()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(remoteDataSource: PaymentRemoteDataSource): PaymentColdWalletSdkManager
    }
}