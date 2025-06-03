package com.tangem.features.details.model

import android.content.res.Resources
import arrow.core.getOrElse
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.common.TapWorkarounds.isVisa
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.CardInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.entity.SelectEmailFeedbackTypeBS
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
import java.util.Locale
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class DetailsModel @Inject constructor(
    socialsBuilder: SocialsBuilder,
    itemsBuilder: ItemsBuilder,
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
    private val getWalletsUseCase: GetWalletsUseCase,
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
                selectFeedbackEmailTypeBSConfig = TangemBottomSheetConfig.Empty,
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
            val userWallets = getWalletsUseCase.invokeSync()

            val scanResponse = getSelectedWalletSyncUseCase().getOrNull()?.scanResponse
                ?: error("Selected wallet is null")

            val cardInfo = getCardInfoUseCase(scanResponse).getOrNull() ?: return@launch

            val feedbackType = when {
                userWallets.all { it.scanResponse.card.isVisa } -> FeedbackEmailType.Visa.DirectUserRequest(cardInfo)
                userWallets.all { it.scanResponse.card.isVisa.not() } -> FeedbackEmailType.DirectUserRequest(cardInfo)
                else -> {
                    showFeedbackEmailTypeOptionBS(cardInfo)
                    return@launch
                }
            }

            sendFeedbackEmailUseCase(feedbackType)
        }
    }

    private fun showFeedbackEmailTypeOptionBS(selectedCardInfo: CardInfo) {
        state.update {
            it.copy(
                selectFeedbackEmailTypeBSConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {
                        state.update {
                            it.copy(
                                selectFeedbackEmailTypeBSConfig =
                                it.selectFeedbackEmailTypeBSConfig.copy(isShown = false),
                            )
                        }
                    },
                    content = SelectEmailFeedbackTypeBS(
                        onOptionClick = { option ->
                            onEmailFeedbackTypeOptionSelected(
                                selectedCardInfo = selectedCardInfo,
                                option = option,
                            )

                            state.update {
                                it.copy(
                                    selectFeedbackEmailTypeBSConfig =
                                    it.selectFeedbackEmailTypeBSConfig.copy(isShown = false),
                                )
                            }
                        },
                    ),
                ),
            )
        }
    }

    private fun onEmailFeedbackTypeOptionSelected(
        selectedCardInfo: CardInfo,
        option: SelectEmailFeedbackTypeBS.Option,
    ) {
        modelScope.launch {
            val feedbackType = when (option) {
                SelectEmailFeedbackTypeBS.Option.General -> {
                    if (selectedCardInfo.isVisa.not()) {
                        FeedbackEmailType.DirectUserRequest(selectedCardInfo)
                    } else {
                        val scanResponse = getWalletsUseCase.invokeSync()
                            .firstOrNull { it.scanResponse.card.isVisa.not() }?.scanResponse ?: return@launch
                        val cardInfo = getCardInfoUseCase(scanResponse).getOrNull() ?: return@launch
                        FeedbackEmailType.DirectUserRequest(cardInfo)
                    }
                }
                SelectEmailFeedbackTypeBS.Option.Visa -> {
                    if (selectedCardInfo.isVisa) {
                        FeedbackEmailType.Visa.DirectUserRequest(selectedCardInfo)
                    } else {
                        val scanResponse = getWalletsUseCase.invokeSync()
                            .firstOrNull { it.scanResponse.card.isVisa }?.scanResponse ?: return@launch
                        val cardInfo = getCardInfoUseCase(scanResponse).getOrNull() ?: return@launch
                        FeedbackEmailType.Visa.DirectUserRequest(cardInfo)
                    }
                }
            }

            sendFeedbackEmailUseCase(feedbackType)
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
        val SYSTEM_LANGUAGE = runCatching { Resources.getSystem().configuration.locales[0].language }.getOrElse { "" }
        val APP_LANGUAGE = Locale.getDefault().language
        val UTM_MARKS = "utm_source=tangem-app" +
            "&utm_medium=app" +
            "&utm_campaign=users-$SYSTEM_LANGUAGE" +
            "&utm_content=devicelang-$APP_LANGUAGE"

        val BUY_TANGEM_URL = "https://buy.tangem.com/?$UTM_MARKS"
    }
}