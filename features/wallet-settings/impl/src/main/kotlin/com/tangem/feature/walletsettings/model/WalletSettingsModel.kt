package com.tangem.feature.walletsettings.model

import android.os.Build
import arrow.core.Either
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.ManageTokens.Source
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.OnOffState.Off
import com.tangem.core.analytics.models.AnalyticsParam.OnOffState.On
import com.tangem.core.analytics.utils.TrackingContextProxy
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
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.DisableWalletNFTUseCase
import com.tangem.domain.nft.EnableWalletNFTUseCase
import com.tangem.domain.nft.GetWalletNFTEnabledUseCase
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.settings.repositories.PermissionRepository
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents.RecoveryPhraseScreenAction
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.*
import com.tangem.feature.walletsettings.impl.R
import com.tangem.feature.walletsettings.utils.AccountItemsDelegate
import com.tangem.feature.walletsettings.utils.AccountListSortingSaver
import com.tangem.feature.walletsettings.utils.ItemsBuilder
import com.tangem.feature.walletsettings.utils.WalletCardItemDelegate
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class WalletSettingsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val itemsBuilder: ItemsBuilder,
    private val accountItemsDelegate: AccountItemsDelegate,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val trackingContextProxy: TrackingContextProxy,
    walletCardItemDelegateFactory: WalletCardItemDelegate.Factory,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    getWalletNFTEnabledUseCase: GetWalletNFTEnabledUseCase,
    private val enableWalletNFTUseCase: EnableWalletNFTUseCase,
    private val disableWalletNFTUseCase: DisableWalletNFTUseCase,
    getWalletNotificationsEnabledUseCase: GetWalletNotificationsEnabledUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val settingsManager: SettingsManager,
    private val permissionsRepository: PermissionRepository,
    private val notificationsRepository: NotificationsRepository,
    private val isUpgradeWalletNotificationEnabledUseCase: IsUpgradeWalletNotificationEnabledUseCase,
    private val dismissUpgradeWalletNotificationUseCase: DismissUpgradeWalletNotificationUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val accountListSortingSaver: AccountListSortingSaver,
) : Model() {

    val params: WalletSettingsComponent.Params = paramsContainer.require()
    val dialogNavigation = SlotNavigation<DialogConfig>()
    val bottomSheetNavigation: SlotNavigation<NetworksAvailableForNotificationBSConfig> = SlotNavigation()
    private val walletCardItemDelegate = walletCardItemDelegateFactory.create(dialogNavigation)

    val state: MutableStateFlow<WalletSettingsUM> = MutableStateFlow(
        value = WalletSettingsUM(
            popBack = router::pop,
            items = persistentListOf(),
            hasRequestPushNotificationsPermission = false,
            onPushNotificationPermissionGranted = ::onPushNotificationPermissionGranted,
            isWalletBackedUp = true,
            isWalletUpgradeDismissed = false,
            accountReorderUM = AccountReorderUM(
                isDragEnabled = false,
                onMove = ::onAccountReorder,
                onDragStopped = ::onAccountDragStopped,
            ),
        ),
    )

    init {
        getUserWalletUseCase.invoke(params.userWalletId).onRight { wallet ->
            trackingContextProxy.addContext(wallet)
            modelScope.launch {
                val accountsCount = if (isAccountsModeEnabledUseCase.invokeSync()) {
                    singleAccountListSupplier(wallet.walletId)
                        .first()
                        .accounts
                        .size
                } else {
                    null
                }
                val event = WalletSettingsAnalyticEvents.WalletSettingsScreenOpened(accountsCount)
                analyticsEventHandler.send(event)
            }
        }

        fun combineUI(wallet: UserWallet) = combine(
            flow = getWalletNFTEnabledUseCase.invoke(params.userWalletId)
                .distinctUntilChanged()
                .conflate(),
            flow2 = getWalletNotificationsEnabledUseCase(params.userWalletId)
                .distinctUntilChanged()
                .conflate(),
            flow3 = isUpgradeWalletNotificationEnabledUseCase(params.userWalletId),
            flow4 = walletCardItemDelegate.cardItemFlow(wallet),
            flow5 = accountItemsDelegate.loadAccount(wallet),
        ) { nftEnabled, notificationsEnabled, isUpgradeNotificationEnabled, cardItem, accountList ->
            val isWalletBackedUp = when (wallet) {
                is UserWallet.Hot -> wallet.backedUp
                is UserWallet.Cold -> true
            }
            state.update { value ->
                value.copy(
                    items = buildItems(
                        userWallet = wallet,
                        cardItem = cardItem,
                        isNFTEnabled = nftEnabled,
                        isNotificationsEnabled = notificationsEnabled,
                        isNotificationsPermissionGranted = isNotificationsPermissionGranted(),
                        isUpgradeNotificationEnabled = isUpgradeNotificationEnabled,
                        accountList = accountList,
                    ),
                    accountReorderUM = AccountReorderUM(
                        isDragEnabled = accountsFeatureToggles.isFeatureEnabled &&
                            accountList.count { it is WalletSettingsAccountsUM.Account } > 1,
                        onMove = ::onAccountReorder,
                        onDragStopped = ::onAccountDragStopped,
                    ),
                    isWalletBackedUp = isWalletBackedUp,
                )
            }
        }
        getUserWalletUseCase.invokeFlow(params.userWalletId)
            .distinctUntilChanged()
            .filterIsInstance<Either.Right<UserWallet>>()
            .flatMapLatest { combineUI(it.value) }
            .launchIn(modelScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingContextProxy.removeContext()
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
        isNotificationsEnabled: Boolean,
        isNotificationsPermissionGranted: Boolean,
        isUpgradeNotificationEnabled: Boolean,
        accountList: List<WalletSettingsAccountsUM>,
    ): PersistentList<WalletSettingsItemUM> {
        val isAccountsFeatureEnabled = accountItemsDelegate.isAccountsSupported(userWallet)
        val isMultiCurrency = when (userWallet) {
            is UserWallet.Cold -> userWallet.isMultiCurrency
            is UserWallet.Hot -> true
        }
        return itemsBuilder.buildItems(
            userWallet = userWallet,
            cardItem = cardItem,
            isReferralAvailable = when (userWallet) {
                is UserWallet.Cold -> userWallet.cardTypesResolver.isTangemWallet()
                is UserWallet.Hot -> true
            },
            isLinkMoreCardsAvailable = when (userWallet) {
                is UserWallet.Cold -> userWallet.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup
                is UserWallet.Hot -> false
            },
            isManageTokensAvailable = if (isAccountsFeatureEnabled) {
                isMultiCurrency && accountList.count { it is WalletSettingsAccountsUM.Account } == 0
            } else {
                isMultiCurrency
            },
            isNFTFeatureEnabled = isMultiCurrency,
            isNFTEnabled = isNFTEnabled,
            onCheckedNFTChange = ::onCheckedNFTChange,
            forgetWallet = {
                onForgetWalletClick(userWallet)
            },
            onLinkMoreCardsClick = {
                when (userWallet) {
                    is UserWallet.Cold -> onLinkMoreCardsClick(scanResponse = userWallet.scanResponse)
                    is UserWallet.Hot -> Unit
                }
            },
            onReferralClick = { onReferralClick(userWallet) },
            onManageTokensClick = {
                analyticsEventHandler.send(Settings.ButtonManageTokens())
                router.push(
                    AppRoute.ManageTokens(
                        source = Source.SETTINGS,
                        portfolioId = if (isAccountsFeatureEnabled) {
                            PortfolioId(
                                accountId = AccountId.forMainCryptoPortfolio(userWalletId = userWallet.walletId),
                            )
                        } else {
                            PortfolioId(userWalletId = userWallet.walletId)
                        },
                    ),
                )
            },
            isNotificationsEnabled = isNotificationsEnabled,
            isNotificationsPermissionGranted = isNotificationsPermissionGranted,
            onCheckedNotificationsChanged = ::onCheckedNotificationsChange,
            onNotificationsDescriptionClick = ::onNotificationsDescriptionClick,
            onAccessCodeClick = { onAccessCodeClick(userWallet) },
            walletUpgradeDismissed = isUpgradeNotificationEnabled,
            onUpgradeWalletClick = { onUpgradeWalletClick() },
            onDismissUpgradeWalletClick = ::onDismissUpgradeWalletClick,
            onBackupClick = ::onBackupClick,
            onCardSettingsClick = ::onCardSettingsClick,
            accountsUM = accountList,
        )
    }

    private fun forgetWallet() = modelScope.launch {
        val hasUserWallets = deleteWalletUseCase(params.userWalletId).getOrElse { error ->
            Timber.e("Unable to delete wallet: $error")

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
        trackingContextProxy.addContext(scanResponse)

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
                state.update { value ->
                    value.copy(
                        hasRequestPushNotificationsPermission = true,
                    )
                }
            } else {
                setNotificationsEnabledUseCase(params.userWalletId, false)
                analyticsEventHandler.send(PushNotificationAnalyticEvents.NotificationsEnabled(false))
            }
        }
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
                hasRequestPushNotificationsPermission = false,
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

    private fun onAccessCodeClick(userWallet: UserWallet) {
        if (userWallet is UserWallet.Hot) {
            val isCodeSet = userWallet.hotWalletId.authType != HotWalletId.AuthType.NoPassword
            analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonAccessCode(isCodeSet))
            if (!state.value.isWalletBackedUp) {
                showMakeBackupAtFirstAlertBS(
                    isUpgradeFlow = false,
                    action = WalletSettingsAnalyticEvents.NoticeBackupFirst.Action.AccessCode,
                )
            } else {
                unlockWalletIfNeedAndProceed { authorizationRequired ->
                    router.push(
                        route = AppRoute.UpdateAccessCode(
                            userWalletId = params.userWalletId,
                            source = AnalyticsParam.ScreensSources.WalletSettings.value,
                        ),
                    )
                }
            }
        }
    }

    private fun onUpgradeWalletClick() {
        if (!state.value.isWalletBackedUp) {
            showMakeBackupAtFirstAlertBS(
                isUpgradeFlow = true,
                action = WalletSettingsAnalyticEvents.NoticeBackupFirst.Action.Upgrade,
            )
        } else {
            unlockWalletIfNeedAndProceed {
                router.push(AppRoute.UpgradeWallet(userWalletId = params.userWalletId))
            }
        }
    }

    private fun onDismissUpgradeWalletClick() {
        modelScope.launch {
            dismissUpgradeWalletNotificationUseCase.invoke(params.userWalletId)
        }
    }

    private fun onBackupClick() {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonBackup())
        router.push(AppRoute.WalletBackup(params.userWalletId))
    }

    private fun onCardSettingsClick() {
        router.push(AppRoute.CardSettings(params.userWalletId))
    }

    private fun showMakeBackupAtFirstAlertBS(
        action: WalletSettingsAnalyticEvents.NoticeBackupFirst.Action,
        isUpgradeFlow: Boolean,
    ) {
        analyticsEventHandler.send(
            event = WalletSettingsAnalyticEvents.NoticeBackupFirst(
                source = AnalyticsParam.ScreensSources.WalletSettings.value,
                action = action,
            ),
        )
        val message = bottomSheetMessage {
            infoBlock {
                icon(R.drawable.ic_passcode_lock_32) {
                    type = MessageBottomSheetUMV2.Icon.Type.Accent
                    backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                }
                title = resourceReference(R.string.hw_backup_need_finish_first)
                body = resourceReference(R.string.hw_backup_to_secure_description)
            }
            primaryButton {
                text = resourceReference(R.string.hw_backup_need_action)
                onClick {
                    router.push(
                        AppRoute.CreateWalletBackup(
                            userWalletId = params.userWalletId,
                            isUpgradeFlow = isUpgradeFlow,
                            shouldSetAccessCode = true,
                            analyticsSource = AnalyticsParam.ScreensSources.WalletSettings.value,
                            analyticsAction = if (isUpgradeFlow) {
                                RecoveryPhraseScreenAction.Upgrade.value
                            } else {
                                RecoveryPhraseScreenAction.AccessCode.value
                            },
                        ),
                    )
                    closeBs()
                }
            }
        }
        messageSender.send(message)
    }

    private fun unlockWalletIfNeedAndProceed(action: (authorizationRequired: Boolean) -> Unit) {
        val userWallet = getUserWalletUseCase(params.userWalletId)
            .getOrElse { error("User wallet with id ${params.userWalletId} not found") }
        if (userWallet is UserWallet.Hot) {
            val hotWalletId = userWallet.hotWalletId
            when (hotWalletId.authType) {
                HotWalletId.AuthType.NoPassword -> {
                    action(false)
                }
                HotWalletId.AuthType.Password,
                HotWalletId.AuthType.Biometry,
                -> modelScope.launch {
                    unlockHotWalletContextualUseCase.invoke(hotWalletId)
                        .onLeft {
                            Timber.e(it, "Unable to unlock wallet with id ${params.userWalletId}")
                        }
                        .onRight {
                            action(true)
                        }
                }
            }
        }
    }

    private fun onAccountReorder(fromIndex: Int, toIndex: Int) {
        state.update { prevState ->
            prevState.copy(
                items = prevState.items.mutate {
                    it.add(toIndex - 1, it.removeAt(fromIndex - 1))
                },
            )
        }
    }

    private fun onAccountDragStopped() {
        val accountIds = state.value.items.mapNotNull { itemUM ->
            val id = (itemUM as? WalletSettingsAccountsUM.Account)?.state?.id
                ?: return@mapNotNull null

            AccountId.forCryptoPortfolio(userWalletId = params.userWalletId, value = id).getOrNull()
        }

        accountListSortingSaver.save(accountIds = accountIds)
    }

    @Suppress("LongMethod")
    private fun onForgetWalletClick(userWallet: UserWallet) {
        val message = when (userWallet) {
            is UserWallet.Cold -> {
                DialogMessage(
                    message = resourceReference(
                        id = R.string.user_wallet_list_delete_prompt,
                    ),
                    firstActionBuilder = {
                        EventMessageAction(
                            title = resourceReference(R.string.common_forget),
                            isWarning = true,
                            onClick = ::forgetWallet,
                        )
                    },
                    secondActionBuilder = { cancelAction() },
                )
            }
            is UserWallet.Hot -> {
                if (!userWallet.backedUp) {
                    analyticsEventHandler.send(
                        event = WalletSettingsAnalyticEvents.NoticeBackupFirst(
                            source = AnalyticsParam.ScreensSources.WalletSettings.value,
                            action = WalletSettingsAnalyticEvents.NoticeBackupFirst.Action.Remove,
                        ),
                    )
                }

                bottomSheetMessage {
                    infoBlock {
                        icon(R.drawable.ic_alert_circle_24) {
                            type = MessageBottomSheetUMV2.Icon.Type.Warning
                            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                        }
                        title = resourceReference(R.string.hw_remove_wallet_notification_title)
                        body = if (userWallet.backedUp) {
                            resourceReference(R.string.hw_remove_wallet_notification_description_has_backup)
                        } else {
                            resourceReference(R.string.hw_remove_wallet_notification_description_without_backup)
                        }
                    }
                    if (userWallet.backedUp) {
                        secondaryButton {
                            text = resourceReference(R.string.hw_remove_wallet_notification_action_forget)
                            onClick {
                                router.push(AppRoute.ForgetWallet(userWallet.walletId))
                                closeBs()
                            }
                        }
                    } else {
                        secondaryButton {
                            text = resourceReference(R.string.hw_remove_wallet_notification_action_forget_anyway)
                            onClick {
                                router.push(AppRoute.ForgetWallet(userWallet.walletId))
                                closeBs()
                            }
                        }
                    }
                    if (userWallet.backedUp) {
                        primaryButton {
                            text = resourceReference(R.string.hw_remove_wallet_notification_action_backup_view)
                            onClick {
                                unlockWalletIfNeedAndProceed {
                                    router.push(AppRoute.ViewPhrase(userWallet.walletId))
                                }
                                closeBs()
                            }
                        }
                    } else {
                        primaryButton {
                            text = resourceReference(R.string.hw_remove_wallet_notification_action_backup_go)
                            onClick {
                                router.push(
                                    AppRoute.CreateWalletBackup(
                                        userWalletId = userWallet.walletId,
                                        analyticsSource = AnalyticsParam.ScreensSources.WalletSettings.value,
                                        analyticsAction = RecoveryPhraseScreenAction.Remove.value,
                                    ),
                                )
                                closeBs()
                            }
                        }
                    }
                }
            }
        }

        messageSender.send(message)
    }
}