package com.tangem.tap.features.details.ui.cardsettings.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.CompletionResult
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.getBackupCardsCount
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.details.ui.cardsettings.CardInfo
import com.tangem.tap.features.details.ui.cardsettings.CardSettingsScreenState
import com.tangem.tap.features.details.ui.cardsettings.api.CardSettingsComponent
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.common.utils.*
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.wallet.R
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
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
) : Model() {

    private val params = paramsContainer.require<CardSettingsComponent.Params>()

    private var previousBiometricsRequestPolicy: Boolean = false

    private val userWalletId = params.userWalletId

    val screenState: MutableStateFlow<CardSettingsScreenState> = MutableStateFlow(getInitialState())

    init {
        updateAccessCodeRequestPolicy()

        cardSettingsInteractor.scannedScanResponse
            .filterNotNull()
            .onEach(::updateCardDetails)
            .launchIn(modelScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Reset card scanned data
        cardSettingsInteractor.clear()
        // Restore the previous value of access code request policy
        cardSdkConfigRepository.isBiometricsRequestPolicy = previousBiometricsRequestPolicy
    }

    private fun updateAccessCodeRequestPolicy() {
        runBlocking {
            // !!!IMPORTANT!!!: Do not forget to restore the previous value in onCleared() method
            previousBiometricsRequestPolicy = cardSdkConfigRepository.isBiometricsRequestPolicy

            val userWallet = getUserWalletUseCase(userWalletId)
                .getOrElse { error("User wallet $userWalletId not found") }

            cardSdkConfigRepository.isBiometricsRequestPolicy =
                userWallet.requireColdWallet().scanResponse.card.isAccessCodeSet && // TODO [REDACTED_TASK_KEY]
                settingsRepository.shouldSaveAccessCodes()
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
            analyticsSource = com.tangem.core.analytics.models.AnalyticsParam.ScreensSources.Settings,
            allowsRequestAccessCodeFromRepository = true,
        )
            .doOnSuccess { scanResponse ->
                val scannedUserWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
                if (userWalletId == scannedUserWalletId || scannedUserWalletId == null) {
                    cardSettingsInteractor.initialize(scanResponse)
                } else {
                    store.dispatchDialogShow(
                        AppDialog.SimpleOkDialogRes(
                            headerId = R.string.common_warning,
                            messageId = R.string.error_wrong_wallet_tapped,
                        ),
                    )
                }
            }
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

        val cardDetails = buildList {
            CardInfo.CardId(cardId).let(::add)
            CardInfo.Issuer(card.issuer.name).let(::add)

            if (!cardTypesResolver.isTangemTwins()) {
                CardInfo.SignedHashes(card.signedHashesCount().toString()).let(::add)
            }

            CardInfo.SecurityMode(
                currentSecurityOption,
                clickable = allowedSecurityOptions.size > 1,
            ).let(::add)

            if (card.backupStatus?.isActive == true && card.isAccessCodeSet) {
                CardInfo.ChangeAccessCode.let(::add)
            }

            if (isAccessCodeRecoveryAllowed(cardTypesResolver)) {
                CardInfo.AccessCodeRecovery(isAccessCodeRecoveryEnabled(cardTypesResolver, card)).let(::add)
            }

            if (isResetCardAllowed) {
                CardInfo.ResetToFactorySettings(
                    description = getResetToFactoryDescription(
                        isActiveBackupStatus = card.backupStatus?.isActive == true,
                        typesResolver = cardTypesResolver,
                    ),
                ).let(::add)
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
                Analytics.send(Settings.CardSettings.ButtonChangeUserCode(AnalyticsParam.UserCode.AccessCode))
                changeAccessCode()
            }
            is CardInfo.ResetToFactorySettings -> {
                Analytics.send(Settings.CardSettings.ButtonFactoryReset())
                resetWalletToFactorySettings()
            }
            is CardInfo.SecurityMode -> {
                Analytics.send(Settings.CardSettings.ButtonChangeSecurityMode())
                store.dispatchNavigationAction {
                    push(route = AppRoute.DetailsSecurity(userWalletId))
                }
            }
            is CardInfo.AccessCodeRecovery -> {
                store.dispatchNavigationAction { push(AppRoute.AccessCodeRecovery) }
            }
            else -> {}
        }
    }

    private fun resetWalletToFactorySettings() {
        val scanResponse = requireNotNull(cardSettingsInteractor.scannedScanResponse.value) {
            "Impossible to reset card if ScanResponse is null"
        }

        if (scanResponse.cardTypesResolver.isTangemTwins()) {
            store.dispatchNavigationAction {
                push(
                    AppRoute.Onboarding(
                        scanResponse = scanResponse,
                        mode = AppRoute.Onboarding.Mode.RecreateWalletTwin,
                    ),
                )
            }
        } else {
            val card = scanResponse.card

            store.dispatchNavigationAction {
                push(
                    route = AppRoute.ResetToFactory(
                        userWalletId = userWalletId,
                        cardId = card.cardId,
                        isActiveBackupStatus = card.backupStatus?.isActive == true,
                        backupCardsCount = scanResponse.getBackupCardsCount() ?: 0,
                    ),
                )
            }
        }
    }

    private fun changeAccessCode() = modelScope.launch {
        val scanResponse = requireNotNull(cardSettingsInteractor.scannedScanResponse.value) { "Scan response is null" }

        when (val result = tangemSdkManager.setAccessCode(scanResponse.card.cardId)) {
            is CompletionResult.Success -> Analytics.send(Settings.CardSettings.UserCodeChanged())
            is CompletionResult.Failure -> {
                Timber.e("Failed to change access code: ${result.error}")
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
        store.dispatchNavigationAction(AppRouter::pop)
    }
}