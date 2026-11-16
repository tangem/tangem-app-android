package com.tangem.features.hotwallet.createmobilewallet

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
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
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletCreationNotSupportedDialog
import com.tangem.core.ui.message.dialog.Dialogs.hotWalletsLimitReachedDialog
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.wallets.analytics.Settings.NoticeMobileWalletLimit
import com.tangem.domain.wallets.models.CreateHotWalletError
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.hotwallet.CreateMobileWalletComponent
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.createmobilewallet.entity.CreateMobileWalletUM
import com.tangem.features.hotwallet.createmobilewallet.importoptions.ImportOptionsBottomSheetConfig
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class CreateMobileWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val createHotWalletUseCase: CreateHotWalletUseCase,
    private val router: Router,
    private val trackingContextProxy: TrackingContextProxy,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val uiMessageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appsFlyerRepository: AppsFlyerRepository,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) : Model() {

    private val params: CreateMobileWalletComponent.Params = paramsContainer.require()

    val importOptionsBottomSheetNavigation = SlotNavigation<ImportOptionsBottomSheetConfig>()

    internal val uiState: StateFlow<CreateMobileWalletUM>
        field = MutableStateFlow(
            CreateMobileWalletUM(
                onBackClick = { router.pop() },
                onImportClick = ::onImportClick,
                onCreateClick = ::onCreateClick,
                isCreateButtonLoading = false,
                onTermsClick = { router.push(AppRoute.Disclaimer(isTosAccepted = true)) },
            ),
        )

    init {
        trackingContextProxy.addHotWalletContext()
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.Onboarding.Started(source = params.source),
        )
        analyticsEventHandler.send(
            event = OnboardingAnalyticsEvent.SeedPhrase.CreateMobileScreenOpened(source = params.source),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingContextProxy.removeContext()
    }

    private fun onImportClick() {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.SeedPhrase.ButtonImportWallet())
        checkHotWalletCreationSupported(notSupported = { return })

        if (hotWalletFeatureToggles.isGoogleDriveBackupEnabled) {
            analyticsEventHandler.send(
                event = OnboardingAnalyticsEvent.Backup.ImportWalletRequest(
                    cloudBackup = AnalyticsParam.CloudBackupAvailability.Available,
                ),
            )
            importOptionsBottomSheetNavigation.activate(ImportOptionsBottomSheetConfig)
        } else {
            router.push(AppRoute.AddExistingWallet())
        }
    }

    fun onImportRecoveryPhrase() {
        importOptionsBottomSheetNavigation.dismiss()
        router.push(AppRoute.AddExistingWallet(mode = AppRoute.AddExistingWallet.Mode.RecoveryPhrase))
    }

    fun onImportCloudBackupsResolved() {
        importOptionsBottomSheetNavigation.dismiss()
        router.push(AppRoute.AddExistingWallet(mode = AppRoute.AddExistingWallet.Mode.CloudRestore))
    }

    fun onImportBottomSheetDismiss() {
        importOptionsBottomSheetNavigation.dismiss()
    }

    private fun onCreateClick() {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.CreateWallet.ButtonCreateWallet())
        checkHotWalletCreationSupported(notSupported = { return })

        modelScope.launch {
            uiState.update {
                it.copy(isCreateButtonLoading = true)
            }

            createHotWalletUseCase(auth = HotAuth.NoAuth, mnemonicType = MnemonicType.Words12).onRight {
                analyticsEventHandler.send(
                    OnboardingAnalyticsEvent.Onboarding.Finished(source = params.source),
                )
                analyticsEventHandler.send(
                    event = OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                        source = params.source,
                        creationType = AnalyticsParam.WalletCreationType.NewSeed,
                        seedPhraseLength = SEED_PHRASE_LENGTH,
                        passPhraseState = AnalyticsParam.EmptyFull.Empty,
                        referralId = readReferralId(),
                    ),
                )

                router.replaceAll(AppRoute.Wallet)
            }.onLeft { error ->
                val cause = (error as? CreateHotWalletError.Failed)?.cause
                TangemLogger.e("Unable to create the mobile wallet: $error", cause)

                uiState.update { it.copy(isCreateButtonLoading = false) }
                val message = when (error) {
                    CreateHotWalletError.LimitReached -> {
                        analyticsEventHandler.send(
                            NoticeMobileWalletLimit(
                                type = NoticeMobileWalletLimit.Type.Create,
                                limit = NoticeMobileWalletLimit.Limit.MobileWallets,
                            ),
                        )
                        hotWalletsLimitReachedDialog()
                    }
                    is CreateHotWalletError.Failed -> DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.common_unknown_error),
                    )
                }
                uiMessageSender.send(message)
            }
        }
    }

    private suspend fun readReferralId(): String? =
        runSuspendCatching { appsFlyerRepository.getConversionData()?.refcode }
            .onFailure { error -> TangemLogger.e("Failed to read the referral for the wallet created event", error) }
            .getOrNull()

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