package com.tangem.features.details.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
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
) : Model() {

    private val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    private val userWalletsFetcher = userWalletsFetcherFactory.create(
        messageSender = messageSender,
        onlyMultiCurrency = false,
        isAuthMode = false,
        onWalletClick = { userWalletId -> router.push(AppRoute.WalletSettings(userWalletId)) },
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
            router.push(AppRoute.CreateWalletSelection)
        } else {
            withProgress(isWalletSavingInProgress) {
                userWalletSaver.scanAndSaveUserWallet(modelScope)
            }
        }
    }
}