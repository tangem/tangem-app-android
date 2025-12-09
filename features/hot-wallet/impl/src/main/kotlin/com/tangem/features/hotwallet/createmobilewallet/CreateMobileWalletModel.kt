package com.tangem.features.hotwallet.createmobilewallet

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.wallets.builder.HotUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.wallets.usecase.SyncWalletWithRemoteUseCase
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class CreateMobileWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val hotUserWalletBuilderFactory: HotUserWalletBuilder.Factory,
    private val saveUserWalletUseCase: SaveWalletUseCase,
    private val syncWalletWithRemoteUseCase: SyncWalletWithRemoteUseCase,
    private val router: Router,
    private val tangemHotSdk: TangemHotSdk,
    private val trackingContextProxy: TrackingContextProxy,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params: CreateMobileWalletComponent.Params = paramsContainer.require()

    internal val uiState: StateFlow<CreateMobileWalletUM>
        field = MutableStateFlow(
            CreateMobileWalletUM(
                onBackClick = { router.pop() },
                onImportClick = ::onImportClick,
                onCreateClick = ::onCreateClick,
                createButtonLoading = false,
            ),
        )

    init {
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(OnboardingAnalyticsEvent.Onboarding.Started(source = params.source))
        analyticsEventHandler.send(OnboardingAnalyticsEvent.SeedPhrase.CreateMobileScreenOpened)
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingContextProxy.removeContext()
    }

    private fun onImportClick() {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.SeedPhrase.ButtonImportWallet)
        checkHotWalletCreationSupported(notSupported = { return })
        router.push(AppRoute.AddExistingWallet)
    }

    private fun onCreateClick() {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.CreateWallet.ButtonCreateWallet)
        checkHotWalletCreationSupported(notSupported = { return })

        modelScope.launch {
            uiState.update {
                it.copy(createButtonLoading = true)
            }

            runSuspendCatching {
                val hotWalletId = tangemHotSdk.generateWallet(HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
                val hotUserWalletBuilder = hotUserWalletBuilderFactory.create(hotWalletId)
                val userWallet = hotUserWalletBuilder.build()

                saveUserWalletUseCase(userWallet)

                sendCreationAnalytics()

                launch(dispatchers.main + NonCancellable) {
                    syncWalletWithRemoteUseCase(userWalletId = userWallet.walletId)
                }

                router.replaceAll(AppRoute.Wallet)
            }.onFailure { throwable ->
                Timber.e(throwable)

                uiState.update { it.copy(createButtonLoading = false) }
            }
        }
    }

    private fun sendCreationAnalytics() {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.Onboarding.Finished(source = params.source))
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                source = params.source,
                creationType = OnboardingAnalyticsEvent.CreateWallet.WalletCreationType.NewSeed,
                seedPhraseLength = SEED_PHRASE_LENGTH,
                passPhraseState = AnalyticsParam.EmptyFull.Empty,
            ),
        )
        analyticsEventHandler.send(
            OnboardingAnalyticsEvent.CreateWallet.AppsFlyerWalletCreatedSuccessfully(
                source = params.source,
                creationType = OnboardingAnalyticsEvent.CreateWallet.WalletCreationType.NewSeed,
                seedPhraseLength = SEED_PHRASE_LENGTH,
                passPhraseState = AnalyticsParam.EmptyFull.Empty,
            )
        )
    }

    private inline fun checkHotWalletCreationSupported(notSupported: () -> Unit) {
        if (!isHotWalletCreationSupported()) {
            uiMessageSender.send(
                hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            notSupported()
        }
    }

    companion object {
        private const val SEED_PHRASE_LENGTH = 12
    }
}