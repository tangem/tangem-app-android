package com.tangem.features.details.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.version.AppVersionProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class DetailsModel @Inject constructor(
    private val socialsBuilder: SocialsBuilder,
    private val itemsBuilder: ItemsBuilder,
    private val appVersionProvider: AppVersionProvider,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val items: MutableSharedFlow<ImmutableList<DetailsItemUM>> = MutableSharedFlow(replay = 1)

    val state: MutableStateFlow<DetailsUM> = MutableStateFlow(
        value = DetailsUM(
            items = persistentListOf(),
            footer = DetailsFooterUM(
                socials = socialsBuilder.buildAll(),
                appVersion = getAppVersion(),
            ),
            popBack = router::pop,
        ),
    )

    init {
        items
            .onEach(::updateState)
            .launchIn(modelScope)
    }

    fun provideChildren(
        walletConnectComponent: WalletConnectComponent,
        userWalletListComponent: UserWalletListComponent,
    ) = modelScope.launch {
        items.emit(itemsBuilder.buldAll(walletConnectComponent, userWalletListComponent))
    }

    private suspend fun updateState(items: ImmutableList<DetailsItemUM>) {
        state.update { prevState ->
            prevState.copy(
                items = items,
            )
        }
    }

    private fun getAppVersion(): String = "${appVersionProvider.versionName} (${appVersionProvider.versionCode})"
}
