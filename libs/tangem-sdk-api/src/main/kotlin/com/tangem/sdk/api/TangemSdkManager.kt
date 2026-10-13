package com.tangem.sdk.api

import androidx.annotation.DrawableRes
import arrow.core.Either
import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.SuccessResponse
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.domain.visa.model.MoneyAccountActivationData
import com.tangem.domain.visa.model.VisaActivationInput
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.preflightread.PreflightReadFilter
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.visa.VisaCardActivationResponse
import com.tangem.sdk.api.visa.VisaCardActivationTaskMode

@Suppress("TooManyFunctions")
interface TangemSdkManager {

    val canUseBiometry: Boolean

    val isEnrollBiometricsNeeded: Boolean

    val keystoreManager: KeystoreManager

    val secureStorage: SecureStorage

    val userCodeRequestPolicy: UserCodeRequestPolicy

    suspend fun checkCanUseBiometry(awaitInitialization: Boolean = true): Boolean

    suspend fun checkNeedEnrollBiometrics(awaitInitialization: Boolean = true): Boolean

    /**
     * Low-level card scan. Don't call it from features: scan through `ScanCardProcessor`, which also handles the card
     * attestation failure dialog, the failed scans counter, the card ID display format and onboarding.
     *
     * @param cardId                                scans only the card with this ID; any card when `null`
     * @param allowsRequestAccessCodeFromRepository allows using the saved access code
     * @param shouldCheckIsAlreadyActivated         lets the SDK confirm that the wallet belongs to the user
     * ("This is my wallet"); off when re-scanning a card already known to the app
     */
    suspend fun scanProduct(
        cardId: String? = null,
        messageRes: Int? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
        shouldCheckIsAlreadyActivated: Boolean,
        source: AnalyticsParam.ScreensSources,
    ): CompletionResult<ScanResponse>

    suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean = false,
    ): CompletionResult<CreateProductWalletTaskResponse>

    // Wallet2 specific
    /**
     * [shouldForceColdRegistration] re-registers the card with the backend even if its walletId is
     * already marked registered — set when the card takes over an existing mobile wallet (upgrade).
     */
    suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
        shouldForceColdRegistration: Boolean = false,
    ): CompletionResult<CreateProductWalletTaskResponse>

    suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
        preflightReadFilter: PreflightReadFilter?,
    ): CompletionResult<DerivationTaskResponse>

    suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey>

    suspend fun resetToFactorySettings(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<Boolean>

    suspend fun resetBackupCard(cardNumber: Int, userWallet: UserWallet.Cold): CompletionResult<Boolean>

    suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit>

    suspend fun clearSavedUserCodes(): CompletionResult<Unit>

    suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse>

    suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse>

    suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse>

    suspend fun setAccessCodeRecoveryEnabled(cardId: String?, enabled: Boolean): CompletionResult<SuccessResponse>

    suspend fun scanCard(
        cardId: String? = null,
        allowRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<CardDTO>

    @Deprecated(
        "com.tangem.sdk.api.TangemSdkManager shouldn't run custom tasks. " +
            "All of them should be specified in com.tangem.sdk.api.TangemSdkManager certain methods.",
    )
    suspend fun <T> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        preflightReadFilter: PreflightReadFilter?,
        cardId: String? = null,
        initialMessage: Message? = null,
        accessCode: String? = null,
        @DrawableRes iconScanRes: Int? = null,
        registerColdWallet: Boolean,
    ): CompletionResult<T>

    /**
     * Wraps [runnable] so that, once it succeeds, the cold wallet is registered with the backend on the same NFC tap,
     * for a card session that isn't started through this manager (e.g. a backup step). Returns [runnable] unchanged
     * when backend authentication is disabled. [shouldForceRegistration] registers a wallet that is already registered
     * again, so the backend gets its up-to-date state.
     */
    fun <T> withColdWalletRegistration(
        runnable: CardSessionRunnable<T>,
        shouldForceRegistration: Boolean,
    ): CardSessionRunnable<T>

    @Suppress("MagicNumber")
    fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?)

    fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy)

    // region Twin-specific

    suspend fun finalizeTwin(
        secondCardPublicKey: ByteArray,
        issuerKeyPair: KeyPair,
        cardId: String,
    ): CompletionResult<ScanResponse>

    suspend fun createFirstTwinWallet(cardId: String): CompletionResult<CreateWalletResponse>

    suspend fun createSecondTwinWallet(
        firstPublicKey: String,
        firstCardId: String,
        issuerKeys: KeyPair,
    ): CompletionResult<CreateWalletResponse>

    fun changeProductType(isRing: Boolean)

    fun clearProductType()

    // endregion

    // region Visa-specific

    suspend fun activateVisaCard(
        mode: VisaCardActivationTaskMode,
        activationInput: VisaActivationInput,
    ): CompletionResult<VisaCardActivationResponse>

    suspend fun visaCustomerWalletApprove(
        visaDataForApprove: VisaDataForApprove,
    ): CompletionResult<VisaSignedDataByCustomerWallet>

    suspend fun tangemPayProduceInitialCredentials(
        preflightReadFilter: PreflightReadFilter,
    ): Either<Throwable, TangemPayInitialCredentials>

    suspend fun tangemPayProduceMoneyAccountData(
        preflightReadFilter: PreflightReadFilter,
    ): Either<Throwable, MoneyAccountActivationData>

    suspend fun getWithdrawalSignature(
        hash: String,
        preflightReadFilter: PreflightReadFilter,
    ): Either<Throwable, WithdrawalSignatureResult>
    // endregion
}