package com.tangem.feature.referral.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.referral.domain.ReferralInteractor
import com.tangem.feature.referral.domain.models.DiscountType
import com.tangem.feature.referral.domain.models.ReferralData
import com.tangem.feature.referral.models.ReferralStateHolder
import com.tangem.feature.referral.models.ReferralStateHolder.ReferralInfoState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralInteractor: ReferralInteractor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    var uiState: ReferralStateHolder by mutableStateOf(createInitiallyUiState())
        private set

    init {
        loadReferralData()
    }

    fun setOnBackClicked(onBackClicked: () -> Unit) {
        uiState = uiState.copy(headerState = ReferralStateHolder.HeaderState(onBackClicked = onBackClicked))
    }

    private fun createInitiallyUiState() = ReferralStateHolder(
        headerState = ReferralStateHolder.HeaderState(onBackClicked = { }),
        referralInfoState = ReferralInfoState.Loading,
        errorToast = ReferralStateHolder.ErrorToast(
            visibility = false,
            changeVisibility = {
                uiState = uiState.copy(
                    referralInfoState = uiState.referralInfoState,
                    errorToast = uiState.errorToast.copy(visibility = false),
                )
            },
        ),
    )

    private fun loadReferralData() {
        val lastUiState = uiState
        uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) { referralInteractor.getReferralStatus() }
                .onSuccess { uiState = uiState.copy(referralInfoState = it.convertToReferralInfoState()) }
                .onFailure { uiState = lastUiState.copy(errorToast = lastUiState.errorToast.copy(visibility = true)) }
        }
    }

    private fun ReferralData.convertToReferralInfoState(): ReferralInfoState = when (this) {
        is ReferralData.ParticipantData -> ReferralInfoState.ParticipantContent(
            award = getAwardValue(),
            address = referral.address,
            discount = getDiscountValue(),
            purchasedWalletCount = referral.walletsPurchased,
            code = referral.promocode,
            url = tosLink,
        )
        is ReferralData.NonParticipantData -> ReferralInfoState.NonParticipantContent(
            award = getAwardValue(),
            discount = getDiscountValue(),
            url = tosLink,
            onParticipateClicked = ::onParticipateClicked,
        )
    }

    private fun ReferralData.getAwardValue(): String =
        "$award ${requireNotNull(tokens.firstOrNull()?.symbol) { "Token list is empty" }}"

    private fun ReferralData.getDiscountValue(): String {
        val discountSymbol = when (discountType) {
            DiscountType.PERCENTAGE -> "%"
            DiscountType.VALUE -> throw java.lang.IllegalStateException("Value doesn't support")
        }
        return "$discount $discountSymbol"
    }

    private fun onParticipateClicked() {
        val lastUiState = uiState
        uiState = uiState.copy(referralInfoState = ReferralInfoState.Loading)
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) { referralInteractor.startReferral() }
                .onSuccess { uiState = uiState.copy(referralInfoState = it.convertToReferralInfoState()) }
                .onFailure { uiState = lastUiState.copy(errorToast = lastUiState.errorToast.copy(visibility = true)) }
        }
    }
}