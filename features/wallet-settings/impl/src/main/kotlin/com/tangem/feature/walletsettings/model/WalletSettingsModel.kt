package com.tangem.feature.walletsettings.model

import android.os.Build
import arrow.core.Either
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
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.bottomSheetMessage
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.DisableWalletNFTUseCase
import com.tangem.domain.nft.EnableWalletNFTUseCase
import com.tangem.domain.nft.GetWalletNFTEnabledUseCase
import com.tangem.domain.notifications.GetIsHuaweiDeviceWithoutGoogleServicesUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.settings.repositories.PermissionRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.walletsettings.analytics.Settings
import com.tangem.feature.walletsettings.analytics.WalletSettingsAnalyticEvents
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.DialogConfig
import com.tangem.feature.walletsettings.entity.NetworksAvailableForNotificationBSConfig
import com.tangem.feature.walletsettings.entity.WalletSettingsItemUM
import com.tangem.feature.walletsettings.entity.WalletSettingsUM
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.utils.AccountItemsDelegate
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.feature.walletsettings.utils.WalletCardItemDelegate
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class WalletSettingsModel @Inject constructor(
    getWalletUseCase: GetUserWalletUseCase,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val itemsBuilder: ItemsBuilder,
    private val accountItemsDelegate: AccountItemsDelegate,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val analyticsContextProxy: AnalyticsContextProxy,
    walletCardItemDelegateFactory: WalletCardItemDelegate.Factory,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    getWalletNFTEnabledUseCase: GetWalletNFTEnabledUseCase,
    private val enableWalletNFTUseCase: EnableWalletNFTUseCase,
    private val disableWalletNFTUseCase: DisableWalletNFTUseCase,
    getWalletNotificationsEnabledUseCase: GetWalletNotificationsEnabledUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val settingsManager: SettingsManager,
    private val permissionsRepository: PermissionRepository,
    private val notificationsRepository: NotificationsRepository,
    private val getIsHuaweiDeviceWithoutGoogleServicesUseCase: GetIsHuaweiDeviceWithoutGoogleServicesUseCase,
    private val isUpgradeWalletNotificationEnabledUseCase: IsUpgradeWalletNotificationEnabledUseCase,
    private val dismissUpgradeWalletNotificationUseCase: DismissUpgradeWalletNotificationUseCase,
) : Model() {

    val params: WalletSettingsComponent.Params = paramsContainer.require()
    val dialogNavigation = SlotNavigation<DialogConfig>()
    val bottomSheetNavigation: SlotNavigation<NetworksAvailableForNotificationBSConfig> = SlotNavigation()
    private val walletCardItemDelegate = walletCardItemDelegateFactory.create(dialogNavigation)

    val state: MutableStateFlow<WalletSettingsUM> = MutableStateFlow(
        value = WalletSettingsUM(
            popBack = router::pop,
            items = persistentListOf(),
            requestPushNotificationsPermission = false,
            onPushNotificationPermissionGranted = ::onPushNotificationPermissionGranted,
            isWalletBackedUp = true,
            walletUpgradeDismissed = false,
        ),
    )

    private val makeBackupAtFirstAlertBS
        get() = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_passcode_lock_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.hw_backup_need_title)
                body = resourceReference(R.string.hw_backup_need_description)
            }
            secondaryButton {
                text = resourceReference(R.string.hw_backup_need_action)
                onClick {
                    router.push(AppRoute.CreateWalletBackup(params.userWalletId))
                    closeBs()
                }
            }
        }

    init {
        fun combineUI(wallet: UserWallet) = combine(
            getWalletNFTEnabledUseCase.invoke(params.userWalletId),
            getWalletNotificationsEnabledUseCase(params.userWalletId),
            isUpgradeWalletNotificationEnabledUseCase(params.userWalletId),
            walletCardItemDelegate.cardItemFlow(wallet),
        ) { nftEnabled, notificationsEnabled, isUpgradeNotificationEnabled, cardItem ->
            val isWalletBackedUp = when (wallet) {
                is UserWallet.Hot -> wallet.backedUp
                is UserWallet.Cold -> true
            }
            val isNeedShowNotifications = !getIsHuaweiDeviceWithoutGoogleServicesUseCase()
            state.update { value ->
                value.copy(
                    items = buildItems(
                        userWallet = wallet,
                        cardItem = cardItem,
                        isNFTEnabled = nftEnabled,
                        isNotificationsEnabled = notificationsEnabled,
                        isNotificationsFeatureEnabled = isNeedShowNotifications,
                        isNotificationsPermissionGranted = isNotificationsPermissionGranted(),
                        isUpgradeNotificationEnabled = isUpgradeNotificationEnabled,
                    ),
                    isWalletBackedUp = isWalletBackedUp,
                )
            }
        }
        getWalletUseCase.invokeFlow(params.userWalletId)
            .distinctUntilChanged()
            .filterIsInstance<Either.Right<UserWallet>>()
            .flatMapLatest { combineUI(it.value) }
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
        userWallet: UserWallet,
        cardItem: WalletSettingsItemUM.CardBlock,
        isNFTEnabled: Boolean,
        isNotificationsFeatureEnabled: Boolean,
        isNotificationsEnabled: Boolean,
        isNotificationsPermissionGranted: Boolean,
        isUpgradeNotificationEnabled: Boolean,
    ): PersistentList<WalletSettingsItemUM> {
        val isMultiCurrency = when (userWallet) {
            is UserWallet.Cold -> userWallet.isMultiCurrency
            is UserWallet.Hot -> true
        }
        return itemsBuilder.buildItems(
            userWallet = userWallet,
            cardItem = cardItem,
            isReferralAvailable = when (userWallet) {
                is UserWallet.Cold -> userWallet.cardTypesResolver.isTangemWallet()
                is UserWallet.Hot -> false
            },
            isLinkMoreCardsAvailable = when (userWallet) {
                is UserWallet.Cold -> userWallet.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup
                is UserWallet.Hot -> false
            },
            isManageTokensAvailable = isMultiCurrency,
            isNFTFeatureEnabled = isMultiCurrency,
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
                when (userWallet) {
                    is UserWallet.Cold -> onLinkMoreCardsClick(scanResponse = userWallet.scanResponse)
                    is UserWallet.Hot -> Unit
                }
            },
            onReferralClick = { onReferralClick(userWallet) },
            isNotificationsEnabled = isNotificationsEnabled,
            isNotificationsFeatureEnabled = isNotificationsFeatureEnabled,
            isNotificationsPermissionGranted = isNotificationsPermissionGranted,
            onCheckedNotificationsChanged = ::onCheckedNotificationsChange,
            onNotificationsDescriptionClick = ::onNotificationsDescriptionClick,
            onAccessCodeClick = ::onAccessCodeClick,
            walletUpgradeDismissed = isUpgradeNotificationEnabled,
            onUpgradeWalletClick = ::onUpgradeWalletClick,
            onDismissUpgradeWalletClick = ::onDismissUpgradeWalletClick,
            accountsUM = with(accountItemsDelegate) { listOf() }, // todo account
        )
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

    private fun onAccessCodeClick() {
        if (!state.value.isWalletBackedUp) {
            messageSender.send(makeBackupAtFirstAlertBS)
        } else {
            router.push(AppRoute.UpdateAccessCode(params.userWalletId))
        }
    }

    private fun onUpgradeWalletClick() {
        router.push(AppRoute.UpgradeWallet(params.userWalletId))
    }

    private fun onDismissUpgradeWalletClick() {
        modelScope.launch {
            dismissUpgradeWalletNotificationUseCase.invoke(params.userWalletId)
        }
    }
}