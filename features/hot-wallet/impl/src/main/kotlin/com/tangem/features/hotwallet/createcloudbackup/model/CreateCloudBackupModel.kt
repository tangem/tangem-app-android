package com.tangem.features.hotwallet.createcloudbackup.model

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.cloudbackup.analytics.analyticsMessage
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.password.PasswordStrengthEvaluator
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.usecase.CreateCloudBackupUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.CreateCloudBackupComponent
import com.tangem.features.hotwallet.createcloudbackup.entity.CreateCloudBackupUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class CreateCloudBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val trackingContextProxy: TrackingContextProxy,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val createCloudBackupUseCase: CreateCloudBackupUseCase,
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase,
    private val cloudBackupRepository: CloudBackupRepository,
) : Model() {

    private val params = paramsContainer.require<CreateCloudBackupComponent.Params>()

    private val serviceName = resourceReference(R.string.hw_cloud_backup_service_name)

    // Authoritative secret storage, kept until a successful upload so a failed one can be retried without
    // re-entering the password. Zeroing is best-effort only: the text field hands the value over as an
    // immutable String, so a copy the app cannot wipe already exists — this just bounds the lifetime of
    // ours and keeps the secret out of equals/hashCode/toString of the UI state.
    private var password: CharArray = CharArray(0)
    private var confirmPassword: CharArray = CharArray(0)

    private var isPasswordVisible = false
    private var isConsentChecked = false

    val uiState: StateFlow<CreateCloudBackupUM>
        field = MutableStateFlow<CreateCloudBackupUM>(buildPreparingUM())

    private val authJobHolder = JobHolder()

    init {
        trackingContextProxy.addHotWalletContext()
        authorize()
    }

    override fun onDestroy() {
        wipeSecrets()
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        trackingContextProxy.removeContext()
        super.onDestroy()
    }

    fun onBack() {
        when (val state = uiState.value) {
            is CreateCloudBackupUM.Preparing -> router.pop()
            is CreateCloudBackupUM.SetPassword -> closeOrConfirmCancel()
            is CreateCloudBackupUM.ConfirmPassword -> if (!state.isLoading) uiState.value = buildSetPasswordUM()
            is CreateCloudBackupUM.Completed -> Unit
        }
    }

    private fun onClose() {
        when (val state = uiState.value) {
            is CreateCloudBackupUM.Preparing -> router.pop()
            is CreateCloudBackupUM.SetPassword -> closeOrConfirmCancel()
            is CreateCloudBackupUM.ConfirmPassword -> if (!state.isLoading) closeOrConfirmCancel()
            is CreateCloudBackupUM.Completed -> Unit
        }
    }

    /** Nothing typed yet means there is nothing to lose, so the confirmation would only be noise. */
    private fun closeOrConfirmCancel() {
        if (password.isEmpty() && confirmPassword.isEmpty()) {
            onCancelSetupConfirmed()
        } else {
            showCancelSetupDialog()
        }
    }

    private fun authorize() {
        if (authJobHolder.isActive) return

        uiState.value = buildPreparingUM()
        modelScope.launch {
            cloudBackupRepository.getAccountInfo(interactive = true).fold(
                ifLeft = ::onAuthError,
                ifRight = {
                    uiState.value = buildSetPasswordUM()
                    analyticsEventHandler.send(WalletSettingsAnalyticEvents.SetCloudPasswordScreen())
                },
            )
        }.saveIn(authJobHolder)
    }

    private fun onAuthError(error: CloudBackupError) {
        if (error == CloudBackupError.AuthCanceled) {
            router.pop()
            return
        }
        val spec = cloudBackupErrorSpec(error, serviceName)
        uiMessageSender.send(
            cloudBackupErrorSheet(
                spec = spec,
                onClosed = { router.pop() },
                onAction = { if (spec.isRetryable) authorize() else router.pop() },
            ),
        )
    }

    private fun showCancelSetupDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.hw_cloud_backup_cancel_setup_title),
                message = resourceReference(
                    id = R.string.hw_cloud_backup_cancel_setup_description,
                    formatArgs = wrappedList(resourceReference(R.string.hw_cloud_backup_service_name)),
                ),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.hw_cloud_backup_cancel_setup_continue),
                        onClick = {},
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_cancel),
                        isWarning = true,
                        onClick = ::onCancelSetupConfirmed,
                    )
                },
            ),
        )
    }

    private fun buildPreparingUM(): CreateCloudBackupUM.Preparing = CreateCloudBackupUM.Preparing(
        onBackClick = ::onBack,
        onCloseClick = ::onClose,
    )

    private fun buildSetPasswordUM(): CreateCloudBackupUM.SetPassword = CreateCloudBackupUM.SetPassword(
        onBackClick = ::onBack,
        onCloseClick = ::onClose,
        password = String(password),
        isPasswordVisible = isPasswordVisible,
        strength = PasswordStrengthEvaluator.evaluateRated(password),
        hint = PasswordStrengthEvaluator.hint(password),
        onPasswordChange = ::onPasswordChange,
        onToggleVisibility = ::onTogglePasswordVisibility,
        onContinueClick = ::onSetPasswordContinue,
    )

    private fun buildConfirmPasswordUM(isLoading: Boolean = false): CreateCloudBackupUM.ConfirmPassword {
        val isMismatch = confirmPassword.isNotEmpty() && !password.contentEquals(confirmPassword)
        return CreateCloudBackupUM.ConfirmPassword(
            onBackClick = ::onBack,
            onCloseClick = ::onClose,
            confirmPassword = String(confirmPassword),
            isPasswordVisible = isPasswordVisible,
            isMismatch = isMismatch,
            isConsentChecked = isConsentChecked,
            isLoading = isLoading,
            onPasswordChange = ::onConfirmPasswordChange,
            onToggleVisibility = ::onToggleConfirmVisibility,
            onConsentChange = ::onConsentChange,
            onConfirmClick = ::onConfirmClick,
        )
    }

    private fun onPasswordChange(value: String) {
        password.fill(' ')
        password = value.toCharArray()
        uiState.value = buildSetPasswordUM()
    }

    private fun onTogglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        uiState.value = buildSetPasswordUM()
    }

    private fun onSetPasswordContinue() {
        if (!PasswordStrengthEvaluator.evaluate(password).isAcceptable) return
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ConfirmCloudPasswordScreen())
        uiState.value = buildConfirmPasswordUM()
    }

    private fun onCancelSetupConfirmed() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.CloudBackupScreenClosed())
        router.pop()
    }

    private fun onConfirmPasswordChange(value: String) {
        confirmPassword.fill(' ')
        confirmPassword = value.toCharArray()
        uiState.value = buildConfirmPasswordUM()
    }

    private fun onToggleConfirmVisibility() {
        isPasswordVisible = !isPasswordVisible
        uiState.value = buildConfirmPasswordUM()
    }

    private fun onConsentChange(checked: Boolean) {
        isConsentChecked = checked
        uiState.value = buildConfirmPasswordUM()
    }

    private fun onConfirmClick() {
        val isMatch = confirmPassword.isNotEmpty() && password.contentEquals(confirmPassword)
        if (!isMatch || !isConsentChecked) return
        startBackup()
    }

    private fun startBackup() {
        uiState.value = buildConfirmPasswordUM(isLoading = true)
        modelScope.launch { runBackup() }
    }

    private suspend fun runBackup() {
        val hotWallet = getUserWalletUseCase(params.userWalletId).getOrElse { null } as? UserWallet.Hot
        if (hotWallet == null) {
            showGenericError()
            return
        }

        val privateInfo = ensureWalletUnlocked(hotWallet.hotWalletId)
            .flatMap { exportSeedPhraseUseCase.invoke(hotWallet.hotWalletId) }
            .getOrElse { error ->
                if (error.isUserCancelled()) {
                    uiState.value = buildConfirmPasswordUM()
                } else {
                    TangemLogger.e("Error on exporting the seed phrase for a cloud backup", error)
                    showGenericError()
                }
                return
            }

        val secret = CloudBackupSecretData(
            mnemonic = privateInfo.mnemonic.mnemonicComponents.joinToCharArray(separator = ' '),
            isPassphraseRequired = privateInfo.passphrase?.isNotEmpty() == true,
        )
        privateInfo.passphrase?.fill(' ')

        try {
            createCloudBackupUseCase(
                walletId = params.userWalletId.stringValue,
                walletName = hotWallet.name,
                secret = secret,
                password = password,
            ).fold(
                ifLeft = ::onUploadError,
                ifRight = {
                    setCloudBackupStateUseCase(params.userWalletId.stringValue, isBackedUp = true)
                    wipeSecrets()
                    sendBackupCompletedEvents()
                    uiState.value = CreateCloudBackupUM.Completed(onFinishClick = ::onFinish)
                },
            )
        } finally {
            secret.wipe()
        }
    }

    private fun onFinish() {
        val hotWallet = getUserWalletUseCase(params.userWalletId).getOrElse { null } as? UserWallet.Hot
        if (hotWallet != null && hotWallet.hotWalletId.authType == HotWalletId.AuthType.NoPassword) {
            router.replaceCurrent(
                AppRoute.UpdateAccessCode(
                    userWalletId = params.userWalletId,
                    source = AnalyticsParam.ScreensSources.WalletSettings.value,
                    // the wallet is already backed up to the cloud, so the access code must be skippable
                    canSkip = true,
                ),
            )
        } else {
            router.pop()
        }
    }

    private fun sendBackupCompletedEvents() {
        analyticsEventHandler.send(
            event = WalletSettingsAnalyticEvents.BackupCompleteScreen(
                source = AnalyticsParam.ScreensSources.WalletSettings.value,
                action = WalletSettingsAnalyticEvents.RecoveryPhraseScreenAction.Backup.value,
                backupType = AnalyticsParam.BackupType.Cloud,
            ),
        )
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.Backup.Finished(backupType = AnalyticsParam.BackupType.Cloud),
        )
    }

    private fun onUploadError(error: CloudBackupError) {
        uiState.value = buildConfirmPasswordUM()
        if (error == CloudBackupError.AuthCanceled) return

        analyticsEventHandler.send(
            WalletSettingsAnalyticEvents.CloudBackupCreationError(errorMessage = error.analyticsMessage()),
        )
        val spec = cloudBackupErrorSpec(error, serviceName)
        uiMessageSender.send(cloudBackupErrorSheet(spec) { if (spec.isRetryable) startBackup() })
    }

    private fun showGenericError() {
        analyticsEventHandler.send(
            WalletSettingsAnalyticEvents.CloudBackupCreationError(
                errorMessage = CloudBackupError.Unknown().analyticsMessage(),
            ),
        )
        uiState.value = buildConfirmPasswordUM()
        val spec = CloudBackupErrorSpec(
            title = resourceReference(R.string.hw_cloud_backup_error_title),
            body = resourceReference(R.string.hw_cloud_backup_error_write),
            isRetryable = true,
        )
        uiMessageSender.send(cloudBackupErrorSheet(spec) { startBackup() })
    }

    private fun wipeSecrets() {
        password.fill(' ')
        password = CharArray(0)
        confirmPassword.fill(' ')
        confirmPassword = CharArray(0)
    }

    /**
     * Obtains a contextual unlock unless one is already held: [UnlockHotWalletContextualUseCase] always
     * prompts the user, while the export reuses a held unlock. So asking only when there is none keeps a
     * retry after a failed upload from prompting for the wallet password again. Cleared in [onDestroy].
     */
    private suspend fun ensureWalletUnlocked(hotWalletId: HotWalletId): Either<Throwable, Unit> =
        getHotWalletContextualUnlockUseCase(hotWalletId).flatMap { heldUnlock ->
            if (heldUnlock == null) {
                unlockHotWalletContextualUseCase(hotWalletId).map { }
            } else {
                Unit.right()
            }
        }

    private fun Throwable.isUserCancelled(): Boolean =
        this is TangemSdkError.UserCancelled || cause is TangemSdkError.UserCancelled
}

/**
 * Joins the words without building a String for the whole phrase, so the only copy the app owns is the
 * returned array and it can be wiped. The words themselves already come from the SDK as Strings.
 */
private fun List<String>.joinToCharArray(separator: Char): CharArray {
    val separators = (size - 1).coerceAtLeast(minimumValue = 0)
    val joined = CharArray(sumOf { it.length } + separators)
    var offset = 0
    forEachIndexed { index, word ->
        if (index > 0) joined[offset++] = separator
        word.toCharArray(destination = joined, destinationOffset = offset)
        offset += word.length
    }
    return joined
}