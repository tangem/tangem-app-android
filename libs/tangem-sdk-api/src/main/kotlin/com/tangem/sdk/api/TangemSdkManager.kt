package com.tangem.sdk.api

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.SuccessResponse
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaActivationInput
import com.tangem.domain.visa.model.VisaDataForApprove
import com.tangem.domain.visa.model.VisaSignedDataByCustomerWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.preflightread.PreflightReadFilter
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.sdk.api.visa.VisaCardActivationResponse
import com.tangem.sdk.api.visa.VisaCardActivationTaskMode

@Suppress("TooManyFunctions")
interface TangemSdkManager {

    val canUseBiometry: Boolean

    val needEnrollBiometrics: Boolean

    val keystoreManager: KeystoreManager

    val secureStorage: SecureStorage

    val userCodeRequestPolicy: UserCodeRequestPolicy

    suspend fun checkCanUseBiometry(awaitInitialization: Boolean = true): Boolean

    suspend fun checkNeedEnrollBiometrics(awaitInitialization: Boolean = true): Boolean

    suspend fun scanProduct(
        cardId: String? = null,
        messageRes: Int? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse>

    suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean = false,
    ): CompletionResult<CreateProductWalletTaskResponse>

    // Wallet2 specific
    suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
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

    suspend fun resetBackupCard(cardNumber: Int, userWalletId: UserWalletId): CompletionResult<Boolean>

    suspend fun saveAccessCode(accessCode: String, cardsIds: Set<String>): CompletionResult<Unit>

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
    ): CompletionResult<T>

    @Suppress("MagicNumber")
    fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?)

    @Deprecated("com.tangem.sdk.api.TangemSdkManager shouldn't returns a string from resources")
    fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String

    fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy)

    // region Twin-specific

    suspend fun finalizeTwin(
        secondCardPublicKey: ByteArray,
        issuerKeyPair: KeyPair,
        cardId: String,
        initialMessage: Message,
    ): CompletionResult<ScanResponse>

    suspend fun createFirstTwinWallet(cardId: String, initialMessage: Message): CompletionResult<CreateWalletResponse>

    @Suppress("LongParameterList")
    suspend fun createSecondTwinWallet(
        firstPublicKey: String,
        firstCardId: String,
        issuerKeys: KeyPair,
        preparingMessage: Message,
        creatingWalletMessage: Message,
        initialMessage: Message,
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

    // endregion
}