package com.tangem.features.polymarket.impl.positionresult.model

import androidx.annotation.StringRes
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateEmitter
import com.tangem.domain.polymarket.interactor.PredictionClaimFailure
import com.tangem.domain.polymarket.interactor.SubmitPredictionClaimInteractor
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.positionresult.PolymarketPositionResultComponent
import com.tangem.features.polymarket.impl.positionresult.ui.state.PolymarketPositionResultUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class PolymarketPositionResultModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val claimInteractor: SubmitPredictionClaimInteractor,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val positionsUpdateEmitter: PredictionPositionsUpdateEmitter,
    private val messageSender: UiMessageSender,
    private val router: Router,
) : Model() {

    private val params = paramsContainer.require<PolymarketPositionResultComponent.Params>()

    val uiState: StateFlow<PolymarketPositionResultUM>
        field = MutableStateFlow(PolymarketPositionResultConverter().convert(params.position))

    fun onActionClick() {
        when (val action = uiState.value.action) {
            is PolymarketPositionResultUM.Action.Claim -> if (!action.isInProgress) claim()
            PolymarketPositionResultUM.Action.Dismiss -> router.pop()
        }
    }

    private fun claim() {
        modelScope.launch {
            setClaimProgress(isInProgress = true)
            val addresses = deriveAddresses.stored(params.userWalletId)
            if (addresses == null) {
                setClaimProgress(isInProgress = false)
                TangemLogger.e("The claim has no stored addresses to sign with")
                showError(R.string.prediction_claim_error_generic)
                return@launch
            }
            claimInteractor.claim(
                SubmitPredictionClaimInteractor.Params(
                    userWalletId = params.userWalletId,
                    walletId = params.userWalletId.stringValue,
                    ownerAddress = addresses.ownerAddress,
                    depositWalletAddress = addresses.depositWalletAddress,
                    conditionId = params.position.id.conditionId,
                    isNegRisk = params.position.isNegRisk,
                ),
            ).fold(
                ifLeft = { failure ->
                    setClaimProgress(isInProgress = false)
                    handle(failure)
                },
                ifRight = {
                    positionsUpdateEmitter.triggerUpdate(params.userWalletId)
                    router.pop()
                },
            )
        }
    }

    private fun handle(failure: PredictionClaimFailure) {
        when (failure) {
            is PredictionClaimFailure.SigningFailed ->
                if (failure.error != PolymarketSigningError.UserCancelled) {
                    TangemLogger.e("The claim was never signed: ${failure.error}")
                    showError(R.string.prediction_claim_error_generic)
                }
            is PredictionClaimFailure.Unconfirmed -> {
                TangemLogger.i("The claim ${failure.receipt.transactionId} was accepted but is unconfirmed")
                positionsUpdateEmitter.triggerUpdate(params.userWalletId)
                showError(R.string.prediction_claim_error_unconfirmed)
            }
            is PredictionClaimFailure.Failed -> {
                TangemLogger.e("The claim ${failure.receipt.transactionId} was rejected on chain")
                positionsUpdateEmitter.triggerUpdate(params.userWalletId)
                showError(R.string.prediction_claim_error_failed)
            }
            is PredictionClaimFailure.Rejected -> {
                TangemLogger.e("The backend refused the claim: ${failure.error}")
                when (failure.error) {
                    is PolymarketRedeemError.RelayerRejected -> {
                        positionsUpdateEmitter.triggerUpdate(params.userWalletId)
                        showError(R.string.prediction_claim_error_rejected)
                    }
                    PolymarketRedeemError.RelayerUnavailable -> showError(R.string.prediction_claim_error_unavailable)
                    else -> showError(R.string.prediction_claim_error_generic)
                }
            }
            PredictionClaimFailure.CallUnavailable,
            PredictionClaimFailure.NonceUnavailable,
            PredictionClaimFailure.Expired,
            -> {
                TangemLogger.e("The claim was never prepared: $failure")
                showError(R.string.prediction_claim_error_generic)
            }
        }
    }

    private fun setClaimProgress(isInProgress: Boolean) {
        uiState.update { state ->
            val action = state.action
            if (action is PolymarketPositionResultUM.Action.Claim) {
                state.copy(action = action.copy(isInProgress = isInProgress))
            } else {
                state
            }
        }
    }

    private fun showError(@StringRes message: Int) {
        messageSender.send(DialogMessage(message = resourceReference(message)))
    }
}