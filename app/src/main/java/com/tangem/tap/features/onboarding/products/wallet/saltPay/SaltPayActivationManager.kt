@file:Suppress("MaximumLineLength")

package com.tangem.tap.features.onboarding.products.wallet.saltPay

import android.net.Uri
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.SignedEthereumTransaction
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.isZero
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.common.services.performRequest
import com.tangem.datasource.api.paymentology.PaymentologyApiService
import com.tangem.datasource.api.paymentology.models.request.CheckRegistrationRequests
import com.tangem.datasource.api.paymentology.models.request.RegisterKYCRequest
import com.tangem.datasource.api.paymentology.models.request.RegisterWalletRequest
import com.tangem.datasource.api.paymentology.models.response.AttestationResponse
import com.tangem.datasource.api.paymentology.models.response.RegisterWalletResponse
import com.tangem.datasource.api.paymentology.models.response.RegistrationResponse
import com.tangem.datasource.api.paymentology.models.response.tryExtractError
import com.tangem.datasource.config.models.KYCProvider
import com.tangem.datasource.config.models.SaltPayConfig
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.domain.common.extensions.successOr
import com.tangem.operations.attestation.AttestWalletKeyResponse
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.ByteArray
import kotlin.Exception
import kotlin.IllegalStateException
import kotlin.NullPointerException
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.UnsupportedOperationException
import kotlin.byteArrayOf
import kotlin.minus
import com.tangem.blockchain.extensions.Result as BlockchainResult

/**
[REDACTED_AUTHOR]
 */
class SaltPayActivationManager(
    val cardId: String,
    val cardPublicKey: ByteArray,
    private val kycProvider: KYCProvider,
    private val paymentologyService: PaymentologyApiService,
    private val gnosisRegistrator: GnosisRegistrator,
) {

    val walletPublicKey = gnosisRegistrator.walletManager.wallet.publicKey.seedKey
    val kycUrlProvider = KYCUrlProvider(walletPublicKey, kycProvider)

    private val approvalValue: BigDecimal = BigDecimal(2).pow(256).minus(BigDecimal.ONE)
        .movePointLeft(gnosisRegistrator.walletManager.wallet.blockchain.decimals())

    private val spendLimitValue: BigDecimal = BigDecimal("100")

    suspend fun checkHasGas(): Result<Unit> {
        return when (val hasGasResult = gnosisRegistrator.checkHasGas()) {
            is BlockchainResult.Success -> if (hasGasResult.data) {
                Result.Success(Unit)
            } else {
                Result.Failure(SaltPayActivationError.NoGas)
            }
            is BlockchainResult.Failure -> Result.Failure(hasGasResult.error as BlockchainSdkError)
        }
    }

    suspend fun registerKYC(): Result<Unit> {
        val result = withContext(Dispatchers.IO) {
            performRequest { paymentologyService.api.registerKYC(makeRegisterKYCRequest()) }
        }
        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> result
        }
    }

    suspend fun checkRegistration(): Result<RegistrationResponse.Item> {
        val requestItem = CheckRegistrationRequests.Item(cardId, cardPublicKey.toHexString())
        val request = CheckRegistrationRequests(listOf(requestItem))

        val response: RegistrationResponse = withContext(Dispatchers.IO) {
            performRequest {
                paymentologyService.api.checkRegistration(request)
            }
        }
            .successOr { return it }
            .tryExtractError<RegistrationResponse>()
            .successOr { return it }

        return try {
            if (response.results.isEmpty()) throw SaltPayActivationError.EmptyResponse

            val item = response.results[0]
            if (item.error != null) throw IllegalStateException(response.makeErrorMessage())

            Result.Success(item)
        } catch (ex: Exception) {
            Result.Failure(ex)
        }
    }

    suspend fun requestAttestationChallenge(): Result<AttestationResponse> {
        val requestItem = CheckRegistrationRequests.Item(cardId, cardPublicKey.toHexString())
        return withContext(Dispatchers.IO) {
            performRequest {
                paymentologyService.api.requestAttestationChallenge(requestItem)
            }
        }
            .successOr { return it }
            .tryExtractError()
    }

    suspend fun sendTransactions(signedTransactions: List<SignedEthereumTransaction>): BlockchainResult<List<String>> {
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
        return withContext(Dispatchers.IO) {
            performRequest {
                paymentologyService.api.registerWallet(request)
            }
        }
            .successOr { return it }
            .tryExtractError()
    }

    suspend fun getAmountToClaim(): Result<Amount> {
        val amount = gnosisRegistrator.getAllowance().successOr {
            return Result.Failure(SaltPayActivationError.FailedToGetFundsToClaim)
        }

        return if (amount.value == null || amount.value?.isZero() == true) {
            Result.Failure(SaltPayActivationError.NoFundsToClaim)
        } else {
            Result.Success(amount)
        }
    }

    suspend fun claim(amountToClaim: BigDecimal, signer: TransactionSigner): Result<Unit> {
        gnosisRegistrator.transferFrom(amountToClaim, signer).successOr {
            val userCancelledError = (it.error as? BlockchainSdkError.WrappedTangemError)
                ?.tangemError as? TangemSdkError.UserCancelled

            return when (userCancelledError) {
                null -> Result.Failure(SaltPayActivationError.ClaimTransactionFailed)
                else -> Result.Failure(userCancelledError)
            }
        }
        return Result.Success(Unit)
    }

    suspend fun getTokenAmount(): Result<BigDecimal> {
        val wallet = gnosisRegistrator.walletManager.safeUpdate().successOr { return it }
        val token = wallet.getFirstToken().guard {
            throw UnsupportedOperationException()
        }

        val amountValue = wallet.amounts[AmountType.Token(token)]?.value.guard {
            throw UnsupportedOperationException()
        }

        return Result.Success(amountValue)
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
        fun stub(): SaltPayActivationManager = SaltPayActivationManager(
            kycProvider = SaltPayConfig.stub().kycProvider,
            paymentologyService = PaymentologyApiService,
            gnosisRegistrator = GnosisRegistrator.stub(),
            cardId = "",
            cardPublicKey = byteArrayOf(),
        )
    }
}

