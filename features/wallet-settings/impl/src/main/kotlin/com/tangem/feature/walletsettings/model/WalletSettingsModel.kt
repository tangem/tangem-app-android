package com.tangem.feature.walletsettings.model

import android.os.Build
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam.OnOffState.Off
import com.tangem.core.analytics.models.AnalyticsParam.OnOffState.On
import com.tangem.core.analytics.utils.AnalyticsContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.nft.DisableWalletNFTUseCase
import com.tangem.domain.nft.EnableWalletNFTUseCase
import com.tangem.domain.nft.GetWalletNFTEnabledUseCase
import com.tangem.domain.notifications.GetIsHuaweiDeviceWithoutGoogleServicesUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.notifications.toggles.NotificationsFeatureToggles
import com.tangem.domain.settings.repositories.PermissionRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.walletsettings.analytics.Settings
import com.tangem.feature.walletsettings.analytics.WalletSettingsAnalyticEvents
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.entity.NetworksAvailableForNotificationBSConfig
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WalletSettingsModel @Inject constructor(
    getWalletUseCase: GetUserWalletUseCase,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val itemsBuilder: ItemsBuilder,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsContextProxy: AnalyticsContextProxy,
    private val getShouldSaveUserWalletsSyncUseCase: ShouldSaveUserWalletsSyncUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    getWalletNFTEnabledUseCase: GetWalletNFTEnabledUseCase,
    private val enableWalletNFTUseCase: EnableWalletNFTUseCase,
    private val disableWalletNFTUseCase: DisableWalletNFTUseCase,
    private val notificationsToggles: NotificationsFeatureToggles,
    getWalletNotificationsEnabledUseCase: GetWalletNotificationsEnabledUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val settingsManager: SettingsManager,
    private val permissionsRepository: PermissionRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val notificationsRepository: NotificationsRepository,
    private val getIsHuaweiDeviceWithoutGoogleServicesUseCase: GetIsHuaweiDeviceWithoutGoogleServicesUseCase,
) : Model() {

    val params: WalletSettingsComponent.Params = paramsContainer.require()
    val dialogNavigation = SlotNavigation<DialogConfig>()
    val bottomSheetNavigation: SlotNavigation<NetworksAvailableForNotificationBSConfig> = SlotNavigation()

    val state: MutableStateFlow<WalletSettingsUM> = MutableStateFlow(
        value = WalletSettingsUM(
            popBack = router::pop,
            items = persistentListOf(),
            requestPushNotificationsPermission = false,
            onPushNotificationPermissionGranted = ::onPushNotificationPermissionGranted,
        ),
    )

    init {
        combine(
            getWalletUseCase.invokeFlow(params.userWalletId).distinctUntilChanged(),
            getWalletNFTEnabledUseCase.invoke(params.userWalletId),
            getWalletNotificationsEnabledUseCase(params.userWalletId),
        ) { maybeWallet, nftEnabled, notificationsEnabled ->
            val wallet = maybeWallet.getOrNull() ?: return@combine
            wallet.requireColdWallet() // TODO [REDACTED_TASK_KEY] [Hot Wallet] Wallet Settings
            val isRenameWalletAvailable = getShouldSaveUserWalletsSyncUseCase()
            val isNeedShowNotifications = notificationsToggles.isNotificationsEnabled &&
                !getIsHuaweiDeviceWithoutGoogleServicesUseCase()
            state.update { value ->
                value.copy(
                    items = buildItems(
                        userWallet = wallet,
                        dialogNavigation = dialogNavigation,
                        isRenameWalletAvailable = isRenameWalletAvailable,
                        isNFTEnabled = nftEnabled,
                        isNotificationsEnabled = notificationsEnabled,
                        isNotificationsFeatureEnabled = isNeedShowNotifications,
                        isNotificationsPermissionGranted = isNotificationsPermissionGranted(),
                        isHotWalletEnabled = hotWalletFeatureToggles.isHotWalletEnabled,
                    ),
                )
            }
        }
            .launchIn(modelScope)
    }

    private fun isNotificationsPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsRepository.hasRuntimePermission(
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
        } else {
            true
        }
    }

    private fun buildItems(
        userWallet: UserWallet.Cold,
        dialogNavigation: SlotNavigation<DialogConfig>,
        isRenameWalletAvailable: Boolean,
        isNFTEnabled: Boolean,
        isNotificationsFeatureEnabled: Boolean,
        isNotificationsEnabled: Boolean,
        isNotificationsPermissionGranted: Boolean,
        isHotWalletEnabled: Boolean,
    ): PersistentList<WalletSettingsItemUM> = itemsBuilder.buildItems(
        userWalletId = userWallet.walletId,
        userWalletName = userWallet.name,
        isReferralAvailable = userWallet.cardTypesResolver.isTangemWallet(),
        isLinkMoreCardsAvailable = userWallet.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup,
        isManageTokensAvailable = userWallet.isMultiCurrency,
        isRenameWalletAvailable = isRenameWalletAvailable,
        renameWallet = { openRenameWalletDialog(userWallet, dialogNavigation) },
        isNFTFeatureEnabled = userWallet.isMultiCurrency,
        isNFTEnabled = isNFTEnabled,
        onCheckedNFTChange = ::onCheckedNFTChange,
        forgetWallet = {
            val message = DialogMessage(
                message = resourceReference(R.string.user_wallet_list_delete_prompt),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_delete),
                        warning = true,
                        onClick = ::forgetWallet,
                    )
                },
                secondActionBuilder = { cancelAction() },
            )

            messageSender.send(message)
        },
        onLinkMoreCardsClick = {
            onLinkMoreCardsClick(scanResponse = userWallet.scanResponse)
        },
        onReferralClick = { onReferralClick(userWallet) },
        isNotificationsEnabled = isNotificationsEnabled,
        isNotificationsFeatureEnabled = isNotificationsFeatureEnabled,
        isNotificationsPermissionGranted = isNotificationsPermissionGranted,
        onCheckedNotificationsChanged = ::onCheckedNotificationsChange,
        onNotificationsDescriptionClick = ::onNotificationsDescriptionClick,
        isHotWalletEnabled = isHotWalletEnabled,
    )

    private fun openRenameWalletDialog(userWallet: UserWallet, dialogNavigation: SlotNavigation<DialogConfig>) {
        val config = DialogConfig.RenameWallet(
            userWalletId = userWallet.walletId,
            currentName = userWallet.name,
        )

        dialogNavigation.activate(config)
    }

    private fun forgetWallet() = modelScope.launch {
        val hasUserWallets = deleteWalletUseCase(params.userWalletId).getOrElse {
            Timber.e("Unable to delete wallet: $it")

            messageSender.send(
                message = SnackbarMessage(resourceReference(R.string.common_unknown_error)),
            )

            return@launch
        }

        if (hasUserWallets) {
            router.pop()
        } else {
            router.replaceAll(AppRoute.Home())
        }
    }

    private fun onLinkMoreCardsClick(scanResponse: ScanResponse) {
        analyticsEventHandler.send(Settings.ButtonCreateBackup)
        analyticsContextProxy.addContext(scanResponse)

        router.push(
            AppRoute.Onboarding(
                scanResponse = scanResponse,
                mode = AppRoute.Onboarding.Mode.AddBackupWallet1,
            ),
        )
    }

    private fun onCheckedNFTChange(isChecked: Boolean) {
        modelScope.launch {
            if (isChecked) {
                enableWalletNFTUseCase.invoke(params.userWalletId)
            } else {
                disableWalletNFTUseCase.invoke(params.userWalletId)
            }
            analyticsEventHandler.send(
                WalletSettingsAnalyticEvents.NftToggleSwitch(
                    enabled = if (isChecked) On else Off,
                ),
            )
        }
    }

    private fun onCheckedNotificationsChange(isChecked: Boolean) {
        modelScope.launch {
            if (isChecked) {
                if (getIsHuaweiDeviceWithoutGoogleServicesUseCase()) {
                    showHuaweiDialog()
                    return@launch
                }
                state.update { value ->
                    value.copy(
                        requestPushNotificationsPermission = true,
                    )
                }
            } else {
                setNotificationsEnabledUseCase(params.userWalletId, false)
                analyticsEventHandler.send(PushNotificationAnalyticEvents.NotificationsEnabled(false))
            }
        }
    }

    private fun showHuaweiDialog() {
        val message = DialogMessage(
            message = resourceReference(R.string.wallet_settings_push_notifications_huawei_warning),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_ok),
                    warning = true,
                    onClick = {},
                )
            },
        )

        messageSender.send(message)
    }

    private fun onNotificationsDescriptionClick() {
        bottomSheetNavigation.activate(NetworksAvailableForNotificationBSConfig)
    }

    private fun openPushSystemSettings() {
        settingsManager.openAppSettings()
    }

    private fun onPushNotificationPermissionGranted(isGranted: Boolean) {
        analyticsEventHandler.send(PushNotificationAnalyticEvents.PermissionStatus(isGranted))
        state.update { value ->
            value.copy(
                requestPushNotificationsPermission = false,
            )
        }
        if (isGranted) {
            modelScope.launch {
                setNotificationsEnabledUseCase(params.userWalletId, true).onRight {
                    notificationsRepository.setNotificationsWasEnabledAutomatically(params.userWalletId.stringValue)
                    analyticsEventHandler.send(PushNotificationAnalyticEvents.NotificationsEnabled(true))
                }
            }
        } else {
            val message = DialogMessage(
                title = resourceReference(R.string.push_notifications_permission_alert_title),
                message = resourceReference(R.string.push_notifications_permission_alert_description),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.push_notifications_permission_alert_positive_button),
                        onClick = ::openPushSystemSettings,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.push_notifications_permission_alert_negative_button),
                        onClick = { cancelAction() },
                    )
                },
            )

            messageSender.send(message)
        }
    }

    private fun onReferralClick(userWallet: UserWallet) {
        if (userWallet is UserWallet.Cold && isDemoCardUseCase(userWallet.cardId)) {
            messageSender.send(
                DialogMessage(message = resourceReference(R.string.alert_demo_feature_disabled)),
            )
        } else {
            router.push(AppRoute.ReferralProgram(userWallet.walletId))
        }
    }
}