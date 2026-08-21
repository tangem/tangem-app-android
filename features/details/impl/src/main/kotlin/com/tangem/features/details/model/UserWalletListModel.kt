package com.tangem.features.details.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.handle
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.SignIn
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.ApplyUserWalletListSortingUseCase
import com.tangem.domain.wallets.usecase.UnlockWalletUseCase
import com.tangem.features.details.entity.AddWalletBS
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.entity.WalletReorderUM
import com.tangem.features.details.impl.R
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UserWalletListModel @Inject constructor(
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val userWalletSaver: UserWalletSaver,
    hotWalletRestrictionManager: HotWalletRestrictionManager,
    private val unlockWalletUseCase: UnlockWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val applyUserWalletListSortingUseCase: ApplyUserWalletListSortingUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    private val isWalletCreationRestrictionEnabled: StateFlow<Boolean> =
        hotWalletRestrictionManager.isCreationEnabled()
    private val userWalletsFetcher = userWalletsFetcherFactory.create(
        messageSender = messageSender,
        onlyMultiCurrency = false,
        isAuthMode = false,
        isClickableIfLocked = true,
        onWalletClick = ::onWalletClicked,
    )

    val state: MutableStateFlow<UserWalletListUM> = MutableStateFlow(
        value = UserWalletListUM(
            userWallets = persistentListOf(),
            isWalletSavingInProgress = false,
            addNewWalletText = TextReference.EMPTY,
            onAddNewWalletClick = ::onAddNewWalletClick,
            walletReorderUM = WalletReorderUM(
                isDragEnabled = false,
                onMove = ::onWalletReorder,
                onDragStopped = ::onWalletDragStopped,
            ),
            addWalletBSConfig = TangemBottomSheetConfig.Empty,
        ),
    )

    init {
        modelScope.launch {
            val userWalletsFlow = userWalletsFetcher.userWallets.stateIn(this)
            state.update { value -> value.copy(userWallets = userWalletsFlow.value) }

            combine(
                flow = userWalletsFlow,
                flow2 = isWalletSavingInProgress,
            ) { userWallets, isWalletSavingInProgress ->
                updateState(userWallets, isWalletSavingInProgress)
            }.collect()
        }
    }

    private fun updateState(userWallets: ImmutableList<UserWalletItemUM>, isWalletSavingInProgress: Boolean) =
        state.update { value ->
            value.copy(
                userWallets = userWallets,
                isWalletSavingInProgress = isWalletSavingInProgress,
                addNewWalletText = resourceReference(R.string.user_wallet_list_add_button),
                walletReorderUM = WalletReorderUM(
                    isDragEnabled = userWallets.size > 1,
                    onMove = ::onWalletReorder,
                    onDragStopped = ::onWalletDragStopped,
                ),
            )
        }

    private fun onAddNewWalletClick() {
        analyticsEventHandler.send(SignIn.ButtonAddWallet(AnalyticsParam.ScreensSources.Settings))

        if (isWalletCreationRestrictionEnabled.value) {
            showAddWalletBottomSheet()
        } else {
            router.push(AppRoute.CreateWalletSelection)
        }
    }

    private fun showAddWalletBottomSheet() {
        state.update { value ->
            value.copy(
                addWalletBSConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::hideAddWalletBottomSheet,
                    content = AddWalletBS(
                        onAddHardwareWalletClick = ::onAddHardwareWalletClick,
                        onAddMobileWalletClick = ::onAddMobileWalletClick,
                    ),
                ),
            )
        }
    }

    private fun hideAddWalletBottomSheet() {
        state.update { value ->
            value.copy(addWalletBSConfig = value.addWalletBSConfig.copy(isShown = false))
        }
    }

    private fun onAddHardwareWalletClick() {
        analyticsEventHandler.send(Settings.ButtonAddHardwareWallet(getWalletsType()))
        hideAddWalletBottomSheet()

        withProgress(isWalletSavingInProgress) {
            userWalletSaver.scanAndSaveUserWallet(modelScope)
        }
    }

    private fun onAddMobileWalletClick() {
        val walletsType = getWalletsType()

        analyticsEventHandler.send(Settings.ButtonAddMobileWallet(walletsType))
        hideAddWalletBottomSheet()

        analyticsEventHandler.send(Settings.NoticeMoreMobileWallets(walletsType))
        messageSender.send(
            message = DialogMessage(
                title = resourceReference(R.string.common_coming_soon),
                message = resourceReference(R.string.user_wallet_coming_soon_dialog_description),
            ),
        )
    }

    private fun getWalletsType(): AnalyticsParam.WalletsType? {
        val userWallets = userWalletsListRepository.userWallets.value ?: return null
        val hasMobile = userWallets.any { it is UserWallet.Hot }
        val hasCold = userWallets.any { it is UserWallet.Cold }

        return when {
            hasMobile && hasCold -> AnalyticsParam.WalletsType.Multiple
            hasMobile -> AnalyticsParam.WalletsType.Mobile
            hasCold -> AnalyticsParam.WalletsType.Cold
            else -> null
        }
    }

    private fun onWalletClicked(userWalletId: UserWalletId) {
        modelScope.launch {
            unlockWalletUseCase(userWalletId, AnalyticsParam.ScreensSources.Settings)
                .onRight { router.push(AppRoute.WalletSettings(userWalletId)) }
                .onLeft { error ->
                    TangemLogger.e("Failed to unlock wallet $userWalletId: $error")
                    error.handle(
                        onUserCancelled = {},
                        isFromUnlockAll = false,
                        onAlreadyUnlocked = { router.push(AppRoute.WalletSettings(userWalletId)) },
                        analyticsEventHandler = analyticsEventHandler,
                        showMessage = messageSender::send,
                    )
                }
        }
    }

    private fun onWalletReorder(fromIndex: Int, toIndex: Int) {
        state.update { prevState ->
            val wallets = prevState.userWallets.toMutableList()
            wallets.add(toIndex, wallets.removeAt(fromIndex))
            prevState.copy(userWallets = wallets.toPersistentList())
        }
    }

    private fun onWalletDragStopped() {
        val userWalletIds = state.value.userWallets.map { UserWalletId(it.id) }

        modelScope.launch {
            applyUserWalletListSortingUseCase(userWalletIds).onRight {
                analyticsEventHandler.send(WalletSettingsAnalyticEvents.WalletsReorder())
            }.onLeft { error ->
                TangemLogger.e("Failed to apply wallet list sorting: $error")
            }
        }
    }
}