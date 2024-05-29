package com.tangem.features.details.component.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.ui.DetailsScreen
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.utils.version.AppVersionProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class DefaultDetailsComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: DetailsComponent.Params,
    private val appVersionProvider: AppVersionProvider,
    walletConnectComponentFactory: WalletConnectComponent.Factory,
    userWalletListComponentFactory: UserWalletListComponent.Factory,
) : DetailsComponent, AppComponentContext by context {

    private val state: MutableStateFlow<DetailsUM?> = MutableStateFlow(value = null)

    private val walletConnectComponent = walletConnectComponentFactory.create(
        context = child(key = "wallet_connect"),
        params = WalletConnectComponent.Params(userWalletId = params.selectedUserWalletId),
    )
    private val userWalletListComponent = userWalletListComponentFactory.create(
        context = child(key = "user_wallet_list"),
    )

    private val itemsBuffer = ItemsBuilder(
        walletConnectComponent = walletConnectComponent,
        userWalletListComponent = userWalletListComponent,
        router = router,
    )
    private val socialsBuilder = SocialsBuilder(
        router = router,
    )

    override val snackbarHostState: SnackbarHostState = SnackbarHostState()

    init {
        initState()

        doOnDestroy {
            messageSender.send(SnackbarMessage(stringReference("DetailsComponent destroyed")))
        }
    }

    private fun initState() = componentScope.launch(dispatchers.io) {
        state.value = DetailsUM(
            items = itemsBuffer.buldAll(),
            footer = DetailsFooterUM(
                socials = socialsBuilder.buildAll(),
                appVersion = getAppVersion(),
            ),
            popBack = router::pop,
        )
    }

    @Composable
    override fun View(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()

        DetailsScreen(
            modifier = modifier,
            state = state ?: return,
            snackbarHostState = snackbarHostState,
        )
    }

    private fun getAppVersion(): String = "${appVersionProvider.versionName} (${appVersionProvider.versionCode})"

    @AssistedFactory
    interface Factory : DetailsComponent.Factory {
        override fun create(context: AppComponentContext, params: DetailsComponent.Params): DefaultDetailsComponent
    }
}
