package com.tangem.tap.domain.tasks.visa

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.*
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.common.map
import com.tangem.common.timemeasure.RealtimeMonotonicTimeSource
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.datasource.local.visa.VisaAuthTokenStorage
import com.tangem.datasource.local.visa.VisaOTPStorage
import com.tangem.datasource.local.visa.VisaOtpData
import com.tangem.datasource.local.visa.hasSavedOTP
import com.tangem.domain.common.visa.VisaUtilities
import com.tangem.domain.common.visa.VisaWalletPublicKeyUtility
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.error.VisaAuthorizationAPIError
import com.tangem.domain.visa.model.*
import com.tangem.domain.visa.repository.VisaActivationRepository
import com.tangem.domain.visa.repository.VisaAuthRepository
import com.tangem.operations.GenerateOTPCommand
import com.tangem.operations.attestation.AttestCardKeyCommand
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import com.tangem.operations.wallet.CreateWalletTask
import com.tangem.sdk.api.visa.VisaCardActivationResponse
import com.tangem.sdk.api.visa.VisaCardActivationTaskMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.time.measureTimedValue

@Suppress("LongParameterList", "LargeClass")
class VisaCardActivationTask @AssistedInject constructor(
    @Assisted private val mode: VisaCardActivationTaskMode,
    @Assisted private val activationInput: VisaActivationInput,
    @Assisted private val coroutineScope: CoroutineScope,
    private val otpStorage: VisaOTPStorage,
    private val visaAuthTokenStorage: VisaAuthTokenStorage,
    private val visaAuthRepository: VisaAuthRepository,
    private val visaActivationRepositoryFactory: VisaActivationRepository.Factory,
) : CardSessionRunnable<VisaCardActivationResponse> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    private class SessionContext(
        val visaActivationRepository: VisaActivationRepository,
        val cardId: String,
        val session: CardSession,
    )

    override fun run(session: CardSession, callback: CompletionCallback<VisaCardActivationResponse>) {
        coroutineScope.launch {
            callback(runSuspend(session))
        }
    }

    private suspend fun runSuspend(session: CardSession): CompletionResult<VisaCardActivationResponse> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        if (card.cardId != activationInput.cardId) {
            return CompletionResult.Failure(VisaActivationError.WrongCard.tangemError)
        }

        val visaActivationRepository = visaActivationRepositoryFactory.create(
            VisaCardId(
                cardId = card.cardId,
                cardPublicKey = card.cardPublicKey.toHexString(),
            ),
        )

        val context = SessionContext(
            visaActivationRepository = visaActivationRepository,
            cardId = card.cardId,
            session = session,
        )

        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            when (mode) {
                is VisaCardActivationTaskMode.Full -> {
                    context.signAuthorizationChallenge(mode.authorizationChallenge)
                }
                is VisaCardActivationTaskMode.SignOnly -> {
                    val wallet =
                        card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
                            ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

                    val derivedPublicKey = when (val deriveKeyResult = context.deriveKey(wallet.publicKey)) {
                        is CompletionResult.Failure -> {
                            return CompletionResult.Failure(deriveKeyResult.error)
                        }
                        is CompletionResult.Success -> {
                            deriveKeyResult.data
                        }
                    }

                    context.signData(
                        mode.dataToSignByCardWallet,
                        derivedPublicKey,
                    )
                }
            }
        }
        Timber.i("VisaCardActivationTask all time: ${timedResult.duration}")
        return timedResult.value
    }

    private suspend fun SessionContext.signAuthorizationChallenge(
        challengeToSign: VisaAuthChallenge.Card,
    ): CompletionResult<VisaCardActivationResponse> {
        val attestationCommand = AttestCardKeyCommand(challenge = challengeToSign.challenge.hexToBytes())
        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                attestationCommand.run(session = session) { attestationResponse ->
                    continuation.resume(attestationResponse)
                }
            }
        }
        Timber.i("AttestCardKeyCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("AttestCardKeyCommand success")
                processSignedAuthorizationChallenge(
                    signedChallenge = challengeToSign.toSignedChallenge(
                        signedChallenge = result.data.cardSignature.toHexString(),
                        salt = result.data.salt.toHexString(),
                    ),
                )
            }
            is CompletionResult.Failure -> {
                Timber.e("AttestCardKeyCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    private suspend fun SessionContext.processSignedAuthorizationChallenge(
        signedChallenge: VisaAuthSignedChallenge,
    ): CompletionResult<VisaCardActivationResponse> {
        when (val createWalletResult = createWallet()) {
            is CompletionResult.Failure -> return CompletionResult.Failure(createWalletResult.error)
            is CompletionResult.Success -> {}
        }

        val card =
            session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val wallet =
            card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
                ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val derivedPublicKey = when (val deriveKeyResult = deriveKey(wallet.publicKey)) {
            is CompletionResult.Failure -> {
                return CompletionResult.Failure(deriveKeyResult.error)
            }
            is CompletionResult.Success -> {
                deriveKeyResult.data
            }
        }

        val walletAddress = VisaWalletPublicKeyUtility.generateAddressOnSecp256k1(derivedPublicKey.publicKey)
            .getOrElse { return CompletionResult.Failure(it.tangemError) }
            .value

        return coroutineScope {
            val dataToSignDeferred =
                async { getDataToSign(signedChallenge = signedChallenge, cardWalletAddress = walletAddress) }
            val otpTaskDeferred = async { createOTP() }

            val dataToSign = dataToSignDeferred.await()
                .getOrElse {
                    otpTaskDeferred.cancel()
                    return@coroutineScope CompletionResult.Failure(it)
                }

            otpTaskDeferred.await()

            signData(
                dataToSign = dataToSign,
                derivedPublicKey = derivedPublicKey,
            )
        }
    }

    private suspend fun SessionContext.getDataToSign(
        signedChallenge: VisaAuthSignedChallenge,
        cardWalletAddress: String,
    ): Either<TangemError, VisaDataToSignByCardWallet> = either {
        catch(
            block = {
                val tokens = visaAuthRepository.getAccessTokens(signedChallenge)

                visaAuthTokenStorage.store(cardId, tokens)

                val remoteState = visaActivationRepository.getActivationRemoteState()
                if (remoteState !is VisaActivationRemoteState.CardWalletSignatureRequired) {
                    raise(VisaActivationError.WrongRemoteState.tangemError)
                }

                visaActivationRepository.getCardWalletAcceptanceData(
                    VisaCardWalletDataToSignRequest(
                        activationOrderInfo = remoteState.activationOrderInfo,
                        cardWalletAddress = cardWalletAddress,
                    ),
                )
            },
            catch = {
                raise(VisaAuthorizationAPIError.tangemError)
            },
        )
    }

    private suspend fun SessionContext.createWallet(): CompletionResult<Unit> {
        coroutineScope { ensureActive() }

        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        return if (card.wallets.any { it.curve == EllipticCurve.Secp256k1 }) {
            CompletionResult.Success(Unit)
        } else {
            val createWalletTask = CreateWalletTask(EllipticCurve.Secp256k1)

            val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
                suspendCancellableCoroutine { continuation ->
                    createWalletTask.run(session) { createWalletResult ->
                        continuation.resume(createWalletResult)
                    }
                }
            }

            Timber.i("CreateWalletTask time: ${timedResult.duration}")

            when (val result = timedResult.value) {
                is CompletionResult.Success -> {
                    Timber.i("CreateWalletTask success")
                    CompletionResult.Success(Unit)
                }
                is CompletionResult.Failure -> {
                    Timber.e("CreateWalletTask failure ${result.error}")
                    CompletionResult.Failure(result.error)
                }
            }
        }
    }

    private suspend fun SessionContext.createOTP(): CompletionResult<Unit> {
        coroutineScope { ensureActive() }

        if (otpStorage.hasSavedOTP(cardId)) {
            return CompletionResult.Success(Unit)
        }

        val otpCommand = GenerateOTPCommand()
        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                otpCommand.run(session) { otpResult ->
                    continuation.resume(otpResult)
                }
            }
        }

        Timber.i("GenerateOTPCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("GenerateOTPCommand success")
                otpStorage.saveOTP(
                    cardId = cardId,
                    data = VisaOtpData(result.data.rootOTP, result.data.rootOTPCounter),
                )
                CompletionResult.Success(Unit)
            }
            is CompletionResult.Failure -> {
                Timber.e("GenerateOTPCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    private suspend fun SessionContext.signData(
        dataToSign: VisaDataToSignByCardWallet,
        derivedPublicKey: ExtendedPublicKey,
    ): CompletionResult<VisaCardActivationResponse> {
        val card =
            session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val wallet =
            card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
                ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        val task = SignHashCommand(
            hash = dataToSign.hashToSign.hexToBytes(),
            walletPublicKey = wallet.publicKey,
            derivationPath = VisaUtilities.visaDefaultDerivationPath,
        )

        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                task.run(session) { signResult ->
                    continuation.resume(signResult)
                }
            }
        }

        Timber.i("SignHashCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("SignHashCommand success")
                handleSignedData(
                    dataToSign = dataToSign,
                    response = result.data,
                    derivedPublicKey = derivedPublicKey,
                )
            }
            is CompletionResult.Failure -> {
                Timber.e("SignHashCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    private suspend fun SessionContext.deriveKey(publicKey: ByteArray): CompletionResult<ExtendedPublicKey> {
        val derivationPath = VisaUtilities.visaDefaultDerivationPath
            ?: return CompletionResult.Failure(VisaActivationError.FailedToCreateAddress.tangemError)

        val derivationTask = DeriveWalletPublicKeyTask(publicKey, derivationPath)
        val derivationTaskResult = suspendCancellableCoroutine { continuation ->
            derivationTask.run(session) { result ->
                continuation.resume(result)
            }
        }

        return derivationTaskResult
    }

    private suspend fun SessionContext.handleSignedData(
        dataToSign: VisaDataToSignByCardWallet,
        derivedPublicKey: ExtendedPublicKey,
        response: SignHashResponse,
    ): CompletionResult<VisaCardActivationResponse> {
        val otp = otpStorage.getOTP(cardId) ?: run {
            createOTP()
            otpStorage.getOTP(cardId) ?: return CompletionResult.Failure(VisaActivationError.MissingRootOTP.tangemError)
        }

        val rsvSignature = UnmarshalHelper.unmarshalSignatureExtended(
            signature = response.signature,
            hash = dataToSign.hashToSign.hexToBytes(),
            publicKey = derivedPublicKey.publicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM().toHexString()

        val signedActivationData = dataToSign.sign(
            rootOTP = otp.rootOTP.toHexString(),
            otpCounter = otp.counter,
            signature = rsvSignature,
        )

        return setupAccessCode().map {
            val card = session.environment.card
                ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

            VisaCardActivationResponse(
                signedActivationData = signedActivationData,
                newCardDTO = CardDTO(card),
            )
        }
    }

    private suspend fun SessionContext.setupAccessCode(): CompletionResult<Unit> {
        val card = session.environment.card ?: return CompletionResult.Failure(TangemSdkError.MissingPreflightRead())

        if (card.isAccessCodeSet || mode !is VisaCardActivationTaskMode.Full) {
            return CompletionResult.Success(Unit)
        }

        Timber.i("Setting access code")

        val task = SetUserCodeCommand.changeAccessCode(mode.accessCode)

        val timedResult = RealtimeMonotonicTimeSource.measureTimedValue {
            suspendCancellableCoroutine { continuation ->
                task.run(session) { setAccessCodeResult ->
                    continuation.resume(setAccessCodeResult)
                }
            }
        }

        Timber.i("SetUserCodeCommand time: ${timedResult.duration}")

        return when (val result = timedResult.value) {
            is CompletionResult.Success -> {
                Timber.i("SetUserCodeCommand success")
                CompletionResult.Success(Unit)
            }
            is CompletionResult.Failure -> {
                Timber.i("SetUserCodeCommand failure ${result.error}")
                CompletionResult.Failure(result.error)
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            mode: VisaCardActivationTaskMode,
            activationInput: VisaActivationInput,
            coroutineScope: CoroutineScope,
        ): VisaCardActivationTask
    }
}