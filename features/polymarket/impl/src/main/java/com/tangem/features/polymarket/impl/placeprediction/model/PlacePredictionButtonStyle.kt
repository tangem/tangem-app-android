package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.polymarket.model.PredictionOrderSigningPreference
import com.tangem.domain.polymarket.usecase.GetPredictionOrderSessionKeyWindowUseCase
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.toKotlinDuration

@Suppress("LongParameterList")
internal class PlacePredictionButtonStyle @AssistedInject constructor(
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val uiState: MutableStateFlow<PlacePredictionUM>,
    @Assisted private val scope: CoroutineScope,
    getUserWalletUseCase: GetUserWalletUseCase,
    private val featureToggles: PolymarketFeatureToggles,
    private val getPredictionOrderSessionKeyWindowUseCase: GetPredictionOrderSessionKeyWindowUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private var ownerStyle: PlaceButtonStyleUM? = null

    private var sessionKeyStyle: PlaceButtonStyleUM? = null

    private var sessionKeyCheck: Job? = null

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

        if (!featureToggles.isPolymarketFastOrdersEnabled) {
            sessionKeyStyle = ownerStyle
            applyIfIdle()
            return
        }

        sessionKeyCheck = scope.launch {
            val signingWindow = withContext(dispatchers.io) {
                getPredictionOrderSessionKeyWindowUseCase(userWalletId = userWalletId)
            }
            if (signingWindow != null) {
                sessionKeyStyle = PlaceButtonStyleUM.PLAIN
                applyIfIdle()
                delay(signingWindow.toKotlinDuration())
            }
            sessionKeyStyle = ownerStyle
            applyIfIdle()
        }
    }

    fun onSessionKeyLost() {
        sessionKeyCheck?.cancel()
        sessionKeyStyle = ownerStyle
    }

    fun onPlacementFailed() {
        if (sessionKeyStyle != PlaceButtonStyleUM.PLAIN) return

        sessionKeyStyle = null
        checkSessionKey()
    }

    fun signingPreference(confirmedWith: PlaceButtonStyleUM): PredictionOrderSigningPreference? = when {
        confirmedWith != PlaceButtonStyleUM.PLAIN -> PredictionOrderSigningPreference.OWNER_ONLY
        featureToggles.isPolymarketFastOrdersEnabled -> PredictionOrderSigningPreference.SESSION_KEY_ONLY
        else -> null
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