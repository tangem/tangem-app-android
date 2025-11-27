package com.tangem.features.hotwallet.createhardwarewallet

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic.SignedIn
import com.tangem.core.analytics.models.Basic.SignedIn.SignInType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.analytics.ParamCardCurrencyConverter
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.hotwallet.createhardwarewallet.entity.CreateHardwareWalletUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val HIDE_PROGRESS_DELAY = 400L

@Suppress("LongParameterList")
@ModelScoped
internal class CreateHardwareWalletModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val urlOpener: UrlOpener,
    private val settingsRepository: SettingsRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val uiMessageSender: UiMessageSender,
    private val scanCardProcessor: ScanCardProcessor,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    val uiState: StateFlow<CreateHardwareWalletUM>
        field = MutableStateFlow(
            CreateHardwareWalletUM(
                onBackClick = { router.pop() },
                onBuyTangemWalletClick = ::onBuyTangemWalletClick,
                onScanDeviceClick = ::onScanDeviceClick,
            ),
        )

    init {
        analyticsEventHandler.send(WalletSettingsAnalyticEvents.CreateWalletScreenOpened)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun onBuyTangemWalletClick() {
        modelScope.launch {
            generateBuyTangemCardLinkUseCase.invoke().let { urlOpener.openUrl(it) }
        }
    }

    private fun onScanDeviceClick() {
        scanCard()
    }

    private fun scanCard() {
        modelScope.launch {
            setLoading(true)

            val shouldSaveAccessCodes = settingsRepository.shouldSaveAccessCodes()
            cardSdkConfigRepository.setAccessCodeRequestPolicy(
                isBiometricsRequestPolicy = shouldSaveAccessCodes,
            )

            val analyticsSource = AnalyticsParam.ScreensSources.Intro

            scanCardProcessor.scan(
                analyticsSource = analyticsSource,
                onProgressStateChange = { showProgress ->
                    if (!showProgress) {
                        delay(HIDE_PROGRESS_DELAY)
                        setLoading(false)
                    } else {
                        setLoading(true)
                    }
                },
                onFailure = { error ->
                    handleScanError(error)
                    delay(HIDE_PROGRESS_DELAY)
                    setLoading(false)
                },
                onSuccess = { scanResponse ->
                    proceedWithScanResponse(scanResponse)
                },
            )
        }
    }

    private suspend fun proceedWithScanResponse(scanResponse: ScanResponse) {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build()

        if (userWallet == null) {
            Timber.e("User wallet not created")
            setLoading(false)
            return
        }

        saveWalletUseCase(userWallet = userWallet).fold(
            ifLeft = {
                delay(HIDE_PROGRESS_DELAY)
                setLoading(false)
                when (it) {
                    is SaveWalletError.DataError -> Timber.e(it.toString(), "Unable to save user wallet")
                    is SaveWalletError.WalletAlreadySaved -> {
                        userWalletsListRepository.unlock(
                            userWalletId = userWallet.walletId,
                            unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(scanResponse),
                        ).onRight {
                            router.replaceAll(AppRoute.Wallet)
                        }
                    }
                }
            },
            ifRight = {
                setLoading(false)
                sendSignedInCardAnalyticsEvent(scanResponse = scanResponse, isImported = userWallet.isImported)
                router.replaceAll(AppRoute.Wallet)
            },
        )
    }

    private suspend fun sendSignedInCardAnalyticsEvent(scanResponse: ScanResponse, isImported: Boolean) {
        val currency = ParamCardCurrencyConverter().convert(value = scanResponse.cardTypesResolver)
        if (currency != null) {
            analyticsEventHandler.send(
                SignedIn(
                    currency = currency,
                    batch = scanResponse.card.batchId,
                    signInType = SignInType.Card,
                    walletsCount = userWalletsListRepository.userWalletsSync().size.toString(),
                    isImported = isImported,
                    hasBackup = scanResponse.card.backupStatus?.isActive,
                ),
            )
        }
    }

    private fun setLoading(isLoading: Boolean) {
        uiState.update { it.copy(isScanInProgress = isLoading) }
    }

    private fun handleScanError(error: TangemError) {
        when (error) {
            is TangemSdkError.NfcFeatureIsUnavailable -> handleNfcFeatureUnavailable()
            is TangemSdkError -> Timber.e(error, "Scan error occurred")
            else -> Timber.e(error, "Error happened")
        }
    }

    private fun handleNfcFeatureUnavailable() {
        uiMessageSender.send(
            message = DialogMessage(
                message = resourceReference(R.string.nfc_error_unavailable),
                title = resourceReference(id = R.string.common_error),
            ),
        )
    }
}