package com.tangem.tap.domain.sdk.impl

import android.content.res.Resources
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
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
class MockTangemSdkManager(
    private val resources: Resources,
) : TangemSdkManager {

    override val canUseBiometry: Boolean
        get() = false

    override val needEnrollBiometrics: Boolean
        get() = TODO()

    override val keystoreManager: KeystoreManager
        get() = TODO()

    override val secureStorage: SecureStorage
        get() = TODO()

    override val userCodeRequestPolicy: UserCodeRequestPolicy
        get() = TODO()

    override suspend fun scanProduct(
        cardId: String?,
        messageRes: Int?,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<ScanResponse> {
        return CompletionResult.Success(MockProvider.getScanResponse())
    }

    override suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
        TODO()
    }

    override suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
        TODO()
    }

    override suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): CompletionResult<DerivationTaskResponse> {
        TODO()
    }

    override suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        TODO()
    }

    override suspend fun resetToFactorySettings(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        TODO()
    }

    override suspend fun saveAccessCode(accessCode: String, cardsIds: Set<String>): CompletionResult<Unit> {
        TODO()
    }

    override suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit> {
        TODO()
    }

    override suspend fun clearSavedUserCodes(): CompletionResult<Unit> {
        TODO()
    }

    override suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
        TODO()
    }

    override suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        TODO()
    }

    override suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        TODO()
    }

    override suspend fun setAccessCodeRecoveryEnabled(
        cardId: String?,
        enabled: Boolean,
    ): CompletionResult<SuccessResponse> {
        TODO()
    }

    override suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        TODO()
    }

    override suspend fun <T> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        cardId: String?,
        initialMessage: Message?,
        accessCode: String?,
        @DrawableRes iconScanRes: Int?,
    ): CompletionResult<T> = withContext(Dispatchers.Main) {
        TODO()
    }

    @Suppress("MagicNumber")
    override fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?) {
    }

    @Deprecated("TangemSdkManager shouldn't returns a string from resources")
    override fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String {
        return resources.getString(stringResId, *formatArgs)
    }

    override fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy) {
        TODO()
    }
}