package com.tangem.feature.onboarding.legacy.redux

import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.routing.AppRouter
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.feature.onboarding.legacy.bridge.GlobalActionBridge
import com.tangem.feature.onboarding.legacy.bridge.NotificationsBridge
import com.tangem.feature.onboarding.legacy.bridge.OnboardingCommonBridge
import com.tangem.feature.onboarding.legacy.bridge.WalletManagerBridge
import com.tangem.sdk.api.TangemSdkManager
import org.rekotlin.StateType
import org.rekotlin.Store

data class DaggerGraphState(
    val isInitialized: Boolean = false,
    val bridges: Bridges? = null,
    val tangemSdkManager: TangemSdkManager? = null,
    val blockchainSDKFactory: BlockchainSDKFactory? = null,
    val appRouter: AppRouter? = null,
    val cardRepository: CardRepository? = null,
    val isDemoCardUseCase: IsDemoCardUseCase? = null,
    val settingsRepository: SettingsRepository? = null,
    val walletsRepository: WalletsRepository? = null,
) : StateType

data class Bridges(
    val globalActionBridge: GlobalActionBridge,
    val walletManagerBridge: WalletManagerBridge,
    val onboardingCommonBridge: OnboardingCommonBridge,
    val notificationsBridge: NotificationsBridge,
)

internal inline fun <reified T> Store<OnboardingReduxState>.inject(getDependency: DaggerGraphState.() -> T?): T {
    return requireNotNull(state.daggerGraphState.getDependency()) {
        "${T::class.simpleName} isn't initialized "
    }
}
