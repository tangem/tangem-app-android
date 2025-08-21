package com.tangem.features.details.model

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R.*
import com.tangem.core.ui.components.bottomsheets.BottomSheetOption
import com.tangem.core.ui.components.bottomsheets.OptionsBottomSheetContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.impl.R
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class UserWalletListModel @Inject constructor(
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    private val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)
    private val userWalletsFetcher = userWalletsFetcherFactory.create(
        messageSender = messageSender,
        onlyMultiCurrency = false,
        authMode = false,
        onWalletClick = { userWalletId -> router.push(AppRoute.WalletSettings(userWalletId)) },
    )

    val state: MutableStateFlow<UserWalletListUM> = MutableStateFlow(
        value = UserWalletListUM(
            userWallets = persistentListOf(),
            isWalletSavingInProgress = false,
            addNewWalletText = TextReference.EMPTY,
            onAddNewWalletClick = ::showAddWalletBottomSheet,
            addWalletBottomSheet = TangemBottomSheetConfig.Empty,
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
            addNewWalletText = if (shouldSaveUserWallets) {
                resourceReference(R.string.user_wallet_list_add_button)
            } else {
                resourceReference(R.string.scan_card_settings_button)
            },
        )
    }

    private fun showAddWalletBottomSheet() {
        state.update { currentState ->
            currentState.copy(
                addWalletBottomSheet = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::dismissAddWalletBottomSheet,
                    content = createAddWalletBottomSheetContent(),
                ),
            )
        }
    }

    private fun dismissAddWalletBottomSheet() {
        state.update { currentState ->
            currentState.copy(
                addWalletBottomSheet = currentState.addWalletBottomSheet.copy(isShown = false),
            )
        }
    }

    private fun createAddWalletBottomSheetContent(): OptionsBottomSheetContent {
        return OptionsBottomSheetContent(
            options = persistentListOf(
                BottomSheetOption(
                    key = ADD_WALLET_KEY_CREATE,
                    label = resourceReference(string.home_button_create_new_wallet),
                ),
                BottomSheetOption(
                    key = ADD_WALLET_KEY_ADD,
                    label = resourceReference(string.home_button_add_existing_wallet),
                ),
                BottomSheetOption(
                    key = ADD_WALLET_KEY_BUY,
                    label = resourceReference(string.details_buy_wallet),
                ),
            ),
            onOptionClick = { optionKey ->
                dismissAddWalletBottomSheet()
                when (optionKey) {
                    ADD_WALLET_KEY_CREATE -> router.push(AppRoute.CreateWalletSelection)
                    ADD_WALLET_KEY_ADD -> router.push(AppRoute.AddExistingWallet)
                    ADD_WALLET_KEY_BUY -> modelScope.launch {
                        generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
                    }
                }
            },
        )
    }

    companion object {
        private const val ADD_WALLET_KEY_CREATE = "create"
        private const val ADD_WALLET_KEY_ADD = "add"
        private const val ADD_WALLET_KEY_BUY = "buy"
    }
}