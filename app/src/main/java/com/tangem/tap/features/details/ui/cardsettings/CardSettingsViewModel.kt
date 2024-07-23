package com.tangem.tap.features.details.ui.cardsettings

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.CompletionResult
import com.tangem.common.doOnSuccess
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.core.analytics.Analytics
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.domain.extensions.signedHashesCount
import com.tangem.tap.domain.sdk.TangemSdkManager
import com.tangem.tap.features.details.ui.common.utils.*
import com.tangem.tap.store
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class CardSettingsViewModel @Inject constructor(
    private val scanCardProcessor: ScanCardProcessor,
    private val tangemSdkManager: TangemSdkManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userWalletId = savedStateHandle.get<Bundle>(AppRoute.CardSettings.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("User wallet ID is required for CardSettingsViewModel")

    private val scannedScanResponse = MutableStateFlow<ScanResponse?>(value = null)

    val screenState: MutableStateFlow<CardSettingsScreenState> = MutableStateFlow(getInitialState())

    private fun getInitialState() = CardSettingsScreenState(
        cardDetails = null,
        onElementClick = ::handleClickingItem,
        onScanCardClick = ::scanCard,
    )

    private fun scanCard() = viewModelScope.launch {
        scanCardProcessor.scan(allowsRequestAccessCodeFromRepository = true)
            .doOnSuccess { scanResponse ->
                scannedScanResponse.value = scanResponse

                val scannedUserWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
                if (userWalletId == scannedUserWalletId || scannedUserWalletId == null) {
                    updateCardDetails(scanResponse)
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
                    description = getResetToFactoryDescription(card.backupStatus, cardTypesResolver),
                ).let(::add)
            }
        }

        screenState.update { state ->
            state.copy(cardDetails = cardDetails)
        }
    }

    private fun handleClickingItem(item: CardInfo) {
        when (item) {
            is CardInfo.ChangeAccessCode -> {
                Analytics.send(Settings.CardSettings.ButtonChangeUserCode(AnalyticsParam.UserCode.AccessCode))
                changeAccessCode()
            }
            is CardInfo.ResetToFactorySettings -> {
                val card = requireNotNull(scannedScanResponse.value) {
                    "Impossible to reset card if ScanResponse is null"
                }.card

                Analytics.send(Settings.CardSettings.ButtonFactoryReset())
                store.dispatchNavigationAction {
                    push(
                        route = AppRoute.ResetToFactory(
                            userWalletId = userWalletId,
                            cardSpecificInfo = AppRoute.ResetToFactory.CardSpecificInfo(
                                cardId = card.cardId,
                                backupStatus = card.backupStatus,
                            ),
                        ),
                    )
                }
            }
            is CardInfo.SecurityMode -> {
                Analytics.send(Settings.CardSettings.ButtonChangeSecurityMode())
                store.dispatchNavigationAction {
                    push(route = AppRoute.DetailsSecurity(userWalletId))
                }
            }
            is CardInfo.AccessCodeRecovery -> {
                store.dispatchNavigationAction {
                    push(route = AppRoute.AccessCodeRecovery(userWalletId))
                }
            }
            else -> {}
        }
    }

    private fun changeAccessCode() = viewModelScope.launch {
        val scanResponse = requireNotNull(scannedScanResponse.value) { "Scan response is null" }

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
}
