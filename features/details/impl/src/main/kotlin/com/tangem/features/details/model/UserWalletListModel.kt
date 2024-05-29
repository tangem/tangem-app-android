package com.tangem.features.details.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.features.details.entity.UserWalletListUM.UserWalletUM
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.details.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@ComponentScoped
internal class UserWalletListModel @Inject constructor(
    private val shouldSaveUserWalletsUseCase: ShouldSaveUserWalletsUseCase,
    private val userWalletSaver: UserWalletSaver,
    private val userWalletsFetcher: UserWalletsFetcher,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)

    val userWallets: SharedFlow<ImmutableList<UserWalletUM>> = userWalletsFetcher
        .userWallets
        .share()

    val shouldSaveUserWallets: SharedFlow<Boolean> = shouldSaveUserWalletsUseCase()
        .distinctUntilChanged()
        .share()

    fun addUserWallet() = withProgress(isWalletSavingInProgress) {
        userWalletSaver.scanAndSaveUserWallet()
    }
}
