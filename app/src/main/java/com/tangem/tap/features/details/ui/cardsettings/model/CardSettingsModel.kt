package com.tangem.tap.features.details.ui.cardsettings.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.common.util.getBackupCardsCount
import com.tangem.domain.card.models.ScanConfig
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.ui.cardsettings.CardInfo
import com.tangem.tap.features.details.ui.cardsettings.CardSettingsScreenState
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.common.utils.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addIf
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class CardSettingsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val scanCardProcessor: ScanCardProcessor,
    private val tangemSdkManager: TangemSdkManager,
    private val cardSettingsInteractor: CardSettingsInteractor,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val uiMessageSender: UiMessageSender,
    private val router: Router,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<CardSettingsComponent.Params>()

    private var isBiometricsRequestPolicyPrevious: Boolean = false

    private var cardIdDisplayFormatPrevious: CardIdDisplayFormat? = null

    private val userWalletId = params.userWalletId

    val screenState: MutableStateFlow<CardSettingsScreenState> = MutableStateFlow(getInitialState())

    init {
        overrideSdkConfig()

        cardSettingsInteractor.scannedScanResponse
            .filterNotNull()
            .onEach(::updateCardDetails)
            .launchIn(modelScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset card scanned data
        cardSettingsInteractor.clear()
        // Restore the previous values of SDK config overridden by this screen
        cardSdkConfigRepository.restoreAccessCodeRequestPolicy(isBiometricsRequestPolicyPrevious)
        cardIdDisplayFormatPrevious?.let(cardSdkConfigRepository::restoreCardIdDisplayFormat)
    }

    private fun overrideSdkConfig() {
        runBlocking {
            // !!!IMPORTANT!!!: Do not forget to restore the previous values in onDestroy() method
            isBiometricsRequestPolicyPrevious = cardSdkConfigRepository.isBiometricsRequestPolicy
            cardIdDisplayFormatPrevious = cardSdkConfigRepository.cardIdDisplayFormat

            val userWallet = getUserWalletUseCase(userWalletId)
                .getOrElse { error("User wallet $userWalletId not found") }
                .requireColdWallet()

            cardSdkConfigRepository.isBiometricsRequestPolicy = userWallet.scanResponse.card.isAccessCodeSet &&
                settingsRepository.shouldSaveAccessCodes()

            // The card of this wallet isn't necessarily the selected one, whose format is set globally
            tangemSdkManager.changeDisplayedCardIdNumbersCount(userWallet.scanResponse)
        }
    }

    private fun getInitialState() = CardSettingsScreenState(
        cardDetails = null,
        onElementClick = ::handleClickingItem,
        onScanCardClick = ::scanCard,
        onBackClick = ::onBackClick,
    )

    private fun scanCard() = modelScope.launch {
        scanCardProcessor.scan(
            config = ScanConfig(
                analyticsSource = com.tangem.core.analytics.models.AnalyticsParam.ScreensSources.CardSettings,
                shouldConfirmWalletOwnership = false,
                canRequestAccessCodeFromRepository = true,
                shouldUpdateCardIdDisplayFormat = false,
                shouldTrackScanFails = false,
                shouldLaunchOnboarding = false,
                callback = object : ScanConfig.Callback {
                    override suspend fun onSuccess(scanResponse: ScanResponse) {
                        val scannedUserWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
                        if (userWalletId == scannedUserWalletId || scannedUserWalletId == null) {
                            cardSettingsInteractor.initialize(scanResponse)
                        } else {
                            uiMessageSender.send(Dialogs.wrongWalletTapped())
                        }
                    }
                },
            ),
        )
    }

    private fun updateCardDetails(scanResponse: ScanResponse) {
        val card = scanResponse.card
        val cardTypesResolver = scanResponse.cardTypesResolver
        val cardId = cardTypesResolver.getCardId()

        val currentSecurityOption = getCurrentSecurityOption(card)
        val allowedSecurityOptions = getAllowedSecurityOptions(
            card = card,
            cardTypesResolver = cardTypesResolver,
            currentSecurityOption = currentSecurityOption,
        )
        val isResetCardAllowed = isResetToFactoryAllowedByCard(card, cardTypesResolver)

        val cardDetails: List<CardInfo> = buildList {
            add(CardInfo.CardId(cardId))
            add(CardInfo.Issuer(card.issuer.name))

            addIf(
                condition = !cardTypesResolver.isTangemTwins(),
                element = CardInfo.SignedHashes(card.signedHashesCount().toString()),
            )

            add(
                CardInfo.SecurityMode(
                    securityOption = currentSecurityOption,
                    clickable = allowedSecurityOptions.size > 1,
                ),
            )

            addIf(
                condition = card.backupStatus?.isActive == true && card.isAccessCodeSet,
                element = CardInfo.ChangeAccessCode,
            )

            addIf(
                condition = isAccessCodeRecoveryAllowed(cardTypesResolver),
                element = CardInfo.AccessCodeRecovery(isAccessCodeRecoveryEnabled(cardTypesResolver, card)),
            )

            addIf(isResetCardAllowed) {
                CardInfo.ResetToFactorySettings(
                    description = getResetToFactoryDescription(
                        isActiveBackupStatus = card.backupStatus?.isActive == true,
                        typesResolver = cardTypesResolver,
                    ),
                )
            }
        }

        screenState.update { state ->
            state.copy(cardDetails = cardDetails)
        }
    }

    private fun CardDTO.signedHashesCount(): Int = wallets.sumOf { it.totalSignedHashes ?: 0 }

    private fun handleClickingItem(item: CardInfo) {
        when (item) {
            is CardInfo.ChangeAccessCode -> {
                analyticsEventHandler.send(
                    Settings.CardSettings.ButtonChangeUserCode(AnalyticsParam.UserCode.AccessCode),
                )
                changeAccessCode()
            }
            is CardInfo.ResetToFactorySettings -> {
                analyticsEventHandler.send(Settings.CardSettings.ButtonFactoryReset())
                resetWalletToFactorySettings()
            }
            is CardInfo.SecurityMode -> {
                analyticsEventHandler.send(Settings.CardSettings.ButtonChangeSecurityMode())
                router.push(route = AppRoute.DetailsSecurity(userWalletId))
            }
            is CardInfo.AccessCodeRecovery -> {
                router.push(AppRoute.AccessCodeRecovery)
            }
            else -> {}
        }
    }

    private fun resetWalletToFactorySettings() {
        val scanResponse = requireNotNull(cardSettingsInteractor.scannedScanResponse.value) {
            "Impossible to reset card if ScanResponse is null"
        }

        if (scanResponse.cardTypesResolver.isTangemTwins()) {
            router.push(
                AppRoute.Onboarding(
                    scanResponse = scanResponse,
                    mode = AppRoute.Onboarding.Mode.RecreateWalletTwin,
                ),
            )
        } else {
            val card = scanResponse.card

            modelScope.launch {
                val hasTangemPay = onboardingRepository.hasTangemPayInWallet(userWalletId).getOrNull() == true
                router.push(
                    route = AppRoute.ResetToFactory(
                        userWalletId = userWalletId,
                        cardId = card.cardId,
                        isActiveBackupStatus = card.backupStatus?.isActive == true,
                        backupCardsCount = scanResponse.getBackupCardsCount() ?: 0,
                        hasTangemPay = hasTangemPay,
                    ),
                )
            }
        }
    }

    private fun changeAccessCode() = modelScope.launch {
        val scanResponse = requireNotNull(cardSettingsInteractor.scannedScanResponse.value) { "Scan response is null" }

        when (val result = tangemSdkManager.setAccessCode(scanResponse.card.cardId)) {
            is CompletionResult.Success -> analyticsEventHandler.send(Settings.CardSettings.UserCodeChanged())
            is CompletionResult.Failure -> {
                TangemLogger.e("Failed to change access code: ${result.error}")
            }
        }
    }

    private fun isResetToFactoryAllowedByCard(card: CardDTO, cardTypesResolver: CardTypesResolver): Boolean {
        val hasPermanentWallet = card.wallets.any { it.settings.isPermanent }
        val isNotAllowed = hasPermanentWallet || cardTypesResolver.isStart2Coin()
        return !isNotAllowed
    }

    private fun onBackClick() {
        cardSettingsInteractor.clear()
        router.pop()
    }
}