class KYCUrlProvider(
    walletPublicKey: ByteArray,
    kycProvider: KYCProvider,
) {

    val kycRefId = UserWalletIdBuilder.walletPublicKey(walletPublicKey).stringValue

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

class SaltPayActivationManagerFactory(
    private val blockchain: Blockchain,
    private val card: CardDTO,
) {

    fun create(): SaltPayActivationManager {
        val wallet = card.wallets.first()
        val saltPayConfig = store.state.globalState.configManager?.config?.saltPayConfig.guard {
            throw NullPointerException("SaltPayConfig is not initialized")
        }
        val wmFactory = store.state.globalState.tapWalletManager.walletManagerFactory
        val paymentologyService = store.state.domainNetworks.paymentologyService

        // return createDummyActivationManager(saltPayConfig, wmFactory, paymentologyService)
        return createActivationPayManager(
            cardId = card.cardId,
            cardPublicKey = card.cardPublicKey,
            kycProvider = saltPayConfig.kycProvider,
            gnosisRegistrator = GnosisRegistrator(
                walletManager = makeSaltPayWalletManager(
                    blockchain = blockchain,
                    walletPublicKey = wallet.publicKey,
                    wmFactory = wmFactory,
                ),
            ),
            paymentologyService = paymentologyService,
        )
    }

    private fun makeSaltPayWalletManager(
        blockchain: Blockchain,
        walletPublicKey: ByteArray,
        wmFactory: WalletManagerFactory,
    ): EthereumWalletManager {
        return wmFactory.makeWalletManager(
            blockchain = blockchain,
            publicKey = Wallet.PublicKey(walletPublicKey, null, null),
            tokens = listOf(SaltPayWorkaround.tokenFrom(blockchain)),
        ) as EthereumWalletManager
    }

    private fun createActivationPayManager(
        cardId: String,
        cardPublicKey: ByteArray,
        kycProvider: KYCProvider,
        gnosisRegistrator: GnosisRegistrator,
        paymentologyService: PaymentologyApiService,
    ): SaltPayActivationManager {
        return SaltPayActivationManager(
            cardId = cardId,
            cardPublicKey = cardPublicKey,
            kycProvider = kycProvider,
            paymentologyService = paymentologyService,
            gnosisRegistrator = gnosisRegistrator,
        )
    }

    @Suppress("UnusedPrivateMember")
    private fun createDummyActivationManager(
        saltPayConfig: SaltPayConfig,
        wmFactory: WalletManagerFactory,
        paymentologyService: PaymentologyApiService,
    ): SaltPayActivationManager {
        val fakeSource = DummySaltPayCardSource()

        val walletManager = wmFactory.makeWalletManager(
            blockchain = fakeSource.blockchain,
            publicKey = fakeSource.blockchainPublicKey,
            tokens = listOf(fakeSource.token),
        ) as EthereumWalletManager

        return createActivationPayManager(
            cardId = fakeSource.cardId,
            cardPublicKey = fakeSource.cardPublicKey,
            kycProvider = saltPayConfig.kycProvider,
            gnosisRegistrator = GnosisRegistrator(walletManager),
            paymentologyService = paymentologyService,
        )
    }

    private data class DummySaltPayCardSource(
        val blockchain: Blockchain = Blockchain.SaltPay,
        val cardId: String = "FF03000001001057",
        private val cardPublicKeyString: String = "039C2D2F9B68003766BFC29766761F55F4084E48B8B012BF439E115A79AB122D48",
        private val walletPublicKeyString: String = "0399B2DF5B129FBF69097DAA7C44FB5849E578CCFC22228A409870C74B56C4C3D1",
    ) {
        val cardPublicKey: ByteArray = cardPublicKeyString.hexToBytes()
        val walletPublicKey: ByteArray = walletPublicKeyString.hexToBytes()
        val blockchainPublicKey = Wallet.PublicKey(walletPublicKey, null, null)
        val token: Token = SaltPayWorkaround.tokenFrom(blockchain)
    }
}