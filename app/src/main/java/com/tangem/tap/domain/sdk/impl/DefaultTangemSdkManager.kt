package com.tangem.tap.domain.sdk.impl

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.*
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.*
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.services.secure.AccessCodeRepository
import com.tangem.common.services.secure.CardAccessTokensRepository
import com.tangem.common.services.secure.SecureStorage
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.res.getStringSafe
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.visa.model.*
import com.tangem.domain.wallets.derivations.DerivationsHelper
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
import com.tangem.hot.sdk.android.PassphraseValidator
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.operations.ScanTask
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.preflightread.PreflightReadFilter
import com.tangem.operations.usersetttings.SetUserCodeRecoveryAllowedTask
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.CreateProductWalletTaskResponse
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.api.visa.VisaCardActivationResponse
import com.tangem.sdk.api.visa.VisaCardActivationTaskMode
import com.tangem.tap.common.analytics.events.TangemSdkErrorEvent
import com.tangem.tap.common.analytics.paramsInterceptor.CardContextInterceptor
import com.tangem.tap.domain.tasks.product.*
import com.tangem.tap.domain.tasks.visa.*
import com.tangem.tap.domain.twins.CreateFirstTwinWalletTask
import com.tangem.tap.domain.twins.CreateSecondTwinWalletTask
import com.tangem.tap.domain.twins.FinalizeTwinTask
import com.tangem.tap.domain.visa.VisaCardScanHandler
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import com.tangem.utils.logging.TangemLogger
import com.tangem.wallet.R
import dagger.Lazy
import kotlinx.coroutines.*
import kotlin.coroutines.resume

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
internal class DefaultTangemSdkManager(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val resources: Resources,
    private val visaCardScanHandler: VisaCardScanHandler,
    private val visaCardActivationTaskFactory: VisaCardActivationTask.Factory,
    private val tangemPayChallengeTaskFactory: TangemPayGenerateAddressAndSignChallengeTask.Factory,
    private val tangemPayMoneyAccountTaskFactory: TangemPayGenerateMoneyAccountAddressTask.Factory,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
    private val analyticsErrorHandler: AnalyticsErrorHandler,
    private val cardRepository: CardRepository,
    private val derivationsHelper: DerivationsHelper,
    // Lazy breaks a DI cycle: the launcher -> hot wallet accessor -> LegacySettingsRepository ->
    // TangemSdkManager. It's only needed when a scan actually runs.
    private val walletRegistrationLauncher: Lazy<WalletRegistrationLauncher>,
    private val authFeatureToggles: AuthFeatureToggles,
) : TangemSdkManager {

    private val tangemSdk: TangemSdk
        get() = cardSdkConfigRepository.sdk

    private val userCodeRepository by lazy {
        AccessCodeRepository(
            keystoreManager = tangemSdk.keystoreManager,
            secureStorage = tangemSdk.secureStorage,
        )
    }

    private val cardAccessTokensRepository by lazy {
        CardAccessTokensRepository(
            keystoreManager = tangemSdk.keystoreManager,
            secureStorage = tangemSdk.secureStorage,
        )
    }

    override val isEnrollBiometricsNeeded: Boolean
        get() {
            val isNeedEnrollBiometrics = tangemSdk.authenticationManager.needEnrollBiometrics
            if (isNeedEnrollBiometrics) {
                analyticsErrorHandler.sendErrorEvent(
                    AnalyticsEvent(
                        category = "TangemSdkManager",
                        event = "needEnrollBiometrics",
                    ),
                )
            }
            return isNeedEnrollBiometrics
        }

    override val canUseBiometry: Boolean
        get() {
            val isCanUseBiometry = tangemSdk.authenticationManager.canAuthenticate || isEnrollBiometricsNeeded
            if (!isCanUseBiometry) {
                analyticsErrorHandler.sendErrorEvent(
                    AnalyticsEvent(
                        category = "TangemSdkManager",
                        event = "cantUseBiometry",
                    ),
                )
            }
            return isCanUseBiometry
        }

    override val keystoreManager: KeystoreManager
        get() = tangemSdk.keystoreManager

    override val secureStorage: SecureStorage
        get() = tangemSdk.secureStorage

    override val userCodeRequestPolicy: UserCodeRequestPolicy
        get() = tangemSdk.config.userCodeRequestPolicy

    override suspend fun checkNeedEnrollBiometrics(awaitInitialization: Boolean): Boolean {
        return isEnrollBiometricsNeeded
    }

    override suspend fun checkCanUseBiometry(awaitInitialization: Boolean): Boolean {
        return canUseBiometry
    }

    override suspend fun scanProduct(
        cardId: String?,
        messageRes: Int?,
        allowsRequestAccessCodeFromRepository: Boolean,
        shouldCheckIsAlreadyActivated: Boolean,
        source: AnalyticsParam.ScreensSources,
    ): CompletionResult<ScanResponse> {
        val message = Message(resources.getStringSafe(messageRes ?: R.string.initial_message_scan_header))
        return coroutineScope {
            runTaskAsyncReturnOnMain(
                runnable = ScanProductTask(
                    card = null,
                    allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
                    visaCardScanHandler = visaCardScanHandler,
                    sessionCoroutineScope = this,
                    shouldCheckIsAlreadyActivated = shouldCheckIsAlreadyActivated,
                    onboardingV2FeatureToggles = onboardingV2FeatureToggles,
                    cardRepository = cardRepository,
                ),
                cardId = cardId,
                initialMessage = message,
                registerColdWallet = true,
            ).also { sendAnalytics(result = it, source = source) }
        }
    }

    override suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
        tangemSdk.config.setupForProduct(
            if (scanResponse.cardTypesResolver.isRing()) {
                ProductType.RING
            } else {
                ProductType.CARD
            },
        )

        return runTaskAsync(
            runnable = CreateProductWalletTask(
                cardTypesResolver = scanResponse.cardTypesResolver,
                shouldReset = shouldReset,
                derivationsHelper = derivationsHelper,
            ),
            preflightReadFilter = null,
            cardId = scanResponse.card.cardId,
            initialMessage = if (scanResponse.cardTypesResolver.isRing()) {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body_ring))
            } else {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body))
            },
            iconScanRes = if (scanResponse.cardTypesResolver.isRing()) R.drawable.img_hand_scan_ring else null,
            registerColdWallet = true,
        )
            .doOnResult { tangemSdk.config.setupForProduct(ProductType.ANY) }
    }

    override suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
        shouldForceColdRegistration: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
        // The card itself has no passphrase length limit, but a wallet the mobile wallet cannot reproduce
        // would break both the upgrade between them and importing the same seed phrase twice
        val passphraseError = runCatching {
            PassphraseValidator.validate(passphrase.orEmpty().toCharArray())
        }.exceptionOrNull()
        if (passphraseError != null) {
            return CompletionResult.Failure(TangemSdkError.ExceptionError(passphraseError))
        }

        val defaultMnemonic = try {
            DefaultMnemonic(mnemonic, tangemSdk.wordlist)
        } catch (e: TangemSdkError.MnemonicException) {
            return CompletionResult.Failure(e)
        }

        tangemSdk.config.setupForProduct(
            if (scanResponse.cardTypesResolver.isRing()) {
                ProductType.RING
            } else {
                ProductType.CARD
            },
        )

        return startSessionAsync(
            runnable = CreateProductWalletTask(
                cardTypesResolver = scanResponse.cardTypesResolver,
                derivationsHelper = derivationsHelper,
                mnemonic = defaultMnemonic,
                passphrase = passphrase,
                shouldReset = shouldReset,
            ),
            preflightReadFilter = null,
            cardId = scanResponse.card.cardId,
            initialMessage = if (scanResponse.cardTypesResolver.isRing()) {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body_ring))
            } else {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body))
            },
            accessCode = null,
            iconScanRes = null,
            registerColdWallet = true,
            shouldForceColdRegistration = shouldForceColdRegistration,
        )
            .doOnResult { tangemSdk.config.setupForProduct(ProductType.ANY) }
    }

    private fun sendAnalytics(result: CompletionResult<ScanResponse>, source: AnalyticsParam.ScreensSources) {
        result
            .doOnSuccess { scanResponse ->
                // We don't use the standard analytics event enrichment mechanism with card context params here,
                // because we've just scanned a new card that may not yet be selected as the current one
                val analyticsEvent = Basic.CardWasScanned(source)
                val interceptor = CardContextInterceptor(scanResponse)
                val params = analyticsEvent.params.toMutableMap()
                interceptor.intercept(params)

                Analytics.send(event = analyticsEvent.withParams(params.toMap()))
            }
            .doOnFailure { tangemError ->
                TangemLogger.e("scanProduct failed: code=${tangemError.code}, message=${tangemError.customMessage}")
                (tangemError as? TangemSdkError)?.let { error ->
                    Analytics.sendErrorEvent(TangemSdkErrorEvent(error))
                }
            }
    }

    override suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
        preflightReadFilter: PreflightReadFilter?,
    ): CompletionResult<DerivationTaskResponse> {
        return runTaskAsyncReturnOnMain(
            runnable = DeriveMultipleWalletPublicKeysTask(derivations),
            cardId = cardId,
            preflightReadFilter = preflightReadFilter,
            registerColdWallet = true,
        )
    }

    override suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> = withContext(Dispatchers.Main) {
        runTaskAsyncReturnOnMain(
            runnable = DeriveWalletPublicKeyTask(walletPublicKey, derivation),
            cardId = cardId,
            registerColdWallet = true,
        )
    }

    override suspend fun resetToFactorySettings(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<Boolean> {
        return runTaskAsyncReturnOnMain(
            runnable = ResetToFactorySettingsTask(
                allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
            ),
            cardId = cardId,
            initialMessage = Message(resources.getStringSafe(R.string.card_settings_reset_card_to_factory)),
            registerColdWallet = false, // reset wipes the card — nothing to register
        )
    }

    override suspend fun resetBackupCard(cardNumber: Int, userWallet: UserWallet.Cold): CompletionResult<Boolean> {
        return runTaskAsyncReturnOnMain(
            runnable = ResetBackupCardTask(userWallet),
            initialMessage = Message(
                resources.getStringSafe(
                    R.string.initial_message_reset_backup_card_header,
                    cardNumber.toString(),
                ),
            ),
            registerColdWallet = false, // reset wipes the card — nothing to register
        )
    }

    override suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit> {
        cardAccessTokensRepository.deleteTokens(cardsIds)
        return userCodeRepository.delete(cardsIds.toSet())
    }

    override suspend fun clearSavedUserCodes(): CompletionResult<Unit> {
        cardAccessTokensRepository.clear()
        return userCodeRepository.clear()
    }

    override suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            runnable = SetUserCodeCommand.changePasscode(null),
            cardId = cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_change_passcode_body)),
            registerColdWallet = false,
        )
    }

    override suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            runnable = SetUserCodeCommand.changeAccessCode(null),
            cardId = cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_change_access_code_body)),
            registerColdWallet = false,
        )
    }

    override suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            runnable = SetUserCodeCommand.resetUserCodes(),
            cardId = cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
            registerColdWallet = false,
        )
    }

    override suspend fun setAccessCodeRecoveryEnabled(
        cardId: String?,
        enabled: Boolean,
    ): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            runnable = SetUserCodeRecoveryAllowedTask(enabled),
            cardId = cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
            registerColdWallet = false,
        )
    }

    override suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        return runTaskAsyncReturnOnMain(
            runnable = ScanTask(allowRequestAccessCodeFromRepository),
            cardId = cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
            registerColdWallet = false,
        )
            .map { CardDTO(it) }
    }

    override suspend fun <T> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        preflightReadFilter: PreflightReadFilter?,
        cardId: String?,
        initialMessage: Message?,
        accessCode: String?,
        @DrawableRes iconScanRes: Int?,
        registerColdWallet: Boolean,
    ): CompletionResult<T> = startSessionAsync(
        runnable = runnable,
        preflightReadFilter = preflightReadFilter,
        cardId = cardId,
        initialMessage = initialMessage,
        accessCode = accessCode,
        iconScanRes = iconScanRes,
        registerColdWallet = registerColdWallet,
        shouldForceColdRegistration = false,
    )

    @Suppress("LongParameterList")
    private suspend fun <T> startSessionAsync(
        runnable: CardSessionRunnable<T>,
        preflightReadFilter: PreflightReadFilter?,
        cardId: String?,
        initialMessage: Message?,
        accessCode: String?,
        @DrawableRes iconScanRes: Int?,
        registerColdWallet: Boolean,
        shouldForceColdRegistration: Boolean,
    ): CompletionResult<T> = coroutineScope {
        val actualRunnable = if (registerColdWallet) {
            withColdWalletRegistration(runnable, shouldForceColdRegistration)
        } else {
            runnable
        }
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                tangemSdk.startSessionWithRunnable(
                    runnable = actualRunnable,
                    cardId = cardId,
                    initialMessage = initialMessage,
                    accessCode = accessCode,
                    preflightReadFilter = preflightReadFilter,
                    iconScanRes = iconScanRes,
                ) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        }
    }

    private suspend fun <T> runTaskAsyncReturnOnMain(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
        preflightReadFilter: PreflightReadFilter? = null,
        registerColdWallet: Boolean,
    ): CompletionResult<T> {
        val result = runTaskAsync(
            runnable = runnable,
            preflightReadFilter = preflightReadFilter,
            cardId = cardId,
            initialMessage = initialMessage,
            registerColdWallet = registerColdWallet,
        )
        return withContext(Dispatchers.Main) { result }
    }

    /**
     * Wraps [runnable] so that the card is bound to the device via cold wallet registration on the
     * same NFC tap — after [runnable] succeeds, or mid-scan before the card attestation (see
     * [ColdWalletRegistrationHost]). Applied only when the call site opts in via
     * `registerColdWallet = true` (and the backend-auth toggle is on) — currently scan / wallet
     * creation / key derivation; signing opts in separately in `TangemSigner`, and the primary
     * card's backup finalization calls this directly. Twin / Visa /
     * walletless cards are skipped inside [WalletRegistrationLauncher]. The registration is launched
     * on the card session's own scope (see [RegisterColdWalletRunnable]), so it can't outlive the tap.
     */
    override fun <T> withColdWalletRegistration(
        runnable: CardSessionRunnable<T>,
        shouldForceRegistration: Boolean,
    ): CardSessionRunnable<T> {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return runnable

        return RegisterColdWalletRunnable(
            inner = runnable,
            launcher = walletRegistrationLauncher.get(),
            shouldForceRegistration = shouldForceRegistration,
        )
    }

    @Suppress("MagicNumber")
    override fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?) {
        tangemSdk.config.cardIdDisplayFormat = when {
            scanResponse == null -> CardIdDisplayFormat.Full
            scanResponse.cardTypesResolver.isTangemTwins() -> CardIdDisplayFormat.LastLuhn(4)
            else -> CardIdDisplayFormat.Full
        }
    }

    @Deprecated("TangemSdkManager shouldn't returns a string from resources")
    override fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String {
        val args = formatArgs.toSet().filterNotNull().toTypedArray()

        return resources.getStringSafe(stringResId, *args)
    }

    override fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy) {
        tangemSdk.config.userCodeRequestPolicy = policy
    }

    // region Twin-specific

    override suspend fun createFirstTwinWallet(
        cardId: String,
        initialMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        return runTaskAsync(
            runnable = CreateFirstTwinWalletTask(cardId),
            preflightReadFilter = null,
            cardId = cardId,
            initialMessage = initialMessage,
            registerColdWallet = false,
        )
    }

    override suspend fun createSecondTwinWallet(
        firstPublicKey: String,
        firstCardId: String,
        issuerKeys: KeyPair,
        preparingMessage: Message,
        creatingWalletMessage: Message,
        initialMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        val task = CreateSecondTwinWalletTask(
            firstPublicKey = firstPublicKey,
            firstCardId = firstCardId,
            issuerKeys = issuerKeys,
            preparingMessage = preparingMessage,
            creatingWalletMessage = creatingWalletMessage,
        )
        return runTaskAsync(
            runnable = task,
            preflightReadFilter = null,
            initialMessage = initialMessage,
            registerColdWallet = false,
        )
    }

    override fun changeProductType(isRing: Boolean) {
        tangemSdk.config.setupForProduct(
            type = if (isRing) ProductType.RING else ProductType.CARD,
        )
    }

    override fun clearProductType() {
        tangemSdk.config.setupForProduct(type = ProductType.ANY)
    }

    override suspend fun finalizeTwin(
        secondCardPublicKey: ByteArray,
        issuerKeyPair: KeyPair,
        cardId: String,
        initialMessage: Message,
    ): CompletionResult<ScanResponse> {
        return runTaskAsync(
            runnable = FinalizeTwinTask(
                twinPublicKey = secondCardPublicKey,
                issuerKeys = issuerKeyPair,
                cardRepository = cardRepository,
            ),
            preflightReadFilter = null,
            cardId = cardId,
            initialMessage = initialMessage,
            registerColdWallet = false,
        )
    }

    // endregion

    // region Visa-specific

    override suspend fun activateVisaCard(
        mode: VisaCardActivationTaskMode,
        activationInput: VisaActivationInput,
    ): CompletionResult<VisaCardActivationResponse> {
        return coroutineScope {
            runTaskAsyncReturnOnMain(
                runnable = visaCardActivationTaskFactory.create(
                    mode = mode,
                    activationInput = activationInput,
                    coroutineScope = this,
                ),
                cardId = activationInput.cardId,
                initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
                registerColdWallet = false, // Visa — not a cold wallet registration target
            )
        }
    }

    override suspend fun visaCustomerWalletApprove(
        visaDataForApprove: VisaDataForApprove,
    ): CompletionResult<VisaSignedDataByCustomerWallet> {
        return runTaskAsyncReturnOnMain(
            runnable = VisaCustomerWalletApproveTask(
                VisaCustomerWalletApproveTask.Input(
                    cardId = visaDataForApprove.customerWalletCardId,
                    targetAddress = visaDataForApprove.targetAddress,
                    hashToSign = visaDataForApprove.dataToSign.hashToSign,
                    sign = visaDataForApprove.dataToSign::sign,
                ),
            ),
            cardId = visaDataForApprove.customerWalletCardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
            registerColdWallet = false, // Visa — not a cold wallet registration target
        )
    }

    override suspend fun tangemPayProduceInitialCredentials(
        preflightReadFilter: PreflightReadFilter,
    ): Either<Throwable, TangemPayInitialCredentials> {
        return coroutineScope {
            val result = runTaskAsyncReturnOnMain(
                runnable = tangemPayChallengeTaskFactory.create(coroutineScope = this),
                cardId = null,
                initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
                preflightReadFilter = preflightReadFilter,
                registerColdWallet = false, // Visa — not a cold wallet registration target
            )

            return@coroutineScope when (result) {
                is CompletionResult.Failure<*> -> result.error.left()
                is CompletionResult.Success<TangemPayInitialCredentials> -> result.data.right()
            }
        }
    }

    override suspend fun tangemPayProduceMoneyAccountData(
        preflightReadFilter: PreflightReadFilter,
    ): Either<Throwable, MoneyAccountActivationData> {
        return coroutineScope {
            val result = runTaskAsyncReturnOnMain(
                runnable = tangemPayMoneyAccountTaskFactory.create(coroutineScope = this),
                cardId = null,
                initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
                preflightReadFilter = preflightReadFilter,
                registerColdWallet = false, // Visa — not a cold wallet registration target
            )

            return@coroutineScope when (result) {
                is CompletionResult.Failure<*> -> result.error.left()
                is CompletionResult.Success<MoneyAccountActivationData> -> result.data.right()
            }
        }
    }

    override suspend fun getWithdrawalSignature(
        hash: String,
        preflightReadFilter: PreflightReadFilter,
    ): Either<Throwable, WithdrawalSignatureResult> {
        return coroutineScope {
            val result = runTaskAsyncReturnOnMain(
                runnable = TangemPaySignWithdrawalHashTask(hash = hash.hexToBytes()),
                cardId = null,
                initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
                preflightReadFilter = preflightReadFilter,
                registerColdWallet = false, // Visa — not a cold wallet registration target
            )

            return@coroutineScope when (result) {
                is CompletionResult.Failure<*> -> {
                    if (result.error is TangemSdkError.UserCancelled) {
                        WithdrawalSignatureResult.Cancelled.right()
                    } else {
                        result.error.left()
                    }
                }
                is CompletionResult.Success<String> -> {
                    WithdrawalSignatureResult.Success(result.data).right()
                }
            }
        }
    }

    // endregion
}