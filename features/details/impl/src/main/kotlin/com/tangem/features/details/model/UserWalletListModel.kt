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
import com.tangem.domain.wallets.usecase.UnlockWalletUseCase
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.impl.R
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UserWalletListModel @Inject constructor(
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
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
        isClickableIfLocked = true,
        onWalletClick = ::onWalletClicked,
    )

    val state: MutableStateFlow<UserWalletListUM> = MutableStateFlow(
        value = UserWalletListUM(
            userWallets = persistentListOf(),
            isWalletSavingInProgress = false,
            addNewWalletText = TextReference.EMPTY,
            onAddNewWalletClick = ::onAddNewWalletClick,
            addNewWalletIconRes = R.drawable.ic_plus_24,
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
            )
        }

    private fun onAddNewWalletClick() {
        analyticsEventHandler.send(SignIn.ButtonAddWallet(AnalyticsParam.ScreensSources.Settings))

        if (hotWalletFeatureToggles.isWalletCreationRestrictionEnabled) {
            withProgress(isWalletSavingInProgress) {
                userWalletSaver.scanAndSaveUserWallet(modelScope)
            }
        } else {
            router.push(AppRoute.CreateWalletSelection)
        }
    }

    private fun onWalletClicked(userWalletId: UserWalletId) {
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
    }
}