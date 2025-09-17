package com.tangem.features.details.model

import android.content.res.Resources
import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.card.common.TapWorkarounds.isVisa
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.repository.FeedbackFeatureToggles
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.models.wallet.UserWallet
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
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val feedbackFeatureToggles: FeedbackFeatureToggles,
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
                isSupportChatAvailable = feedbackFeatureToggles.isUsedeskEnabled,
                userWalletId = params.userWalletId,
                onSupportEmailClick = ::sendFeedback,
                onSupportChatClick = ::openUseDesk,
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

            val selectedUserWallet = getSelectedWalletSyncUseCase().getOrNull()
                ?: error("Selected wallet is null")

            val metaInfo = getWalletMetaInfoUseCase(selectedUserWallet.walletId).getOrNull() ?: return@launch

            val feedbackType = when {
                userWallets.all { it is UserWallet.Cold && it.scanResponse.card.isVisa } ->
                    FeedbackEmailType.Visa.DirectUserRequest(metaInfo)
                userWallets.all { it !is UserWallet.Cold || it.scanResponse.card.isVisa.not() } ->
                    FeedbackEmailType.DirectUserRequest(metaInfo)
                else -> {
                    showFeedbackEmailTypeOptionBS(metaInfo)
                    return@launch
                }
            }

            sendFeedbackEmailUseCase(feedbackType)
        }
    }

    private fun openUseDesk() {
        modelScope.launch {
            val userWallet = getSelectedWalletSyncUseCase().getOrNull() ?: error("Selected wallet is null")
            val metaInfo = getWalletMetaInfoUseCase.invoke(userWallet.walletId).getOrNull() ?: return@launch
            router.push(AppRoute.Usedesk(metaInfo))
        }
    }

    private fun showFeedbackEmailTypeOptionBS(selectedWalletMetaInfo: WalletMetaInfo) {
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
                                selectedWalletMetaInfo = selectedWalletMetaInfo,
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
        selectedWalletMetaInfo: WalletMetaInfo,
        option: SelectEmailFeedbackTypeBS.Option,
    ) {
        modelScope.launch {
            val feedbackType = when (option) {
                SelectEmailFeedbackTypeBS.Option.General -> {
                    if (selectedWalletMetaInfo.isVisa == false) {
                        FeedbackEmailType.DirectUserRequest(selectedWalletMetaInfo)
                    } else {
                        val userWallet = getWalletsUseCase.invokeSync()
                            .firstOrNull {
                                it is UserWallet.Hot || it is UserWallet.Cold && it.scanResponse.card.isVisa.not()
                            } ?: return@launch

                        val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
                        FeedbackEmailType.DirectUserRequest(metaInfo)
                    }
                }
                SelectEmailFeedbackTypeBS.Option.Visa -> {
                    if (selectedWalletMetaInfo.isVisa == true) {
                        FeedbackEmailType.Visa.DirectUserRequest(selectedWalletMetaInfo)
                    } else {
                        val userWallet = getWalletsUseCase.invokeSync()
                            .firstOrNull { it is UserWallet.Cold && it.scanResponse.card.isVisa }
                            ?: return@launch
                        val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
                        FeedbackEmailType.Visa.DirectUserRequest(metaInfo)
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