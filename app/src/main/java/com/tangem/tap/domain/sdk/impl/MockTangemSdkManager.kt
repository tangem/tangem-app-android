package com.tangem.tap.domain.sdk.impl

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.SuccessResponse
import com.tangem.common.authentication.keystore.DummyKeystoreManager
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.InMemoryStorage
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.wallet.CreateWalletResponse
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse

@Suppress("TooManyFunctions")
class MockTangemSdkManager(
    private val resources: Resources,
) : TangemSdkManager {

    private var userCodeRequestPolicyInternal: UserCodeRequestPolicy = UserCodeRequestPolicy.Default

    override val canUseBiometry: Boolean = false

    override val needEnrollBiometrics: Boolean = false

    override val keystoreManager = DummyKeystoreManager()

    override val secureStorage = InMemoryStorage()

    override val userCodeRequestPolicy: UserCodeRequestPolicy
        get() = userCodeRequestPolicyInternal

    override suspend fun checkCanUseBiometry(awaitInitialization: Boolean): Boolean = canUseBiometry

    override suspend fun checkNeedEnrollBiometrics(awaitInitialization: Boolean): Boolean = needEnrollBiometrics

    override suspend fun scanProduct(
        cardId: String?,
        messageRes: Int?,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<ScanResponse> {
        return MockProvider.getScanResponse()
    }

    override suspend fun createProductWallet(
        scanResponse: ScanResponse,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
        return MockProvider.getCreateProductWalletResponse()
    }

    override suspend fun importWallet(
        scanResponse: ScanResponse,
        mnemonic: String,
        passphrase: String?,
        shouldReset: Boolean,
    ): CompletionResult<CreateProductWalletTaskResponse> {
        return MockProvider.getImportWalletResponse()
    }

    override suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): CompletionResult<DerivationTaskResponse> {
        return MockProvider.getDerivationTaskResponse()
    }

    override suspend fun deriveExtendedPublicKey(
        cardId: String?,
        walletPublicKey: ByteArray,
        derivation: DerivationPath,
    ): CompletionResult<ExtendedPublicKey> {
        return MockProvider.getExtendedPublicKey()
    }

    override suspend fun resetToFactorySettings(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        return MockProvider.getCardDto()
    }

    override suspend fun resetBackupCard(cardNumber: Int, userWalletId: UserWalletId): CompletionResult<Unit> {
        return CompletionResult.Success(Unit)
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
        return MockProvider.getSuccessResponse()
    }

    override suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return MockProvider.getSuccessResponse()
    }

    override suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        return MockProvider.getSuccessResponse()
    }

    override suspend fun setAccessCodeRecoveryEnabled(
        cardId: String?,
        enabled: Boolean,
    ): CompletionResult<SuccessResponse> {
        return MockProvider.getSuccessResponse()
    }

    override suspend fun scanCard(
        cardId: String?,
        allowRequestAccessCodeFromRepository: Boolean,
    ): CompletionResult<CardDTO> {
        return MockProvider.getCardDto()
    }

    override suspend fun <T> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        cardId: String?,
        initialMessage: Message?,
        accessCode: String?,
        @DrawableRes iconScanRes: Int?,
    ): CompletionResult<T> = error("This method is deprecated")

    override fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?) {
        // intentionally do nothing
    }

    @Deprecated("TangemSdkManager shouldn't returns a string from resources")
    override fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String {
        return resources.getString(stringResId, *formatArgs)
    }

    override fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy) {
        userCodeRequestPolicyInternal = policy
    }

    // region Twin-specific

    override suspend fun createFirstTwinWallet(
        cardId: String,
        initialMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        return MockProvider.createFirstTwinWallet()
    }

    override suspend fun createSecondTwinWallet(
        firstPublicKey: String,
        firstCardId: String,
        issuerKeys: KeyPair,
        preparingMessage: Message,
        creatingWalletMessage: Message,
        initialMessage: Message,
    ): CompletionResult<CreateWalletResponse> {
        return MockProvider.createSecondTwinWallet()
    }

    override suspend fun finalizeTwin(
        secondCardPublicKey: ByteArray,
        issuerKeyPair: KeyPair,
        cardId: String,
        initialMessage: Message,
    ): CompletionResult<ScanResponse> {
        return MockProvider.finalizeTwin()
    }

    // endregion
}