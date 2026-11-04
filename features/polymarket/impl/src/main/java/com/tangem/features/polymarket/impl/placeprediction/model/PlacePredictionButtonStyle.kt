package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.polymarket.usecase.CanSignPredictionOrderWithSessionKeyUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceButtonStyleUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class PlacePredictionButtonStyle @AssistedInject constructor(
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val uiState: MutableStateFlow<PlacePredictionUM>,
    @Assisted private val scope: CoroutineScope,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val featureToggles: PolymarketFeatureToggles,
    private val canSignPredictionOrderWithSessionKeyUseCase: CanSignPredictionOrderWithSessionKeyUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private var ownerStyle: PlaceButtonStyleUM? = null

    private var sessionKeyStyle: PlaceButtonStyleUM? = null

    private var sessionKeyCheck: Job? = null

    private var isSessionKeyLost = false

    init {
        getUserWalletUseCase(userWalletId).onRight { userWallet ->
            val style = if (userWallet.isHotWallet) PlaceButtonStyleUM.HOLD else PlaceButtonStyleUM.CARD
            ownerStyle = style
            uiState.update { it.copy(placeButtonStyle = style) }
        }
    }

    fun checkSessionKey() {
        val ownerStyle = ownerStyle ?: return
        sessionKeyCheck?.cancel()

        if (isSessionKeyLost || !featureToggles.isPolymarketSessionKeysEnabled) {
            sessionKeyStyle = ownerStyle
            applyIfIdle()
            return
        }

        sessionKeyCheck = scope.launch {
            val canSign = withContext(dispatchers.io) {
                canSignPredictionOrderWithSessionKeyUseCase(userWalletId = userWalletId)
            }
            sessionKeyStyle = if (canSign) PlaceButtonStyleUM.PLAIN else ownerStyle
            applyIfIdle()
        }
    }

    fun onSessionKeyLost() {
        sessionKeyCheck?.cancel()
        isSessionKeyLost = true
        sessionKeyStyle = ownerStyle
    }

    fun onPlacementFailed() {
        if (sessionKeyStyle != PlaceButtonStyleUM.PLAIN) return

        sessionKeyStyle = null
        checkSessionKey()
    }

    fun applyIfIdle() {
        val style = sessionKeyStyle ?: return
        uiState.update { state -> if (state.submit is SubmitUM.Idle) state.copy(placeButtonStyle = style) else state }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            userWalletId: UserWalletId,
            uiState: MutableStateFlow<PlacePredictionUM>,
            scope: CoroutineScope,
        ): PlacePredictionButtonStyle
    }
}