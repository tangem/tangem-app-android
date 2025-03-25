package com.tangem.tap.domain.sdk.impl

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.Log
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.*
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.*
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.common.usersCode.UserCodeRepository
import com.tangem.core.analytics.Analytics
import com.tangem.core.res.getStringSafe
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaActivationInput
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.onboarding.v2.OnboardingV2FeatureToggles
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
import com.tangem.tap.derivationsFinder
import com.tangem.tap.domain.tasks.product.CreateProductWalletTask
import com.tangem.tap.domain.tasks.product.ResetBackupCardTask
import com.tangem.tap.domain.tasks.product.ResetToFactorySettingsTask
import com.tangem.tap.domain.tasks.product.ScanProductTask
import com.tangem.tap.domain.tasks.visa.VisaCardActivationTask
import com.tangem.tap.domain.tasks.visa.VisaCustomerWalletApproveTask
import com.tangem.tap.domain.twins.CreateFirstTwinWalletTask
import com.tangem.tap.domain.twins.CreateSecondTwinWalletTask
import com.tangem.tap.domain.twins.FinalizeTwinTask
import com.tangem.tap.domain.visa.VisaCardScanHandler
import com.tangem.wallet.R
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

@Suppress("TooManyFunctions", "LargeClass")
internal class DefaultTangemSdkManager(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val resources: Resources,
    private val visaCardScanHandler: VisaCardScanHandler,
    private val visaCardActivationTaskFactory: VisaCardActivationTask.Factory,
    private val onboardingV2FeatureToggles: OnboardingV2FeatureToggles,
) : TangemSdkManager {

    private val awaitInitializationMutex = Mutex()

    private val tangemSdk: TangemSdk
        get() = cardSdkConfigRepository.sdk

    private val userCodeRepository by lazy {
        UserCodeRepository(
            keystoreManager = tangemSdk.keystoreManager,
            secureStorage = tangemSdk.secureStorage,
        )
    }

    override val canUseBiometry: Boolean
        get() = tangemSdk.authenticationManager.canAuthenticate || needEnrollBiometrics

    override val needEnrollBiometrics: Boolean
        get() = tangemSdk.authenticationManager.needEnrollBiometrics

    override val keystoreManager: KeystoreManager
        get() = tangemSdk.keystoreManager

    override val secureStorage: SecureStorage
        get() = tangemSdk.secureStorage

    override val userCodeRequestPolicy: UserCodeRequestPolicy
        get() = tangemSdk.config.userCodeRequestPolicy

    override suspend fun checkNeedEnrollBiometrics(awaitInitialization: Boolean): Boolean {
        return try {
            needEnrollBiometrics
        } catch (e: TangemSdkError.AuthenticationNotInitialized) {
            Log.error {
                "Trying to access `needEnrollBiometrics` flag when authentication manager is not initialized: " +
                    if (awaitInitialization) "awaiting initialization" else "failing"
            }

            if (awaitInitialization) {
                awaitAuthenticationManagerInitialization().needEnrollBiometrics
            } else {
                throw e
            }
        }
    }

    override suspend fun checkCanUseBiometry(awaitInitialization: Boolean): Boolean {
        return try {
            canUseBiometry
        } catch (e: TangemSdkError.AuthenticationNotInitialized) {
            Log.error {
                "Trying to access `canUseBiometry` flag when authentication manager is not initialized: " +
                    if (awaitInitialization) "awaiting initialization" else "failing"
            }

            if (awaitInitialization) {
                val manager = awaitAuthenticationManagerInitialization()

                manager.canAuthenticate || manager.needEnrollBiometrics
            } else {
                throw e
            }
        }
    }

    override suspend fun scanProduct(
        cardId: String?,
        messageRes: Int?,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<ScanResponse> {
        val message = Message(resources.getStringSafe(messageRes ?: R.string.initial_message_scan_header))
        return coroutineScope {
            runTaskAsyncReturnOnMain(
                runnable = ScanProductTask(
                    card = null,
                    derivationsFinder = derivationsFinder,
                    allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
                    visaCardScanHandler = visaCardScanHandler,
                    visaCoroutineScope = this,
                    onboardingV2FeatureToggles = onboardingV2FeatureToggles,
                ),
                cardId = cardId,
                initialMessage = message,
            ).also { sendScanResultsToAnalytics(it) }
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
                derivationStyleProvider = scanResponse.derivationStyleProvider,
                shouldReset = shouldReset,
            ),
            cardId = scanResponse.card.cardId,
            initialMessage = if (scanResponse.cardTypesResolver.isRing()) {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body_ring))
            } else {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body))
            },
            iconScanRes = if (scanResponse.cardTypesResolver.isRing()) R.drawable.img_hand_scan_ring else null,
            preflightReadFilter = null,
        )
            .doOnResult { tangemSdk.config.setupForProduct(ProductType.ANY) }
    }

    override suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
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

        return runTaskAsync(
            runnable = CreateProductWalletTask(
                cardTypesResolver = scanResponse.cardTypesResolver,
                derivationStyleProvider = scanResponse.derivationStyleProvider,
                mnemonic = defaultMnemonic,
                passphrase = passphrase,
                shouldReset = shouldReset,
            ),
            cardId = scanResponse.card.cardId,
            initialMessage = if (scanResponse.cardTypesResolver.isRing()) {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body_ring))
            } else {
                Message(resources.getStringSafe(R.string.initial_message_create_wallet_body))
            },
            preflightReadFilter = null,
        )
            .doOnResult { tangemSdk.config.setupForProduct(ProductType.ANY) }
    }

    private fun sendScanResultsToAnalytics(result: CompletionResult<ScanResponse>) {
        if (result is CompletionResult.Failure) {
            (result.error as? TangemSdkError)?.let { error ->
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
        )
    }

    override suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> = withContext(Dispatchers.Main) {
        runTaskAsyncReturnOnMain(
            DeriveWalletPublicKeyTask(walletPublicKey, derivation),
            cardId,
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
        )
    }

    override suspend fun resetBackupCard(cardNumber: Int, userWalletId: UserWalletId): CompletionResult<Boolean> {
        return runTaskAsyncReturnOnMain(
            runnable = ResetBackupCardTask(userWalletId),
            initialMessage = Message(
                resources.getStringSafe(
                    R.string.initial_message_reset_backup_card_header,
                    cardNumber.toString(),
                ),
            ),
        )
    }

    override suspend fun saveAccessCode(accessCode: String, cardsIds: Set<String>): CompletionResult<Unit> {
        return userCodeRepository.save(
            cardsIds = cardsIds,
            userCode = UserCode(
                type = UserCodeType.AccessCode,
                stringValue = accessCode,
            ),
        )
    }

    override suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit> {
        return userCodeRepository.delete(cardsIds.toSet())
    }

    override suspend fun clearSavedUserCodes(): CompletionResult<Unit> {
        return userCodeRepository.clear()
    }

    override suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changePasscode(null),
            cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_change_passcode_body)),
        )
    }

    override suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changeAccessCode(null),
            cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_change_access_code_body)),
        )
    }

    override suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.resetUserCodes(),
            cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
        )
    }

    override suspend fun setAccessCodeRecoveryEnabled(
        cardId: String?,
        enabled: Boolean,
    ): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeRecoveryAllowedTask(enabled),
            cardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
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
    ): CompletionResult<T> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            tangemSdk.startSessionWithRunnable(
                runnable = runnable,
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

    private suspend fun <T> runTaskAsyncReturnOnMain(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
        preflightReadFilter: PreflightReadFilter? = null,
    ): CompletionResult<T> {
        val result = runTaskAsync(
            runnable = runnable,
            cardId = cardId,
            initialMessage = initialMessage,
            preflightReadFilter = preflightReadFilter,
        )
        return withContext(Dispatchers.Main) { result }
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

    private suspend fun awaitAuthenticationManagerInitialization(): AuthenticationManager {
        return awaitInitializationMutex.withLock {
            var attemps = 0

            do {
                if (tangemSdk.authenticationManager.isInitialized) {
                    break
                } else {
                    if (attemps++ >= MAX_INITIALIZE_ATTEMPTS) {
                        error("Can't initialize authentication manager after $MAX_INITIALIZE_ATTEMPTS attempts")
                    } else {
                        delay(timeMillis = 200)
                    }
                }
            } while (true)

            tangemSdk.authenticationManager
        }
    }

    // region Twin-specific

    override suspend fun createFirstTwinWallet(
        cardId: String,
        initialMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        return runTaskAsync(
            runnable = CreateFirstTwinWalletTask(cardId),
            cardId = cardId,
            initialMessage = initialMessage,
            preflightReadFilter = null,
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
        return runTaskAsync(runnable = task, cardId = null, initialMessage = initialMessage, preflightReadFilter = null)
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
            runnable = FinalizeTwinTask(secondCardPublicKey, issuerKeyPair),
            cardId = cardId,
            initialMessage = initialMessage,
            preflightReadFilter = null,
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
            )
        }
    }

    override suspend fun visaCustomerWalletApprove(
        visaDataForApprove: VisaDataForApprove,
    ): CompletionResult<VisaSignedDataByCustomerWallet> {
        return runTaskAsyncReturnOnMain(
            runnable = VisaCustomerWalletApproveTask(
                visaDataForApprove = visaDataForApprove,
            ),
            cardId = visaDataForApprove.customerWalletCardId,
            initialMessage = Message(resources.getStringSafe(R.string.initial_message_tap_header)),
        )
    }

    // endregion

    companion object {
        private const val MAX_INITIALIZE_ATTEMPTS = 10
    }
}