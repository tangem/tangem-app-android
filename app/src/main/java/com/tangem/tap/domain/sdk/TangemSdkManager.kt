package com.tangem.tap.domain.sdk

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.Message
import com.tangem.common.*
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.core.*
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse

interface TangemSdkManager {

    val canUseBiometry: Boolean

    val needEnrollBiometrics: Boolean

    val keystoreManager: KeystoreManager

    val secureStorage: SecureStorage

    val userCodeRequestPolicy: UserCodeRequestPolicy

    suspend fun scanProduct(
        cardId: String? = null,
        messageRes: Int? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse>

    suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean = false,
    ): CompletionResult<CreateProductWalletTaskResponse>

    suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse>

    suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): CompletionResult<DerivationTaskResponse>

    suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey>

    suspend fun resetToFactorySettings(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO>

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

    suspend fun <T> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
        accessCode: String? = null,
        @DrawableRes iconScanRes: Int? = null,
    ): CompletionResult<T>

    @Suppress("MagicNumber")
    fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?)

    @Deprecated("TangemSdkManager shouldn't returns a string from resources")
    fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String

    fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy)
}