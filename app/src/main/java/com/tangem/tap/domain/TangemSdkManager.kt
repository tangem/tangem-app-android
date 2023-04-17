package com.tangem.tap.domain

import android.content.Context
import androidx.annotation.StringRes
import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CardFilter
import com.tangem.common.CompletionResult
import com.tangem.common.SuccessResponse
import com.tangem.common.UserCode
import com.tangem.common.UserCodeType
import com.tangem.common.biometric.BiometricManager
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.Config
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.map
import com.tangem.common.usersCode.UserCodeRepository
import com.tangem.core.analytics.Analytics
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.ScanResponse
import com.tangem.operations.CommandResponse
import com.tangem.operations.ScanTask
import com.tangem.operations.derivation.DerivationTaskResponse
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import com.tangem.operations.pins.CheckUserCodesCommand
import com.tangem.operations.pins.CheckUserCodesResponse
import com.tangem.operations.pins.SetUserCodeCommand
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.domain.tasks.CreateWalletAndRescanTask
import com.tangem.tap.domain.tasks.product.CreateProductWalletTask
import com.tangem.tap.domain.tasks.product.CreateProductWalletTaskResponse
import com.tangem.tap.domain.tasks.product.ResetToFactorySettingsTask
import com.tangem.tap.domain.tasks.product.ScanProductTask
import com.tangem.tap.domain.tokens.UserTokensRepository
import com.tangem.wallet.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class TangemSdkManager(private val tangemSdk: TangemSdk, private val context: Context) {

    private val userCodeRepository by lazy {
        UserCodeRepository(
            biometricManager = tangemSdk.biometricManager,
            secureStorage = tangemSdk.secureStorage,
        )
    }

    val canUseBiometry: Boolean
        get() = tangemSdk.biometricManager.canAuthenticate || needEnrollBiometrics

    val needEnrollBiometrics: Boolean
        get() = tangemSdk.biometricManager.canEnrollBiometrics

    val biometricManager: BiometricManager
        get() = tangemSdk.biometricManager

    suspend fun scanProduct(
        userTokensRepository: UserTokensRepository,
        cardId: String? = null,
        additionalBlockchainsToDerive: Collection<Blockchain>? = null,
        messageRes: Int? = null,
        allowsRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<ScanResponse> {
        val message = Message(context.getString(messageRes ?: R.string.initial_message_scan_header))
        return runTaskAsyncReturnOnMain(
            runnable = ScanProductTask(
                card = null,
                userTokensRepository = userTokensRepository,
                additionalBlockchainsToDerive = additionalBlockchainsToDerive,
                allowsRequestAccessCodeFromRepository = allowsRequestAccessCodeFromRepository,
            ),
            cardId = cardId,
            initialMessage = message,
        ).also { sendScanResultsToAnalytics(it) }
    }

    suspend fun createProductWallet(scanResponse: ScanResponse): CompletionResult<CreateProductWalletTaskResponse> {
        return runTaskAsync(
            CreateProductWalletTask(scanResponse.cardTypesResolver),
            scanResponse.card.cardId,
            Message(context.getString(R.string.initial_message_create_wallet_body)),
        )
    }

    private fun sendScanResultsToAnalytics(result: CompletionResult<ScanResponse>) {
        if (result is CompletionResult.Failure) {
            (result.error as? TangemSdkError)?.let { error ->
                Analytics.send(Basic.ScanError(error))
            }
        }
    }

    suspend fun createWallet(cardId: String?): CompletionResult<CardDTO> {
        return runTaskAsyncReturnOnMain(
            CreateWalletAndRescanTask(),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_create_wallet_body)),
        )
            .map { CardDTO(it) }
    }

    suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>,
    ): CompletionResult<DerivationTaskResponse> {
        return runTaskAsyncReturnOnMain(DeriveMultipleWalletPublicKeysTask(derivations), cardId)
    }

    suspend fun resetToFactorySettings(cardId: String): CompletionResult<CardDTO> {
        return runTaskAsyncReturnOnMain(
            runnable = ResetToFactorySettingsTask(),
            cardId = cardId,
            initialMessage = Message(context.getString(R.string.card_settings_reset_card_to_factory)),
        )
            .map { CardDTO(it) }
    }

    suspend fun saveAccessCode(accessCode: String, cardsIds: Set<String>): CompletionResult<Unit> {
        return userCodeRepository.save(
            cardsIds = cardsIds,
            userCode = UserCode(
                type = UserCodeType.AccessCode,
                stringValue = accessCode,
            ),
        )
    }

    suspend fun deleteSavedUserCodes(cardsIds: Set<String>): CompletionResult<Unit> {
        return userCodeRepository.delete(cardsIds.toSet())
    }

    suspend fun clearSavedUserCodes(): CompletionResult<Unit> {
        return userCodeRepository.clear()
    }

    suspend fun setPasscode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changePasscode(null),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_change_passcode_body)),
        )
    }

    suspend fun setAccessCode(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.changeAccessCode(null),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_change_access_code_body)),
        )
    }

    suspend fun setLongTap(cardId: String?): CompletionResult<SuccessResponse> {
        return runTaskAsyncReturnOnMain(
            SetUserCodeCommand.resetUserCodes(),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_tap_header)),
        )
    }

    suspend fun checkUserCodes(cardId: String?): CompletionResult<CheckUserCodesResponse> {
        return runTaskAsyncReturnOnMain(
            CheckUserCodesCommand(),
            cardId,
            initialMessage = Message(context.getString(R.string.initial_message_tap_header)),
        )
    }

    suspend fun scanCard(
        cardId: String? = null,
        allowRequestAccessCodeFromRepository: Boolean = false,
    ): CompletionResult<CardDTO> {
        return runTaskAsyncReturnOnMain(
            runnable = ScanTask(allowRequestAccessCodeFromRepository),
            cardId = cardId,
            initialMessage = Message(context.getString(R.string.initial_message_tap_header)),
        )
            .map { CardDTO(it) }
    }

    suspend fun <T : CommandResponse> runTaskAsync(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
        accessCode: String? = null,
    ): CompletionResult<T> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            tangemSdk.startSessionWithRunnable(runnable, cardId, initialMessage, accessCode) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }
    }

    private suspend fun <T : CommandResponse> runTaskAsyncReturnOnMain(
        runnable: CardSessionRunnable<T>,
        cardId: String? = null,
        initialMessage: Message? = null,
    ): CompletionResult<T> {
        val result = runTaskAsync(runnable, cardId, initialMessage)
        return withContext(Dispatchers.Main) { result }
    }

    @Suppress("MagicNumber")
    fun changeDisplayedCardIdNumbersCount(scanResponse: ScanResponse?) {
        tangemSdk.config.cardIdDisplayFormat = when {
            scanResponse == null -> CardIdDisplayFormat.Full
            scanResponse.cardTypesResolver.isTangemTwins() -> CardIdDisplayFormat.LastLuhn(4)
            scanResponse.cardTypesResolver.isSaltPay() -> CardIdDisplayFormat.None
            else -> CardIdDisplayFormat.Full
        }
    }

    fun getString(@StringRes stringResId: Int, vararg formatArgs: Any?): String {
        return context.getString(stringResId, *formatArgs)
    }

    fun setAccessCodeRequestPolicy(useBiometricsForAccessCode: Boolean) {
        tangemSdk.config.userCodeRequestPolicy = if (useBiometricsForAccessCode) {
            UserCodeRequestPolicy.AlwaysWithBiometrics(codeType = UserCodeType.AccessCode)
        } else {
            UserCodeRequestPolicy.Default
        }
    }

    fun useBiometricsForAccessCode(): Boolean {
        val policy = tangemSdk.config.userCodeRequestPolicy
        return policy is UserCodeRequestPolicy.AlwaysWithBiometrics && policy.codeType == UserCodeType.AccessCode
    }

    companion object {
        val config = Config(
            linkedTerminal = true,
            allowUntrustedCards = true,
            filter = CardFilter(
                allowedCardTypes = FirmwareVersion.FirmwareType.values().toList(),
                maxFirmwareVersion = FirmwareVersion(major = 4, minor = 52),
            ),
        )
    }
}
