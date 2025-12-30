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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.domain.wallets.usecase.UnlockWalletUseCase
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.impl.R
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UserWalletListModel @Inject constructor(
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val userWalletSaver: UserWalletSaver,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val unlockWalletUseCase: UnlockWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    private val userWalletsFetcher = userWalletsFetcherFactory.create(
        messageSender = messageSender,
        onlyMultiCurrency = false,
        isAuthMode = false,
        isClickableIfLocked = hotWalletFeatureToggles.isHotWalletEnabled,
        onWalletClick = ::onWalletClicked,
    )

    val state: MutableStateFlow<UserWalletListUM> = MutableStateFlow(
        value = UserWalletListUM(
            userWallets = persistentListOf(),
            isWalletSavingInProgress = false,
            addNewWalletText = TextReference.EMPTY,
            onAddNewWalletClick = ::onAddNewWalletClick,
        ),
    )

    init {
        combine(
            flow = userWalletsFetcher.userWallets,
            flow2 = shouldSaveUserWalletsUseCase(),
            flow3 = isWalletSavingInProgress,
        ) { userWallets, shouldSaveUserWallets, isWalletSavingInProgress ->
            updateState(userWallets, shouldSaveUserWallets, isWalletSavingInProgress)
        }.launchIn(modelScope)
    }

    private fun updateState(
        userWallets: ImmutableList<UserWalletItemUM>,
        shouldSaveUserWallets: Boolean,
        isWalletSavingInProgress: Boolean,
    ) = state.update { value ->
        value.copy(
            userWallets = userWallets,
            isWalletSavingInProgress = isWalletSavingInProgress,
            addNewWalletText = if (shouldSaveUserWallets || hotWalletFeatureToggles.isHotWalletEnabled) {
                resourceReference(R.string.user_wallet_list_add_button)
            } else {
                resourceReference(R.string.scan_card_settings_button)
            },
        )
    }

    private fun onAddNewWalletClick() {
        if (hotWalletFeatureToggles.isHotWalletEnabled) {
            analyticsEventHandler.send(SignIn.ButtonAddWallet(AnalyticsParam.ScreensSources.Settings))
            router.push(AppRoute.CreateWalletSelection)
        } else {
            withProgress(isWalletSavingInProgress) {
                userWalletSaver.scanAndSaveUserWallet(modelScope)
            }
        }
    }

    private fun onWalletClicked(userWalletId: UserWalletId) {
        if (hotWalletFeatureToggles.isHotWalletEnabled) {
            modelScope.launch {
                unlockWalletUseCase(userWalletId)
                    .onRight { router.push(AppRoute.WalletSettings(userWalletId)) }
                    .onLeft { error ->
                        Timber.e("Failed to unlock wallet $userWalletId: $error")
                        error.handle(
                            onUserCancelled = {},
                            isFromUnlockAll = false,
                            onAlreadyUnlocked = { router.push(AppRoute.WalletSettings(userWalletId)) },
                            analyticsEventHandler = analyticsEventHandler,
                            showMessage = messageSender::send,
                        )
                    }
            }
        } else {
            router.push(AppRoute.WalletSettings(userWalletId))
        }
    }
}