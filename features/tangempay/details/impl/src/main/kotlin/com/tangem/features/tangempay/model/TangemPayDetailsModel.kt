package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.pay.repository.CardDetailsRepository
import com.tangem.features.tangempay.components.TangemPayDetailsComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.transformers.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: CardDetailsRepository,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
) : Model() {

    private val params: TangemPayDetailsComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()
    private val revealCardDetailsJobHolder = JobHolder()

    init {
        fetchBalance()
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = cardDetailsRepository.getCardBalance()
            uiState.update(DetailsBalanceTransformer(result))
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            fetchBalance().join()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun getInitialState() = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = router::pop, items = null),
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = ::onRefreshSwipe),
        balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(actionButtons = persistentListOf()),
        cardDetailsUM = TangemPayCardDetailsUM(),
        isBalanceHidden = false,
    ).let {
        DetailsHiddenStateTransformer(
            onClickReveal = ::revealCardDetails,
            cardNumberEnd = params.cardNumberEnd,
        ).transform(it)
    }

    private fun revealCardDetails() {
        modelScope.launch {
            uiState.update(
                transformer = DetailsRevealProgressStateTransformer(onClickHide = ::hideCardDetails),
            )
            cardDetailsRepository.revealCardDetails()
                .onRight {
                    uiState.update(
                        transformer = DetailsRevealedStateTransformer(
                            details = it,
                            onClickHide = ::hideCardDetails,
                            onClickCopy = ::copyData,
                        ),
                    )
                }
                .onLeft {
                    uiState.update(
                        transformer = DetailsHiddenStateTransformer(
                            onClickReveal = ::revealCardDetails,
                            cardNumberEnd = params.cardNumberEnd,
                        ),
                    )
                    showError()
                }
        }.saveIn(revealCardDetailsJobHolder)
    }

    private fun hideCardDetails() {
        modelScope.launch {
            revealCardDetailsJobHolder.cancel()
            uiState.update(
                transformer = DetailsHiddenStateTransformer(
                    onClickReveal = ::revealCardDetails,
                    cardNumberEnd = params.cardNumberEnd,
                ),
            )
        }
    }

    private fun copyData(text: String) {
        clipboardManager.setText(text = text.filterNot { it.isWhitespace() }, isSensitive = true)
    }
    private fun showError() {
        uiMessageSender.send(
            SnackbarMessage(TextReference.Res(R.string.tangempay_card_details_error_text)),
        )
    }
}