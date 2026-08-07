package com.tangem.features.hotwallet.createcloudbackup.model

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.icon
import com.tangem.core.ui.components.bottomsheets.message.infoBlock
import com.tangem.core.ui.components.bottomsheets.message.onClick
import com.tangem.core.ui.components.bottomsheets.message.primaryButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.password.PasswordStrengthEvaluator
import com.tangem.domain.cloudbackup.usecase.CreateCloudBackupUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.CreateCloudBackupComponent
import com.tangem.features.hotwallet.createcloudbackup.entity.CreateCloudBackupUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val createCloudBackupUseCase: CreateCloudBackupUseCase,
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase,
) : Model() {

    private val params = paramsContainer.require<CreateCloudBackupComponent.Params>()

    // Authoritative secret storage, kept until a successful upload so a failed one can be retried without
    // re-entering the password. Zeroing is best-effort only: the text field hands the value over as an
    // immutable String, so a copy the app cannot wipe already exists — this just bounds the lifetime of
    // ours and keeps the secret out of equals/hashCode/toString of the UI state.
    private var password: CharArray = CharArray(0)
    private var confirmPassword: CharArray = CharArray(0)

    private var isPasswordVisible = false
    private var isConsentChecked = false

    val uiState: StateFlow<CreateCloudBackupUM>
        field = MutableStateFlow<CreateCloudBackupUM>(buildSetPasswordUM())

    override fun onDestroy() {
        wipeSecrets()
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        super.onDestroy()
    }

    fun onBack() {
        when (val state = uiState.value) {
            is CreateCloudBackupUM.SetPassword -> showCancelSetupDialog()
            is CreateCloudBackupUM.ConfirmPassword -> if (!state.isLoading) uiState.value = buildSetPasswordUM()
            is CreateCloudBackupUM.Completed -> Unit
        }
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
                        title = resourceReference(R.string.hw_cloud_backup_cancel_setup_cancel),
                        isWarning = true,
                        onClick = { router.pop() },
                    )
                },
            ),
        )
    }

    private fun buildSetPasswordUM(): CreateCloudBackupUM.SetPassword = CreateCloudBackupUM.SetPassword(
        onBackClick = ::onBack,
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
        uiState.value = buildConfirmPasswordUM()
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
            mnemonic = privateInfo.mnemonic.mnemonicComponents.joinToString(separator = " "),
            isPassphraseRequired = privateInfo.passphrase?.isNotEmpty() == true,
        )
        privateInfo.passphrase?.fill(' ')

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
                uiState.value = CreateCloudBackupUM.Completed(onFinishClick = ::onFinish)
            },
        )
    }

    private fun onFinish() {
        val hotWallet = getUserWalletUseCase(params.userWalletId).getOrElse { null } as? UserWallet.Hot
        if (hotWallet != null && hotWallet.hotWalletId.authType == HotWalletId.AuthType.NoPassword) {
            router.replaceCurrent(
                AppRoute.UpdateAccessCode(
                    userWalletId = params.userWalletId,
                    source = AnalyticsParam.ScreensSources.WalletSettings.value,
                ),
            )
        } else {
            router.pop()
        }
    }

    private fun onUploadError(error: CloudBackupError) {
        if (error == CloudBackupError.AuthCanceled) {
            uiState.value = buildConfirmPasswordUM()
            return
        }

        val isPermissions = error == CloudBackupError.AuthPermissionsMissing || error == CloudBackupError.AuthRequired
        val titleRes = if (isPermissions) {
            R.string.hw_cloud_backup_permissions_title
        } else {
            R.string.hw_cloud_backup_error_title
        }
        val bodyRes = when (error) {
            CloudBackupError.NetworkError -> R.string.hw_cloud_backup_error_network
            CloudBackupError.AuthPermissionsMissing,
            CloudBackupError.AuthRequired,
            -> R.string.hw_cloud_backup_permissions_description
            CloudBackupError.CloudUnavailable -> R.string.hw_cloud_backup_error_unavailable
            else -> R.string.hw_cloud_backup_error_write
        }
        val isRetryable = when (error) {
            CloudBackupError.NetworkError,
            is CloudBackupError.WriteError,
            is CloudBackupError.ReadError,
            is CloudBackupError.Unknown,
            CloudBackupError.BackupNotFound,
            -> true
            else -> false
        }
        showErrorSheet(titleRes = titleRes, bodyRes = bodyRes, isRetryable = isRetryable)
    }

    private fun showGenericError() = showErrorSheet(
        titleRes = R.string.hw_cloud_backup_error_title,
        bodyRes = R.string.hw_cloud_backup_error_write,
        isRetryable = true,
    )

    private fun showErrorSheet(titleRes: Int, bodyRes: Int, isRetryable: Boolean) {
        uiState.value = buildConfirmPasswordUM()
        uiMessageSender.send(
            bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.ic_alert_triangle_20) {
                        type = MessageBottomSheetUM.Icon.Type.Warning
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
                    }
                    title = resourceReference(titleRes)
                    body = resourceReference(bodyRes)
                }
                primaryButton {
                    val buttonRes = if (isRetryable) R.string.hw_cloud_backup_retry else R.string.common_got_it
                    text = resourceReference(buttonRes)
                    onClick {
                        if (isRetryable) startBackup()
                        closeBs()
                    }
                }
            },
        )
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