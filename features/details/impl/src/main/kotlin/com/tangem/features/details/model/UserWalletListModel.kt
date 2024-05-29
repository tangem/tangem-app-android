package com.tangem.features.details.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.wallets.usecase.ShouldSaveUserWalletsUseCase
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.utils.UserWalletSaver
import com.tangem.features.details.utils.UserWalletsReceiver
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
    private val userWalletsReceiver: UserWalletsReceiver,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val isWalletSavingInProgress: MutableStateFlow<Boolean> = MutableStateFlow(value = false)

    val userWallets: SharedFlow<ImmutableList<UserWalletListUM.UserWalletUM>> = userWalletsReceiver.userWallets.share()

    val shouldSaveUserWallets: SharedFlow<Boolean> = shouldSaveUserWalletsUseCase()
        .distinctUntilChanged()
        .share()

    fun addUserWallet() = withProgress(isWalletSavingInProgress) {
        val maybeError = userWalletSaver.saveUserWallet().leftOrNull()

        if (maybeError != null) {
            messageSender.send(SnackbarMessage(maybeError))
        }
    }
}
