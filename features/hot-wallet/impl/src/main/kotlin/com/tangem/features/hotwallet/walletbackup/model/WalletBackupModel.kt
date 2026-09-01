package com.tangem.features.hotwallet.walletbackup.model

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cloud_24_filled
import com.tangem.core.ui.utils.DateTimeFormatters
import org.joda.time.DateTime
import com.tangem.domain.cloudbackup.analytics.analyticsMessage
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.features.hotwallet.walletbackup.entity.WalletBackupUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.Lazy
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class WalletBackupModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    private val router: Router,
    private val trackingContextProxy: TrackingContextProxy,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val uiMessageSender: UiMessageSender,
    private val cloudBackupRepository: Lazy<CloudBackupRepository>,
    private val setCloudBackupStateUseCase: Lazy<SetCloudBackupStateUseCase>,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : Model() {

    private val params: WalletBackupComponent.Params = paramsContainer.require()

    private val serviceName = resourceReference(R.string.hw_cloud_backup_service_name)

    private val isScreenOpenedEventSent: AtomicBoolean = AtomicBoolean(false)
    private val isFirstResume: AtomicBoolean = AtomicBoolean(true)

    private var isManuallyBackedUp: Boolean? = null
    private var isCloudStatusResolved: Boolean = !hotWalletFeatureToggles.isGoogleDriveBackupEnabled

    private var cloudBackupInfo: CloudBackupInfo? = null
    private var refreshStatusJob: Job? = null
    private var cloudSignInJob: Job? = null

    val uiState: StateFlow<WalletBackupUM>
        field = MutableStateFlow(
            WalletBackupUM(
                onBackClick = { router.pop() },
                hardwareWalletOption = LabelUM(
                    text = resourceReference(R.string.common_recommended),
                    style = LabelStyle.ACCENT,
                ).takeIf { params.isColdWalletOptionShown },
                recoveryPhraseOption = noBackupLabel(),
                googleDriveOption = LabelUM(
                    text = resourceReference(R.string.common_coming_soon),
                    style = LabelStyle.REGULAR,
                ).takeUnless { hotWalletFeatureToggles.isGoogleDriveBackupEnabled },
                googleDriveStatus = BackupStatus.ComingSoon,
                onRecoveryPhraseClick = ::onRecoveryPhraseClick,
                onGoogleDriveClick = ::onGoogleDriveBackupClick,
                onHardwareWalletClick = ::onHardwareWalletClick,
                isBackedUp = false,
            ),
        )

    init {
        trackingContextProxy.addHotWalletContext()
        getUserWalletUseCase.invokeFlow(params.userWalletId)
            .onEach { either ->
                either.fold(
                    ifLeft = {
                        TangemLogger.e("Error on getting user wallet: $it")
                    },
                    ifRight = { userWallet ->
                        updateBackupStatuses(userWallet)
                        if (userWallet is UserWallet.Hot) {
                            isManuallyBackedUp = userWallet.backedUp
                            sendScreenOpenedEventIfReady()
                        }
                    },
                )
            }.launchIn(modelScope)

        if (hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
            refreshCloudBackupStatus()
        }
    }

    override fun onDestroy() {
        trackingContextProxy.removeContext()
        clearHotWalletContextualUnlockUseCase.invoke(params.userWalletId)
        super.onDestroy()
    }

    private fun noBackupLabel() = LabelUM(
        text = resourceReference(R.string.hw_backup_no_backup),
        style = LabelStyle.WARNING,
    )

    private fun doneLabel() = LabelUM(
        text = resourceReference(R.string.common_done),
        style = LabelStyle.ACCENT,
    )

    private fun actionRequiredLabel() = LabelUM(
        text = resourceReference(R.string.hw_cloud_backup_status_action_required),
        style = LabelStyle.ATTENTION,
    )

    private fun networkErrorLabel() = LabelUM(
        text = resourceReference(R.string.hw_cloud_backup_status_network_error),
        style = LabelStyle.ATTENTION,
    )

    private fun comingSoonLabel() = LabelUM(
        text = resourceReference(R.string.common_coming_soon),
        style = LabelStyle.REGULAR,
    )

    private fun updateBackupStatuses(userWallet: UserWallet) {
        uiState.update { currentState ->
            if (userWallet is UserWallet.Hot) {
                currentState.updateBackupStatusesHotWallet(userWallet)
            } else {
                currentState
            }
        }
    }

    private fun WalletBackupUM.updateBackupStatusesHotWallet(userWallet: UserWallet.Hot): WalletBackupUM {
        val updated = copy(
            recoveryPhraseOption = if (userWallet.backedUp) doneLabel() else noBackupLabel(),
            isBackedUp = userWallet.backedUp,
        )

        return if (hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
            updated
        } else {
            updated.copy(
                googleDriveOption = comingSoonLabel(),
                googleDriveStatus = BackupStatus.ComingSoon,
            )
        }
    }

    private fun refreshCloudBackupStatus() {
        if (cloudSignInJob?.isActive == true) return

        refreshStatusJob?.cancel()
        setGoogleDriveStatus(BackupStatus.Loading)
        refreshStatusJob = modelScope.launch {
            loadCloudBackup(interactive = false)
            isCloudStatusResolved = true
            sendScreenOpenedEventIfReady()
        }
    }

    private fun startCloudSignIn(onVerified: (CloudBackupInfo?) -> Unit = {}) {
        if (cloudSignInJob?.isActive == true) return

        refreshStatusJob?.cancel()
        setGoogleDriveStatus(BackupStatus.Loading)
        cloudSignInJob = modelScope.launch {
            cloudBackupRepository.get().signOut()

            val info = loadCloudBackup(interactive = true).getOrElse { error ->
                onCloudAccessFailed(error)
                return@launch
            }
            onVerified(info)
        }
    }

    private suspend fun loadCloudBackup(interactive: Boolean): Either<CloudBackupError, CloudBackupInfo?> {
        val repository = cloudBackupRepository.get()
        val walletId = params.userWalletId.stringValue
        val wasBackedUp = repository.isBackedUp(walletId)

        return repository.findBackups(interactive = interactive)
            .onLeft { error ->
                if (error !is CloudBackupError.AuthRequired) {
                    TangemLogger.e("Error on finding cloud backups: $error")
                }
                cloudBackupInfo = null
                setGoogleDriveStatus(resolveFailedStatus(error, wasBackedUp))
            }
            .map { backups ->
                val info = backups.firstOrNull { it.walletId == walletId }
                cloudBackupInfo = info
                if (info != null && !wasBackedUp) {
                    setCloudBackupStateUseCase.get()(walletId, isBackedUp = true)
                }
                setGoogleDriveStatus(resolveFoundStatus(info, wasBackedUp))
                info
            }
    }

    /**
     * Reports the screen opening once, with both backup statuses filled in. The wallet and the cloud
     * lookup resolve independently, so whichever finishes last triggers the send.
     */
    private fun sendScreenOpenedEventIfReady() {
        val isBackedUp = isManuallyBackedUp ?: return
        if (!isCloudStatusResolved) return
        if (isScreenOpenedEventSent.getAndSet(true)) return

        analyticsEventHandler.send(
            event = WalletSettingsAnalyticEvents.BackupScreenOpened(
                isBackedUp = isBackedUp,
                cloudBackupState = uiState.value.googleDriveStatus.toAnalyticsState(),
            ),
        )
    }

    private fun resolveFoundStatus(info: CloudBackupInfo?, wasBackedUp: Boolean): BackupStatus = when {
        info != null -> BackupStatus.Done
        wasBackedUp -> BackupStatus.ActionRequired(BackupStatus.ActionRequired.Reason.FileNotFound)
        else -> BackupStatus.NoBackup
    }

    private fun resolveFailedStatus(error: CloudBackupError, wasBackedUp: Boolean): BackupStatus = when {
        !wasBackedUp -> BackupStatus.NoBackup
        error == CloudBackupError.NetworkError -> BackupStatus.NetworkError
        else -> BackupStatus.ActionRequired(BackupStatus.ActionRequired.Reason.NoAccess)
    }

    private fun isAccessError(error: CloudBackupError): Boolean =
        error == CloudBackupError.AuthRequired || error == CloudBackupError.AuthPermissionsMissing

    private fun setGoogleDriveStatus(status: BackupStatus) {
        uiState.update { state ->
            state.copy(
                googleDriveStatus = status,
                googleDriveOption = labelFor(status),
            )
        }
    }

    private fun labelFor(status: BackupStatus): LabelUM? = when (status) {
        BackupStatus.Loading -> null
        BackupStatus.Done -> doneLabel()
        BackupStatus.NoBackup -> noBackupLabel()
        BackupStatus.NetworkError -> networkErrorLabel()
        is BackupStatus.ActionRequired -> actionRequiredLabel()
        BackupStatus.ComingSoon -> comingSoonLabel()
    }

    private fun onRecoveryPhraseClick() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonRecoveryPhrase())
        if (uiState.value.isBackedUp) {
            getUserWalletUseCase.invoke(params.userWalletId)
                .fold(
                    ifLeft = {
                        TangemLogger.e("Error on getting user wallet: $it")
                    },
                    ifRight = { userWallet ->
                        when (userWallet) {
                            is UserWallet.Cold -> {
                                val userWalletId = userWallet.walletId
                                TangemLogger.e("Unexpected cold wallet when request seed phrase: $userWalletId")
                            }
                            is UserWallet.Hot -> showSeedPhrase(userWallet)
                        }
                    },
                )
        } else {
            router.push(
                AppRoute.WalletActivation(
                    userWalletId = params.userWalletId,
                    isBackupExists = false,
                ),
            )
        }
    }

    private fun onGoogleDriveBackupClick() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonGoogleDriveBackup())

        if (!hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
            showGoogleDriveComingSoonDialog()
            return
        }

        when (val status = uiState.value.googleDriveStatus) {
            BackupStatus.Done -> cloudBackupInfo?.let(::showRemoveBackupSheet)
            BackupStatus.NoBackup,
            BackupStatus.ComingSoon,
            -> onNoBackupClick()
            BackupStatus.Loading -> Unit
            BackupStatus.NetworkError -> refreshCloudBackupStatus()
            is BackupStatus.ActionRequired -> when (status.reason) {
                BackupStatus.ActionRequired.Reason.NoAccess -> showCantAccessSheet()
                BackupStatus.ActionRequired.Reason.FileNotFound -> showBackupNotFoundSheet()
            }
        }
    }

    private fun onNoBackupClick() {
        startCloudSignIn { info ->
            if (info != null) showRemoveBackupSheet(info) else openCreateCloudBackup()
        }
    }

    private fun openCreateCloudBackup() {
        router.push(AppRoute.CreateCloudBackup(params.userWalletId))
    }

    private fun onCloudAccessFailed(error: CloudBackupError) = when {
        error == CloudBackupError.AuthCanceled -> Unit
        isAccessError(error) -> showCantAccessSheet()
        else -> showCloudErrorDialog(error)
    }

    private fun showCantAccessSheet() {
        uiMessageSender.send(
            bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.ic_alert_triangle_20) {
                        type = MessageBottomSheetUM.Icon.Type.Warning
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
                    }
                    title = resourceReference(
                        id = R.string.hw_cloud_backup_access_unavailable_title,
                        formatArgs = wrappedList(serviceName),
                    )
                    body = resourceReference(R.string.hw_cloud_backup_no_access_description)
                }
                secondaryButton {
                    text = resourceReference(R.string.common_not_now)
                    onClick { closeBs() }
                }
                primaryButton {
                    text = resourceReference(R.string.hw_cloud_backup_retry)
                    onClick {
                        startCloudSignIn()
                        closeBs()
                    }
                }
            },
        )
    }

    private fun showBackupNotFoundSheet() {
        uiMessageSender.send(
            bottomSheetMessage {
                infoBlock {
                    icon(R.drawable.ic_alert_triangle_20) {
                        type = MessageBottomSheetUM.Icon.Type.Warning
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
                    }
                    title = resourceReference(R.string.hw_cloud_backup_not_found_title)
                    body = resourceReference(R.string.hw_cloud_backup_not_found_description)
                }
                secondaryButton {
                    text = resourceReference(R.string.hw_cloud_backup_not_found_forget)
                    onClick {
                        modelScope.launch {
                            setCloudBackupStateUseCase.get()(params.userWalletId.stringValue, isBackedUp = false)
                            refreshCloudBackupStatus()
                        }
                        closeBs()
                    }
                }
                primaryButton {
                    text = resourceReference(R.string.hw_cloud_backup_not_found_create)
                    onClick {
                        router.push(AppRoute.CreateCloudBackup(params.userWalletId))
                        closeBs()
                    }
                }
            },
        )
    }

    /**
     * Refreshes the Google Drive backup status when the screen is resumed (e.g. after returning from

     * resume is skipped because [init] already fetched the status on creation.
     */
    fun onScreenResumed() {
        if (isFirstResume.getAndSet(false)) return
        if (hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
            refreshCloudBackupStatus()
        }
    }

    private fun showGoogleDriveComingSoonDialog() {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.hw_backup_google_drive_dialog_title),
                message = resourceReference(id = R.string.hw_backup_google_drive_dialog_message),
                firstAction = EventMessageAction(
                    title = resourceReference(id = R.string.common_ok),
                    onClick = {},
                ),
            ),
        )
    }

    private fun showRemoveBackupSheet(info: CloudBackupInfo) {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.CloudBackupDetailsScreen())
        val backupTime = DateTimeFormatters.formatDate(
            date = DateTime(info.createdAtMillis),
            formatter = DateTimeFormatters.dateTimeMMMdYYYY,
        )
        uiMessageSender.send(
            bottomSheetMessage {
                infoBlock {
                    vector(Icons.ic_cloud_24_filled) {
                        type = MessageBottomSheetUM.Vector.Type.Accent
                        backgroundType = MessageBottomSheetUM.Vector.BackgroundType.Accent
                    }
                    title = resourceReference(
                        id = R.string.hw_cloud_backup_remove_title,
                        formatArgs = wrappedList(serviceName, info.walletName),
                    )
                    body = resourceReference(R.string.hw_cloud_backup_sheet_backup_time, wrappedList(backupTime))
                }
                secondaryButton {
                    text = resourceReference(R.string.hw_cloud_backup_sheet_remove)
                    onClick {
                        closeBs()
                        showDeleteConfirmationDialog(info.fileId)
                    }
                }
            },
        )
    }

    private fun showDeleteConfirmationDialog(fileId: String) {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.CloudBackupDeletionRequest())
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(
                    id = R.string.hw_backup_cloud_delete_confirm_title,
                    formatArgs = wrappedList(serviceName),
                ),
                message = resourceReference(
                    id = R.string.hw_cloud_backup_delete_confirm_message,
                    formatArgs = wrappedList(serviceName),
                ),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_delete),
                        isWarning = true,
                        onClick = { deleteCloudBackup(fileId) },
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_cancel),
                        onClick = {},
                    )
                },
            ),
        )
    }

    private fun deleteCloudBackup(fileId: String) {
        modelScope.launch {
            cloudBackupRepository.get().deleteBackup(fileId).fold(
                ifLeft = { error ->
                    TangemLogger.e("Error on deleting cloud backup: $error")
                    showCloudErrorDialog(error)
                    analyticsEventHandler.send(
                        WalletSettingsAnalyticEvents.CloudBackupDeletionError(errorMessage = error.analyticsMessage()),
                    )
                },
                ifRight = {
                    analyticsEventHandler.send(WalletSettingsAnalyticEvents.CloudBackupDeleted())
                    setCloudBackupStateUseCase.get()(params.userWalletId.stringValue, isBackedUp = false)
                    cloudBackupInfo = null
                    setGoogleDriveStatus(BackupStatus.NoBackup)
                    uiMessageSender.send(
                        SnackbarMessage(
                            message = resourceReference(R.string.hw_cloud_backup_removed),
                            startIconId = R.drawable.ic_check_24,
                        ),
                    )
                },
            )
        }
    }

    private fun showCloudErrorDialog(error: CloudBackupError) {
        if (error == CloudBackupError.AuthCanceled) return

        val (title, message) = when {
            error == CloudBackupError.NetworkError ->
                resourceReference(R.string.common_error) to
                    resourceReference(R.string.hw_cloud_backup_error_network)
            error == CloudBackupError.CloudUnavailable ->
                resourceReference(R.string.common_error) to
                    resourceReference(R.string.hw_cloud_backup_error_unavailable)
            isAccessError(error) ->
                resourceReference(R.string.hw_cloud_backup_permissions_title_v2, wrappedList(serviceName)) to
                    resourceReference(R.string.hw_cloud_backup_permissions_description_v2, wrappedList(serviceName))
            else ->
                resourceReference(R.string.common_error) to
                    resourceReference(R.string.common_unknown_error)
        }
        uiMessageSender.send(
            DialogMessage(
                title = title,
                message = message,
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_ok),
                    onClick = {},
                ),
            ),
        )
    }

    private fun showSeedPhrase(hotWallet: UserWallet.Hot) {
        modelScope.launch {
            unlockHotWalletContextualUseCase.invoke(hotWallet.hotWalletId)
                .fold(
                    ifLeft = {
                        TangemLogger.e("Error while export seed phrase: $it")
                    },
                    ifRight = { seedPhrasePrivateInfo ->
                        router.push(AppRoute.ViewPhrase(params.userWalletId))
                    },
                )
        }
    }

    private fun onHardwareWalletClick() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonHardwareUpdate())
        router.push(AppRoute.WalletHardwareBackup(params.userWalletId))
    }
}

/**
 * Maps the screen status to the `Cloud Backup` analytics value. [BackupStatus.NetworkError] joins
 * `Action Required`: in both cases the wallet is locally marked as backed up but the file could not be
 * confirmed. Transient states carry no value.
 */
private fun BackupStatus.toAnalyticsState(): AnalyticsParam.CloudBackupState? = when (this) {
    BackupStatus.Done -> AnalyticsParam.CloudBackupState.Done
    BackupStatus.NoBackup -> AnalyticsParam.CloudBackupState.Incomplete
    BackupStatus.NetworkError,
    is BackupStatus.ActionRequired,
    -> AnalyticsParam.CloudBackupState.ActionRequired
    BackupStatus.Loading, BackupStatus.ComingSoon -> null
}