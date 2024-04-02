package com.tangem.tap.domain

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.common.*
import com.tangem.common.authentication.keystore.KeystoreManager
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.*
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.services.secure.SecureStorage
import com.tangem.common.usersCode.UserCodeRepository
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.crypto.bip39.DefaultMnemonic
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.ScanTask
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.operations.usersetttings.SetUserCodeRecoveryAllowedTask
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.tap.derivationsFinder
import com.tangem.tap.domain.tasks.product.CreateProductWalletTask
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse
import com.tangem.tap.domain.tasks.product.ResetToFactorySettingsTask
import com.tangem.tap.domain.tasks.product.ScanProductTask
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Suppress("TooManyFunctions")
class MockTangemSdkManager(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val resources: Resources,
) : TangemSdkManager {

    override val canUseBiometry: Boolean
        get() = TODO()

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
        TODO()
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

    override suspend fun setAccessCodeRecoveryEnabled(cardId: String?, enabled: Boolean): CompletionResult<SuccessResponse> {
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
        TODO()
    }

    @Deprecated("TangemSdkManager shouldn't returns a string from resources")
    override fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String {
        return resources.getString(stringResId, *formatArgs)
    }

    override fun setUserCodeRequestPolicy(policy: UserCodeRequestPolicy) {
        TODO()
    }
}
