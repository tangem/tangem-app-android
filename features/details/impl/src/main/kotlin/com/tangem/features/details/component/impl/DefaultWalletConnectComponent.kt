package com.tangem.features.details.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import arrow.core.getOrElse
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.navigation.AppScreen
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.routing.DetailsRoute
import com.tangem.features.details.ui.WalletConnectBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

internal class DefaultWalletConnectComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: WalletConnectComponent.Params,
    private val checkIsWalletConnectAvailableUseCase: CheckIsWalletConnectAvailableUseCase,
) : WalletConnectComponent, AppComponentContext by context {

    private val userWalletId = params.userWalletId

    override suspend fun checkIsAvailable(): Boolean = checkIsWalletConnectAvailableUseCase(userWalletId).getOrElse {
        Timber.w("Unable to check WalletConnect availability: $it")

        false
    }

    @Composable
    override fun View(modifier: Modifier) {
        WalletConnectBlock(
            modifier = modifier,
            onClick = ::navigateToWalletConnect,
        )
    }

    private fun navigateToWalletConnect() {
        router.push(DetailsRoute.Screen(AppScreen.WalletConnectSessions))
    }

    @AssistedFactory
    interface Factory : WalletConnectComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: WalletConnectComponent.Params,
        ): DefaultWalletConnectComponent
    }
}
