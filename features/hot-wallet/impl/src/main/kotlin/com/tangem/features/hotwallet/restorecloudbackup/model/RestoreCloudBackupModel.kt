package com.tangem.features.hotwallet.restorecloudbackup.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.cloudbackup.analytics.analyticsMessage
import com.tangem.domain.cloudbackup.analytics.analyticsWalletId
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.models.CloudBackupSecretData
import com.tangem.domain.cloudbackup.usecase.RestoreCloudBackupUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.features.hotwallet.MnemonicRepository
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImportError
import com.tangem.features.hotwallet.addexistingwallet.im.port.model.HotWalletImporter
import com.tangem.features.hotwallet.restorecloudbackup.CloudRestoreResultHolder
import com.tangem.features.hotwallet.restorecloudbackup.RestoreCloudBackupComponent
import com.tangem.features.hotwallet.restorecloudbackup.entity.BackupRowUM
import com.tangem.features.hotwallet.restorecloudbackup.entity.RestoreCloudBackupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single coordinator that owns the whole restore-cloud-backup flow and its secret material.
 *
 * The backups and cloud account email are resolved by the import bottom sheet and handed over through
 * [CloudRestoreResultHolder]; this model consumes them once and picks the initial step (one backup goes
 * straight to password entry, several show the list). Restore via [RestoreCloudBackupUseCase] (download +
 * decrypt happen in the data layer) yields the wallet secret, which is fed to [MnemonicRepository] +
 * [HotWalletImporter] (the same import mechanism as the seed-phrase flow). The password lives here as a
 * [CharArray] (never inside a serializable route or a logged UM) and is wiped on success and in
 * [onDestroy]. Nothing secret is ever logged, sent to analytics, or put into an exception message.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class RestoreCloudBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    cloudRestoreResultHolder: CloudRestoreResultHolder,
    private val restoreCloudBackupUseCase: RestoreCloudBackupUseCase,
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase,
    private val mnemonicRepository: MnemonicRepository,
    private val hotWalletImporter: HotWalletImporter,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<RestoreCloudBackupComponent.Params>()

    private var password: CharArray = CharArray(0)
    private var isPasswordVisible = false
    private var isPasswordError = false

    private var passphrase: CharArray = CharArray(0)
    private var isPassphraseVisible = false

    private var restoredSecret: CloudBackupSecretData? = null
    private var restoredWalletName: String = ""

    private var accountEmail: String? = null
    private var backups: List<CloudBackupInfo> = emptyList()
    private var selectedBackup: CloudBackupInfo? = null

    val uiState: StateFlow<RestoreCloudBackupUM>
        field = MutableStateFlow(resolveInitialState(cloudRestoreResultHolder))

    override fun onDestroy() {
        wipeSecrets()
        super.onDestroy()
    }

    fun onBack() {
        uiState.value.onBack()
    }

    private fun resolveInitialState(holder: CloudRestoreResultHolder): RestoreCloudBackupUM {
        val result = holder.result.value
        holder.clear()

        val loaded = result?.backups.orEmpty().sortedByDescending { it.createdAtMillis }
        if (loaded.isEmpty()) {
            // The import bottom sheet only navigates here with >= 1 backup; guard the impossible case.
            modelScope.launch { onRootBack() }
            return RestoreCloudBackupUM.BackupList(
                items = persistentListOf(),
                accountEmail = null,
                onBack = ::onRootBack,
            )
        }

        accountEmail = result?.accountEmail
        backups = loaded
        return if (loaded.size == 1) {
            selectedBackup = loaded.first()
            sendEnterPasswordScreenEvent()
            buildEnterPasswordUM()
        } else {
            analyticsEventHandler.send(
                event = OnboardingAnalyticsEvent.Backup.SelectCloudBackupScreen(backupCount = loaded.size),
            )
            buildBackupListUM(loaded)
        }
    }

    private fun sendEnterPasswordScreenEvent() {
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.Backup.EnterCloudBackupPasswordScreen(
                userWalletId = analyticsWalletId(selectedBackup?.walletId),
            ),
        )
    }

    private fun buildBackupListUM(list: List<CloudBackupInfo>): RestoreCloudBackupUM.BackupList =
        RestoreCloudBackupUM.BackupList(
            items = list.map { info ->
                BackupRowUM(
                    walletName = info.walletName,
                    createdAtMillis = info.createdAtMillis,
                    onClick = { showEnterPassword(info) },
                )
            }.toImmutableList(),
            accountEmail = accountEmail,
            onBack = ::onRootBack,
        )

    private fun showEnterPassword(backup: CloudBackupInfo) {
        selectedBackup = backup
        wipePassword()
        sendEnterPasswordScreenEvent()
        uiState.value = buildEnterPasswordUM()
    }

    private fun buildEnterPasswordUM(isLoading: Boolean = false): RestoreCloudBackupUM.EnterPassword =
        RestoreCloudBackupUM.EnterPassword(
            walletName = selectedBackup?.walletName.orEmpty(),
            createdAtMillis = selectedBackup?.createdAtMillis ?: 0L,
            password = String(password),
            isPasswordVisible = isPasswordVisible,
            isError = isPasswordError,
            isLoading = isLoading,
            onPasswordChange = ::onPasswordChange,
            onToggleVisibility = ::onTogglePasswordVisibility,
            onRestoreClick = ::onRestoreClick,
            accountEmail = accountEmail,
            onBack = ::onEnterPasswordBack,
        )

    private fun onPasswordChange(value: String) {
        password.fill(' ')
        password = value.toCharArray()
        isPasswordError = false
        uiState.value = buildEnterPasswordUM()
    }

    private fun onTogglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        uiState.value = buildEnterPasswordUM()
    }

    private fun onEnterPasswordBack() {
        if ((uiState.value as? RestoreCloudBackupUM.EnterPassword)?.isLoading == true) return
        wipePassword()
        if (backups.size > 1) {
            uiState.value = buildBackupListUM(backups)
        } else {
            onRootBack()
        }
    }

    private fun onRestoreClick() {
        val backup = selectedBackup ?: return
        if (password.isEmpty()) return
        uiState.value = buildEnterPasswordUM(isLoading = true)
        modelScope.launch { runRestore(backup) }
    }

    private suspend fun runRestore(backup: CloudBackupInfo) {
        restoreCloudBackupUseCase(fileId = backup.fileId, password = password).fold(
            ifLeft = { error ->
                if (error == CloudBackupError.WrongPassword) {
                    analyticsEventHandler.send(OnboardingAnalyticsEvent.Backup.WrongCloudBackupPassword())
                    showWrongPassword()
                } else {
                    showError(error)
                }
            },
            ifRight = { secret -> onSecretRestored(secret, name = backup.walletName) },
        )
    }

    private suspend fun onSecretRestored(secret: CloudBackupSecretData, name: String) {
        wipeRestoredSecret()
        restoredSecret = secret
        restoredWalletName = name
        if (secret.isPassphraseRequired) {
            uiState.value = buildEnterPassphraseUM()
        } else {
            importSecret(secret, name = name, passphrase = null)
        }
    }

    private fun buildEnterPassphraseUM(isLoading: Boolean = false): RestoreCloudBackupUM.EnterPassphrase =
        RestoreCloudBackupUM.EnterPassphrase(
            passphrase = String(passphrase),
            isPassphraseVisible = isPassphraseVisible,
            isLoading = isLoading,
            onPassphraseChange = ::onPassphraseChange,
            onToggleVisibility = ::onTogglePassphraseVisibility,
            onContinueClick = ::onPassphraseContinueClick,
            accountEmail = accountEmail,
            onBack = ::onEnterPassphraseBack,
        )

    private fun onPassphraseChange(value: String) {
        passphrase.fill(' ')
        passphrase = value.toCharArray()
        uiState.value = buildEnterPassphraseUM()
    }

    private fun onTogglePassphraseVisibility() {
        isPassphraseVisible = !isPassphraseVisible
        uiState.value = buildEnterPassphraseUM()
    }

    private fun onEnterPassphraseBack() {
        if ((uiState.value as? RestoreCloudBackupUM.EnterPassphrase)?.isLoading == true) return
        wipePassphrase()
        wipeRestoredSecret()
        uiState.value = buildEnterPasswordUM()
    }

    private fun onPassphraseContinueClick() {
        val secret = restoredSecret ?: return
        if (passphrase.isEmpty()) return
        uiState.value = buildEnterPassphraseUM(isLoading = true)
        modelScope.launch {
            // a copy: the importer keeps the array while wipeSecrets() clears ours on success
            importSecret(secret, name = restoredWalletName, passphrase = passphrase.copyOf())
        }
    }

    private suspend fun importSecret(secret: CloudBackupSecretData, name: String, passphrase: CharArray?) {
        val mnemonic = runCatching {
            // the SDK only parses a String, so this copy of the phrase cannot be wiped
            mnemonicRepository.generateMnemonic(String(secret.mnemonic))
        }.getOrNull()

        if (mnemonic == null) {
            showError(CloudBackupError.InvalidBackupFile)
            return
        }

        hotWalletImporter.import(
            scope = modelScope,
            mnemonic = mnemonic,
            passphrase = passphrase?.takeIf { it.isNotEmpty() },
            name = name,
        ).fold(
            ifLeft = { error ->
                when (error) {
                    HotWalletImportError.AlreadySaved -> showAlreadyAdded()
                    is HotWalletImportError.Unknown -> showError(CloudBackupError.Unknown())
                }
            },
            ifRight = { userWalletId ->
                wipeSecrets()
                setCloudBackupStateUseCase(userWalletId.stringValue, isBackedUp = true)
                params.callbacks.onWalletImported(userWalletId)
            },
        )
    }

    private fun showAlreadyAdded() {
        uiState.value = buildCurrentStepUM()
        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(R.string.user_wallet_list_error_wallet_already_saved),
                firstAction = EventMessageAction(title = resourceReference(R.string.common_ok), onClick = {}),
            ),
        )
    }

    private fun showWrongPassword() {
        isPasswordError = true
        uiState.value = buildEnterPasswordUM()
    }

    private fun showError(error: CloudBackupError) {
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.Backup.ImportCloudBackupError(
                userWalletId = analyticsWalletId(selectedBackup?.walletId),
                errorMessage = error.analyticsMessage(),
            ),
        )
        uiState.value = buildCurrentStepUM()
        uiMessageSender.send(errorDialog(error))
    }

    /** Rebuilds the step the flow is currently on, clearing its loading state */
    private fun buildCurrentStepUM(): RestoreCloudBackupUM = when (uiState.value) {
        is RestoreCloudBackupUM.EnterPassphrase -> buildEnterPassphraseUM()
        else -> buildEnterPasswordUM()
    }

    private fun errorDialog(error: CloudBackupError): DialogMessage {
        val messageRes = if (error == CloudBackupError.NetworkError) {
            R.string.hw_cloud_backup_error_network
        } else {
            R.string.hw_cloud_backup_restore_error_with_recovery
        }
        return DialogMessage(
            title = resourceReference(R.string.hw_cloud_backup_restore_error_title),
            message = resourceReference(messageRes),
            firstAction = EventMessageAction(title = resourceReference(R.string.common_ok), onClick = {}),
        )
    }

    private fun onRootBack() {
        wipeSecrets()
        params.callbacks.onBack()
    }

    private fun wipePassword() {
        password.fill(' ')
        password = CharArray(0)
        isPasswordVisible = false
        isPasswordError = false
    }

    private fun wipePassphrase() {
        passphrase.fill(' ')
        passphrase = CharArray(0)
        isPassphraseVisible = false
    }

    private fun wipeRestoredSecret() {
        restoredSecret?.wipe()
        restoredSecret = null
    }

    private fun wipeSecrets() {
        wipePassword()
        wipePassphrase()
        wipeRestoredSecret()
    }
}