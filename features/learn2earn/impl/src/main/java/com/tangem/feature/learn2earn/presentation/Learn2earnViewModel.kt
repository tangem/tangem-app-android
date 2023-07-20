package com.tangem.feature.learn2earn.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.learn2earn.analytics.AnalyticsParam
import com.tangem.feature.learn2earn.analytics.Learn2earnEvents
import com.tangem.feature.learn2earn.domain.api.Learn2earnInteractor
import com.tangem.feature.learn2earn.domain.api.WebViewResult
import com.tangem.feature.learn2earn.domain.api.WebViewResultHandler
import com.tangem.feature.learn2earn.domain.models.PromotionError
import com.tangem.feature.learn2earn.impl.R
import com.tangem.feature.learn2earn.presentation.ui.state.*
import com.tangem.lib.crypto.models.errors.UserCancelledException
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
* [REDACTED_AUTHOR]
 */
@HiltViewModel
class Learn2earnViewModel @Inject constructor(
    private val interactor: Learn2earnInteractor,
    private val router: Learn2earnRouter,
    private val dispatchers: AppCoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
) : ViewModel() {

    var uiState: Learn2earnState by mutableStateOf(
        Learn2earnState.init(
            uiActions = Learn2earnUiActions(
                onButtonStoryClick = ::onButtonStoryClick,
                onButtonMainClick = ::onButtonMainClick,
            ),
        ),
    )
        private set

    init {
        uiState = uiState
            .updateStoriesVisibility(isVisible = interactor.isPromotionActiveOnStories())
            .updateGetBonusVisibility(isVisible = interactor.isPromotionActiveOnMain())
    }

    fun onMainScreenCreated() {
        updateMainScreenViews()
    }

    fun onMainScreenRefreshed() {
        updateMainScreenViews()
    }

    private fun updateMainScreenViews() {
        if (!interactor.isPromotionActiveOnMain()) {
            uiState = uiState.updateGetBonusVisibility(isVisible = false)
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val error = interactor.validateUserWallet()
            when {
                error == null -> {
                    updateUi {
                        uiState
                            .updateStoriesVisibility(isVisible = interactor.isPromotionActiveOnStories())
                            .updateGetBonusVisibility(isVisible = interactor.isPromotionActiveOnMain())
                            .changeGetBounsDescription(getBonusDescription())
                    }
                }
                // additional check after the error has been handled by interactor
                !interactor.isPromotionActive() -> {
                    updateUi { uiState.updateViewsVisibility(isVisible = false) }
                }
                else -> {
                    updateViewsVisibilityOnError(error)
                }
            }
        }
    }

    private suspend fun updateViewsVisibilityOnError(error: PromotionError) {
        when (error) {
            is PromotionError.ProgramNotFound,
            is PromotionError.ProgramWasEnd,
            is PromotionError.CodeWasAlreadyUsed,
            is PromotionError.WalletAlreadyHasAward,
            is PromotionError.CardAlreadyHasAward,
            -> {
                updateUi { uiState.updateViewsVisibility(isVisible = false) }
            }
            is PromotionError.CodeWasNotAppliedInShop -> {
                updateUi { uiState.updateGetBonusVisibility(isVisible = false) }
            }
            is PromotionError.CodeNotFound,
            -> {
                updateUi {
                    uiState.updateViewsVisibility(isVisible = true)
                        .changeGetBounsDescription(getBonusDescription())
                }
            }
            is PromotionError.UnknownError,
            PromotionError.NetworkUnreachable,
            -> Unit
        }
    }

    private fun onButtonStoryClick() {
        analytics.send(Learn2earnEvents.IntroductionProcess.ButtonLearn())
        router.openWebView(interactor.buildUriForNewUser(), interactor.getBasicAuthHeaders())
    }

    private fun onButtonMainClick() {
        if (interactor.isUserHadPromoCode() || interactor.isUserRegisteredInPromotion()) {
            analytics.send(Learn2earnEvents.MainScreen.NoticeLear2earn(AnalyticsParam.ClientType.Old()))
            requestAward()
        } else {
            analytics.send(Learn2earnEvents.MainScreen.NoticeLear2earn(AnalyticsParam.ClientType.New()))
            subscribeToWebViewResultEvents()
            router.openWebView(interactor.buildUriForOldUser(), interactor.getBasicAuthHeaders())
        }
    }

    private fun requestAward() {
        viewModelScope.launch(dispatchers.io) {
            updateUi { uiState.updateProgress(showProgress = true) }

            runCatching { interactor.requestAward() }
                .onSuccess { requestAwardResult ->
                    requestAwardResult.fold(
                        onSuccess = {
                            analytics.send(Learn2earnEvents.MainScreen.NoticeClaimSuccess())
                            val onHideDialog = {
                                uiState = uiState.updateGetBonusVisibility(isVisible = false)
                                    .hideDialog()
                            }
                            val successDialog = MainScreenState.Dialog.Claimed(
                                networkFullName = interactor.getAwardNetworkName(),
                                onOk = onHideDialog,
                                onDismissRequest = onHideDialog,
                            )
                            updateUi { uiState.showDialog(successDialog) }
                        },
                        onFailure = {
                            val errorDialog = createErrorDialog(it.toDomainError())
                            updateUi {
                                uiState.updateProgress(showProgress = false)
                                    .showDialog(errorDialog)
                            }
                        },
                    )
                }
                .onFailure { exception ->
                    updateUi { uiState.updateProgress(showProgress = false) }
                    if (exception is UserCancelledException) return@launch

                    val errorDialog = createErrorDialog(exception.toDomainError())
                    updateUi { uiState.showDialog(errorDialog) }
                }
        }
    }

    private fun getBonusDescription(): MainScreenState.Description {
        return if (interactor.isUserRegisteredInPromotion()) {
            MainScreenState.Description.GetBonus
        } else {
            MainScreenState.Description.Learn(interactor.getAwardAmount())
        }
    }

    private fun createErrorDialog(error: PromotionError): MainScreenState.Dialog {
        return when (error) {
            is PromotionError.CodeWasNotAppliedInShop -> {
                val onHideDialog = { uiState = uiState.hideDialog() }
                MainScreenState.Dialog.PromoCodeNotRegistered(
                    onOk = {
                        uiState = uiState.hideDialog()
                        subscribeToWebViewResultEvents()
                        router.openWebView(interactor.buildUriForNewUser(), interactor.getBasicAuthHeaders())
                    },
                    onCancel = onHideDialog,
                    onDismissRequest = onHideDialog,
                )
            }
            is PromotionError.CodeNotFound,
            is PromotionError.CodeWasAlreadyUsed,
            is PromotionError.CardAlreadyHasAward,
            is PromotionError.WalletAlreadyHasAward,
            is PromotionError.ProgramNotFound,
            is PromotionError.ProgramWasEnd,
            -> {
                val onHideDialog = {
                    uiState = uiState.hideDialog()
                        .updateGetBonusVisibility(isVisible = false)
                }
                MainScreenState.Dialog.Error(
                    textReference = TextReference.Str(error.description),
                    onOk = onHideDialog,
                    onDismissRequest = onHideDialog,
                )
            }
            is PromotionError.UnknownError, PromotionError.NetworkUnreachable -> {
                val onHideDialog = { uiState = uiState.hideDialog() }
                MainScreenState.Dialog.Error(
                    textReference = TextReference.Res(R.string.common_server_unavailable),
                    onOk = onHideDialog,
                    onDismissRequest = onHideDialog,
                )
            }
        }
    }

    private fun subscribeToWebViewResultEvents() {
        interactor.webViewResultHandler = object : WebViewResultHandler {
            override fun handleResult(result: WebViewResult) {
                when (result) {
                    is WebViewResult.NewUserLearningFinished -> {
                        interactor.webViewResultHandler = null
                    }
                    WebViewResult.ReadyForAward -> {
                        interactor.webViewResultHandler = null
                        updateMainScreenViews()
                        requestAward()
                    }
                    is WebViewResult.OldUserLearningFinished,
                    is WebViewResult.Learn2earnAnalyticsEvent,
                    WebViewResult.Empty,
                    -> Unit
                }
            }
        }
    }

    private suspend fun updateUi(updateBlock: suspend () -> Learn2earnState) {
        withContext(dispatchers.io) {
            val newState = updateBlock.invoke()
            withContext(dispatchers.main) { uiState = newState }
        }
    }
}

private fun Throwable.toDomainError(): PromotionError {
    return this as? PromotionError ?: PromotionError.UnknownError(message ?: "Unknown error")
}
