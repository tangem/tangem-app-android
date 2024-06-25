package com.tangem.features.details.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.feedback.FeedbackManager
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ComponentScoped
@Suppress("LongParameterList")
internal class DetailsModel @Inject constructor(
    private val socialsBuilder: SocialsBuilder,
    private val itemsBuilder: ItemsBuilder,
    private val appVersionProvider: AppVersionProvider,
    private val checkIsWalletConnectAvailableUseCase: CheckIsWalletConnectAvailableUseCase,
    private val router: Router,
    private val paramsContainer: ParamsContainer,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val feedbackManager: FeedbackManager,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params: DetailsComponent.Params = paramsContainer.require()

    private val items: MutableStateFlow<ImmutableList<DetailsItemUM>> = MutableStateFlow(value = persistentListOf())

    val state: MutableStateFlow<DetailsUM> = MutableStateFlow(
        value = DetailsUM(
            items = items.value,
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

        checkWalletConnectAvailability()
    }

    private fun checkWalletConnectAvailability() = modelScope.launch {
        val isWalletConnectAvailable = checkIsWalletConnectAvailableUseCase(params.userWalletId).getOrElse {
            Timber.w("Unable to check WalletConnect availability: $it")

            false
        }

        items.value = itemsBuilder.buildAll(
            isWalletConnectAvailable = isWalletConnectAvailable,
            onSupportClick = ::sendFeedback,
        )
    }

    private fun sendFeedback() {
        modelScope.launch {
            val scanResponse = getSelectedWalletSyncUseCase().getOrNull()?.scanResponse
                ?: error("Selected wallet is null")

            val cardInfo = getCardInfoUseCase(scanResponse = scanResponse).getOrNull()
                ?: error("CardInfo must be not null")

            feedbackManager.sendEmail(type = FeedbackEmailType.DirectUserRequest(cardInfo))
        }
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