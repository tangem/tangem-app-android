package com.tangem.tap.domain.sdk.impl

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.Message
import com.tangem.common.*
import com.tangem.common.authentication.keystore.DummyKeystoreManager
import com.tangem.common.core.*
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.InMemoryStorage
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

    override val canUseBiometry = false

    override val needEnrollBiometrics = false

    override val keystoreManager = DummyKeystoreManager()

    override val secureStorage = InMemoryStorage()

    override val userCodeRequestPolicy: UserCodeRequestPolicy
        get() = userCodeRequestPolicyInternal

    private var userCodeRequestPolicyInternal: UserCodeRequestPolicy = UserCodeRequestPolicy.Default

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
        return CompletionResult.Success(MockProvider.getDerivationTaskResponse())
    }

    override suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        return CompletionResult.Success(MockProvider.getExtendedPublicKey())
    }

    override suspend fun resetToFactorySettings(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        return CompletionResult.Success(MockProvider.getCardDto())
    }

    override suspend fun saveAccessCode(accessCode: String, cardsIds: Set<String>): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun clearSavedUserCodes(): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
    }

    override suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
        return CompletionResult.Success(MockProvider.getSuccessResponse())
    }

    override suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return CompletionResult.Success(MockProvider.getSuccessResponse())
    }

    override suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        return CompletionResult.Success(MockProvider.getSuccessResponse())
    }

    override suspend fun setAccessCodeRecoveryEnabled(
        cardId: String?,
        enabled: Boolean,
    ): CompletionResult<SuccessResponse> {
        return CompletionResult.Success(MockProvider.getSuccessResponse())
    }

    override suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        return CompletionResult.Success(MockProvider.getCardDto())
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
        userCodeRequestPolicyInternal = policy
    }
}
