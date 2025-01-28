package com.tangem.data.visa

import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.datasource.api.visa.TangemVisaAuthApi
import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaAuthSession
import com.tangem.domain.visa.model.VisaAuthSignedChallenge
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("UnusedPrivateMember")
internal class DefaultVisaAuthRepository @Inject constructor(
    private val visaAuthApi: TangemVisaAuthApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : VisaAuthRepository {

    override suspend fun getCardAuthChallenge(cardId: String, cardPublicKey: String): VisaAuthChallenge.Card =
        withContext(dispatchers.io) {
            // val response = visaAuthApi.generateNonceByCard(
            //     cardId = cardId,
            //     cardPublicKey = cardPublicKey,
            // )
            //
            // VisaAuthChallenge.Card(
            //     challenge = response.nonce,
            //     session = VisaAuthSession(response.sessionId),
            // )

            VisaAuthChallenge.Card(
                challenge = CryptoUtils.generateRandomBytes(length = 16).toHexString(),
                session = VisaAuthSession("session"),
            )
        }

    override suspend fun getCustomerWalletAuthChallenge(
        cardId: String,
        walletPublicKey: String,
    ): VisaAuthChallenge.Wallet = withContext(dispatchers.io) {
        // val response = visaAuthApi.generateNonceByWalletAddress(
        //     customerId = cardId,
        //     customerWalletAddress = walletPublicKey,
        // )
        //
        // VisaAuthChallenge.Wallet(
        //     challenge = response.nonce,
        //     session = VisaAuthSession(response.sessionId),
        // )
        VisaAuthChallenge.Wallet(
            challenge = CryptoUtils.generateRandomBytes(length = 32).toHexString(),
            session = VisaAuthSession("session"),
        )
    }

    override suspend fun getAccessTokens(signedChallenge: VisaAuthSignedChallenge): VisaAuthTokens =
        withContext(dispatchers.io) {
            // val response = when (signedChallenge) {
            //     is VisaAuthSignedChallenge.ByCardPublicKey -> {
            //         visaAuthApi.getAccessToken(
            //             sessionId = signedChallenge.challenge.session.sessionId,
            //             signature = signedChallenge.signature,
            //             salt = signedChallenge.salt,
            //         )
            //     }
            //     is VisaAuthSignedChallenge.ByWallet -> {
            //         visaAuthApi.getAccessToken(
            //             sessionId = signedChallenge.challenge.session.sessionId,
            //             signature = signedChallenge.signature,
            //             salt = null,
            //         )
            //     }
            // }
            //
            // VisaAuthTokens(
            //     accessToken = response.accessToken,
            //     refreshToken = response.refreshToken,
            // )
            VisaAuthTokens(
                accessToken = "accessToken",
                refreshToken = "refreshToken2",
            )
        }
}