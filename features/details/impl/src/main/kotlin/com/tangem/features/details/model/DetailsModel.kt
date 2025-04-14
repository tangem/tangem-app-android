package com.tangem.features.details.model

import arrow.core.getOrElse
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.version.AppVersionProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class DetailsModel @Inject constructor(
    socialsBuilder: SocialsBuilder,
    private val itemsBuilder: ItemsBuilder,
    private val appVersionProvider: AppVersionProvider,
    private val checkIsWalletConnectAvailableUseCase: CheckIsWalletConnectAvailableUseCase,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val appInstanceIdProvider: AppInstanceIdProvider,
    paramsContainer: ParamsContainer,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val appStateHolder: ReduxStateHolder,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: DetailsComponent.Params = paramsContainer.require()

    private val items: MutableStateFlow<ImmutableList<DetailsItemUM>>

    val state: MutableStateFlow<DetailsUM>

    init {
        // Use to save compatibility with screens that using Redux states
        bootstrapScreenState()

        val isWalletConnectAvailable = runBlocking {
            // danger region, this works immediately, but will be refactored later with WC
            checkIsWalletConnectAvailableUseCase(params.userWalletId).getOrElse {
                Timber.w("Unable to check WalletConnect availability: $it")

                false
            }
        }

        items = MutableStateFlow(
            itemsBuilder.buildAll(
                isWalletConnectAvailable = isWalletConnectAvailable,
                onSupportClick = ::sendFeedback,
                onBuyClick = ::onBuyClick,
            ),
        )

        state = MutableStateFlow(
            value = DetailsUM(
                items = items.value,
                footer = DetailsFooterUM(
                    socials = socialsBuilder.buildAll(),
                    appVersion = getAppVersion(),
                ),
                popBack = router::pop,
            ),
        )

        items
            .onEach(::updateState)
            .launchIn(modelScope)
    }

    private fun bootstrapScreenState() {
        appStateHolder.dispatch(LegacyAction.PrepareDetailsScreen)
    }

    private fun sendFeedback() {
        modelScope.launch {
            val scanResponse = getSelectedWalletSyncUseCase().getOrNull()?.scanResponse
                ?: error("Selected wallet is null")

            val cardInfo = getCardInfoUseCase(scanResponse).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(cardInfo = cardInfo))
        }
    }

    private fun onBuyClick() {
        modelScope.launch {
            urlOpener.openUrl(buildBuyLink())
        }
    }

    private fun updateState(items: ImmutableList<DetailsItemUM>) {
        state.update { prevState ->
            prevState.copy(items = items)
        }
    }

    private fun getAppVersion(): String = "${appVersionProvider.versionName} (${appVersionProvider.versionCode})"

    private suspend fun buildBuyLink(): String {
        return appInstanceIdProvider.getAppInstanceId()?.let {
            "$BUY_TANGEM_URL&app_instance_id=$it"
        } ?: BUY_TANGEM_URL
    }

    private companion object {
        const val BUY_TANGEM_URL = "https://buy.tangem.com/?utm_source=tangem&utm_medium=app"
    }
}