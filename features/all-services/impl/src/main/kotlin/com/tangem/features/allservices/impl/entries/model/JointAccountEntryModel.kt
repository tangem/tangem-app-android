package com.tangem.features.allservices.impl.entries.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.features.jointaccount.JointAccountFeatureToggles
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Opens joint account creation without a wallet: the flow asks for one after its own intro. */
@ModelScoped
internal class JointAccountEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
    userWalletsListRepository: UserWalletsListRepository,
    private val jointAccountFeatureToggles: JointAccountFeatureToggles,
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
        val source = AppRoute.JointAccountCreation.Source.AllServices
        analyticsEventHandler.send(JointAccountAnalyticsEvent.ButtonCreateJointAccount(source = source))
        router.push(AppRoute.JointAccountCreation(userWalletId = null, source = source))
    }

    private fun isOfferable(wallets: List<UserWallet>?): Boolean =
        jointAccountFeatureToggles.isJointAccountEnabled && wallets.orEmpty().any(::canHoldAccounts)

    private fun canHoldAccounts(wallet: UserWallet): Boolean = wallet.isAccountsSupported && !wallet.isLocked
}