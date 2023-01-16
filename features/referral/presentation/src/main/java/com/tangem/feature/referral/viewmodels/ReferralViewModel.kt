package com.tangem.feature.referral.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.feature.referral.analytics.ReferralEvents
import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.models.DiscountType
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.domain.models.ReferralInfo
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.ErrorSnackbar
import com.tangem.feature.referral.models.ReferralStateHolder.ReferralInfoState
import com.tangem.feature.referral.router.ReferralRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

@HiltViewModel
internal class ReferralViewModel @Inject constructor(
    private val referralInteractor: ReferralInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel() {

    var uiState: ReferralStateHolder by mutableStateOf(createInitiallyUiState())
        private set

    private var referralRouter: ReferralRouter by Delegates.notNull()

    init {
        loadReferralData()
    }

    fun setRouter(router: ReferralRouter) {
        referralRouter = router
        uiState = uiState.copy(headerState = ReferralStateHolder.HeaderState(onBackClicked = router::back))
    }

    fun onScreenOpened() {
        analyticsEventHandler.send(ReferralEvents.ReferralScreenOpened)
    }

    private fun createInitiallyUiState() = ReferralStateHolder(
        headerState = ReferralStateHolder.HeaderState(onBackClicked = { }),
        referralInfoState = ReferralInfoState.Loading,
        errorSnackbar = null,
        analytics = ReferralStateHolder.Analytics(
            onAgreementClicked = ::onAgreementClicked,
            onCopyClicked = ::onCopyClicked,
            onShareClicked = ::onShareClicked,
        ),
    )

    private fun loadReferralData() {
        uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) { referralInteractor.getReferralStatus() }
                .onSuccess(::showContent)
                .onFailure(::showErrorSnackbar)
        }
    }

    private fun participate() {
        analyticsEventHandler.send(ReferralEvents.ClickParticipate)
        uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) { referralInteractor.startReferral() }
                .onSuccess(::showContent)
                .onFailure(::showErrorSnackbar)
        }
    }

    private fun showContent(referralData: ReferralData) {
        uiState = uiState.copy(referralInfoState = referralData.convertToReferralInfoState())
    }

    private fun showErrorSnackbar(throwable: Throwable) {
        uiState = uiState.copy(
            errorSnackbar = ErrorSnackbar(throwable = throwable, onOkClicked = referralRouter::back),
        )
    }

    private fun onAgreementClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickTaC)
    }

    private fun onCopyClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickCopy)
    }

    private fun onShareClicked() {
        analyticsEventHandler.send(ReferralEvents.ClickShare)
    }

    private fun ReferralData.convertToReferralInfoState(): ReferralInfoState = when (this) {
        is ReferralData.ParticipantData -> ReferralInfoState.ParticipantContent(
            award = getAwardValue(),
            networkName = getNetworkName(),
            address = referral.getAddressValue(),
            discount = getDiscountValue(),
            purchasedWalletCount = referral.walletsPurchased,
            code = referral.promocode,
            shareLink = referral.shareLink,
            url = tosLink,
        )
        is ReferralData.NonParticipantData -> ReferralInfoState.NonParticipantContent(
            award = getAwardValue(),
            networkName = getNetworkName(),
            discount = getDiscountValue(),
            url = tosLink,
            onParticipateClicked = ::participate,
        )
    }

    private fun ReferralData.getAwardValue(): String = "$award ${getToken().symbol}"

    private fun ReferralData.getNetworkName(): String = getToken().networkId.replaceFirstChar(Char::uppercase)

    @Suppress("MagicNumber")
    private fun ReferralInfo.getAddressValue(): String {
        check(address.length > 5) { "Invalid address" }
        return address.substring(startIndex = 0, endIndex = 4) + "..." +
            address.substring(startIndex = address.length - 5, endIndex = address.length)
    }

    private fun ReferralData.getDiscountValue(): String {
        val discountSymbol = when (discountType) {
            DiscountType.PERCENTAGE -> "%"
            DiscountType.VALUE -> this.getToken().symbol
        }
        return "$discount$discountSymbol"
    }

    private fun ReferralData.getToken() = requireNotNull(tokens.firstOrNull()) { "Token list is empty" }
}
