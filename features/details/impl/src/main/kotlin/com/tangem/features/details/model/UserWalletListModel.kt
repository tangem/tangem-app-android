package com.tangem.features.details.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.entity.UserWalletListUM.UserWalletUM
import com.tangem.features.details.impl.R
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.details.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ComponentScoped
internal class UserWalletListModel @Inject constructor(
    userWalletsFetcher: UserWalletsFetcher,
    shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val userWalletSaver: UserWalletSaver,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)

    val state: MutableStateFlow<UserWalletListUM> = MutableStateFlow(
        value = UserWalletListUM(
            userWallets = persistentListOf(),
            isWalletSavingInProgress = false,
            addNewWalletText = TextReference.EMPTY,
            onAddNewWalletClick = ::addUserWallet,
        ),
    )

    init {
        combine(
            userWalletsFetcher.userWallets,
            shouldSaveUserWalletsUseCase(),
            isWalletSavingInProgress,
            transform = ::updateState,
        ).launchIn(modelScope)
    }

    private fun updateState(
        userWallets: ImmutableList<UserWalletUM>,
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

    private fun addUserWallet() = withProgress(isWalletSavingInProgress) {
        userWalletSaver.scanAndSaveUserWallet()
    }
}