package com.tangem.tap.features.onboarding.products.wallet.saltPay

import android.net.Uri
import com.tangem.blockchain.blockchains.ethereum.SignedEthereumTransaction
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.successOr
import com.tangem.network.api.paymentology.AttestationResponse
import com.tangem.network.api.paymentology.PaymentologyApiService
import com.tangem.network.api.paymentology.RegisterKYCRequest
import com.tangem.network.api.paymentology.RegisterWalletRequest
import com.tangem.network.api.paymentology.RegisterWalletResponse
import com.tangem.network.api.paymentology.RegistrationResponse
import com.tangem.operations.attestation.AttestWalletKeyResponse
import com.tangem.tap.domain.tokens.UserWalletId
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.tap.persistence.SaltPayRegistrationStorage
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class SaltPayRegistrationManager(
    val cardId: String,
    val cardPublicKey: ByteArray,
    val walletPublicKey: ByteArray,
    private val kycProvider: KYCProvider,
    private val paymentologyService: PaymentologyApiService,
    private val gnosisRegistrator: GnosisRegistrator,
    private val registrationStorage: SaltPayRegistrationStorage,
) {
    val kycUrlProvider = KYCUrlProvider(walletPublicKey, kycProvider)

    private val approvalValue: BigDecimal = BigDecimal.valueOf(Math.pow(2.toDouble(), 255.toDouble()))
    private val spendLimitValue: BigDecimal = BigDecimal("100")

    fun transactionIsSent(): Boolean {
        return registrationStorage.data.transactionsSent
    }

    suspend fun checkHasGas(): Result<Unit> {
        return when (val hasGasResult = gnosisRegistrator.checkHasGas()) {
            is com.tangem.blockchain.extensions.Result.Success -> if (hasGasResult.data) {
                Result.Success(Unit)
            } else {
                Result.Failure(SaltPayRegistrationError.NoGas)
            }
            is com.tangem.blockchain.extensions.Result.Failure -> Result.Failure(SaltPayRegistrationError.NoGas)
        }
    }

    suspend fun registerKYC(): Result<Unit> {
        return when (val result = paymentologyService.registerKYC(makeRegisterKYCRequest())) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> result
        }
    }

    suspend fun checkRegistration(): Result<RegistrationResponse.Item> {
        val registrationResponse = paymentologyService.checkRegistration(cardId, cardPublicKey).successOr { return it }

        return try {
            if (!registrationResponse.success || registrationResponse.results.isEmpty()) {
                throw SaltPayRegistrationError.EmptyResponse(registrationResponse.error)
            }
            Result.Success(registrationResponse.results[0])
        } catch (ex: SaltPayRegistrationError) {
            Result.Failure(ex)
        }
    }

    suspend fun requestAttestationChallenge(): Result<AttestationResponse> {
        return paymentologyService.requestAttestationChallenge(cardId, cardPublicKey)
    }

    suspend fun sendTransactions(
        signedTransactions: List<SignedEthereumTransaction>,
    ): com.tangem.blockchain.extensions.Result<List<String>> {
        return gnosisRegistrator.sendTransactions(signedTransactions)
    }

    suspend fun registerWallet(
        attestResponse: AttestWalletKeyResponse,
        pinCode: String,
    ): Result<RegisterWalletResponse> {
        val request = RegisterWalletRequest(
            cardId = cardId,
            publicKey = cardPublicKey,
            walletPublicKey = walletPublicKey,
            walletSalt = attestResponse.salt,
            walletSignature = attestResponse.walletSignature,
            cardSalt = attestResponse.publicKeySalt ?: byteArrayOf(),
            cardSignature = attestResponse.cardSignature ?: byteArrayOf(),
            pin = pinCode,
        )

        val result = paymentologyService.registerWallet(request)

        registrationStorage.data = registrationStorage.data.copy(
            transactionsSent = result is Result.Success,
        )

        return result
    }

    fun makeRegistrationTask(challenge: ByteArray): SaltPayRegistrationTask {
        return SaltPayRegistrationTask(
            gnosisRegistrator = gnosisRegistrator,
            challenge = challenge,
            walletPublicKey = walletPublicKey,
            approvalValue = approvalValue,
            spendLimitValue = spendLimitValue,
        )
    }

    private fun makeRegisterKYCRequest(): RegisterKYCRequest = RegisterKYCRequest(
        cardId = cardId,
        publicKey = cardPublicKey,
        kycProvider = "UTORG",
        kycRefId = kycUrlProvider.kycRefId,
    )

    companion object {
        fun stub(): SaltPayRegistrationManager = SaltPayRegistrationManager(
            kycProvider = SaltPayConfig.stub().kycProvider,
            paymentologyService = PaymentologyApiService.stub(),
            gnosisRegistrator = GnosisRegistrator.stub(),
            registrationStorage = SaltPayRegistrationStorage.stub(),
            cardId = "",
            cardPublicKey = byteArrayOf(),
            walletPublicKey = byteArrayOf(),
        )
    }
}

class KYCUrlProvider(
    walletPublicKey: ByteArray,
    kycProvider: KYCProvider,
) {

    val kycRefId = UserWalletId(walletPublicKey).stringValue

    val requestUrl: String = makeWebRequestUrl(kycProvider)

    val doneUrl = "https://success.tangem.com/"

    private fun makeWebRequestUrl(kycProvider: KYCProvider): String {
        val baseUri = Uri.parse(kycProvider.baseUrl)
        return Uri.Builder()
            .scheme(baseUri.scheme)
            .authority(baseUri.authority)
            .encodedPath(baseUri.path)
            .appendQueryParameter(kycProvider.sidParameterKey, kycProvider.sidValue)
            .appendQueryParameter(kycProvider.externalIdParameterKey, kycRefId)
            .build().toString()
    }
}
