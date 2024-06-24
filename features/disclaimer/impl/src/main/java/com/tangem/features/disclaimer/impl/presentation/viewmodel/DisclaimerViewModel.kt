package com.tangem.features.disclaimer.impl.presentation.viewmodel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.common.routing.AppRoute.Disclaimer.Companion.IS_TOS_ACCEPTED_KEY
import com.tangem.domain.card.repository.CardRepository
import com.tangem.features.disclaimer.impl.navigation.DefaultDisclaimerRouter
import com.tangem.features.disclaimer.impl.presentation.state.DisclaimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DisclaimerViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val disclaimerRouter: DefaultDisclaimerRouter,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, DisclaimerClickIntents {

    private val isTosAccepted: Boolean = savedStateHandle[IS_TOS_ACCEPTED_KEY] ?: false

    val state: DisclaimerState
        get() = DisclaimerState(
            onAccept = ::onAccept,
            url = DISCLAIMER_URL,
            isTosAccepted = isTosAccepted,
        )

    override fun onAccept(shouldAskPushPermission: Boolean) {
        viewModelScope.launch {
            cardRepository.acceptTangemTOS()
            disclaimerRouter.openPushNotificationPermission()
        }
    }

    private companion object {
        const val DISCLAIMER_URL = "https://tangem.com/tangem_tos.html"
    }
}