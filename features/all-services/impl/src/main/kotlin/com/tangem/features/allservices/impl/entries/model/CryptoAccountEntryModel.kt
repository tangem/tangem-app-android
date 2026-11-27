package com.tangem.features.allservices.impl.entries.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Opens the crypto account intro, which asks for the wallet itself. */
@ModelScoped
internal class CryptoAccountEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    /** Seeded from the repository's current value, so the row is right on the first frame. */
    val isVisible: StateFlow<Boolean> = userWalletsListRepository.userWallets
        .map(::isOfferable)
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = isOfferable(userWalletsListRepository.userWallets.value),
        )

    fun onClick() {
        router.push(AppRoute.CryptoAccountIntro)
    }

    private fun isOfferable(wallets: List<UserWallet>?): Boolean =
        wallets.orEmpty().any { it.isAccountsSupported && !it.isLocked }
}