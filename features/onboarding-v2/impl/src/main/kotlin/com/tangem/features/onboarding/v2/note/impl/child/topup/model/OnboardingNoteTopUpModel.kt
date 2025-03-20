package com.tangem.features.onboarding.v2.note.impl.child.topup.model

import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onramp.GetLegacyTopUpUrlUseCase
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.note.impl.child.topup.OnboardingNoteTopUpComponent
import com.tangem.features.onboarding.v2.note.impl.child.topup.ui.state.OnboardingNoteTopUpUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class OnboardingNoteTopUpModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val userWalletBuilderFactory: UserWalletBuilder.Factory,
    private val getLegacyTopUpUrlUseCase: GetLegacyTopUpUrlUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params = paramsContainer.require<OnboardingNoteTopUpComponent.Params>()
    private val commonState = params.childParams.commonState
    private val scanResponse = params.childParams.commonState.value.scanResponse
    private var userWallet: UserWallet? = null

    private val _uiState = MutableStateFlow(
        OnboardingNoteTopUpUM(
            onRefreshBalanceClick = ::refreshBalance,
            onBuyCryptoClick = ::onBuyCryptoClick,
            onShowWalletAddressClick = ::onShowWalletAddressClick,
        ),
    )

    val uiState: StateFlow<OnboardingNoteTopUpUM> = _uiState

    init {
        Analytics.send(OnboardingEvent.Topup.ScreenOpened)
        observeArtwork()
        modelScope.launch {
            createUserWalletIfNull()
            observeCryptoCurrencyStatus()
            refreshBalance()
        }
    }

    private fun refreshBalance() {
        modelScope.launch {
            showBalanceLoadingProgress(true)
            createUserWalletIfNull()
            val userWalletId = requireNotNull(userWallet?.walletId)
            fetchCurrencyStatusUseCase(
                userWalletId = userWalletId,
                refresh = true,
            )
            showBalanceLoadingProgress(false)
        }
    }

    private fun onBuyCryptoClick() {
        val cryptoCurrencyStatus = params.childParams.commonState.value.cryptoCurrencyStatus ?: return
        modelScope.launch {
            getLegacyTopUpUrlUseCase(cryptoCurrencyStatus).onRight {
                urlOpener.openUrl(it)
            }
        }
    }

    private fun onShowWalletAddressClick() {
        // TODO [REDACTED_TASK_KEY]
    }

    private suspend fun createUserWalletIfNull() {
        if (userWallet != null) {
            return
        }
        val commonState = params.childParams.commonState.value
        userWallet = commonState.userWallet ?: createUserWallet(scanResponse)
    }

    private fun observeArtwork() {
        modelScope.launch {
            params.childParams.commonState.collect {
                _uiState.value = _uiState.value.copy(
                    cardArtworkUrl = it.cardArtworkUrl,
                )
            }
        }
    }

    private fun observeCryptoCurrencyStatus() {
        val userWalletId = userWallet?.walletId ?: return
        getPrimaryCurrencyStatusUpdatesUseCase(userWalletId = userWalletId).map { it.getOrNull() }.filterNotNull()
            .onEach(::applyCryptoCurrencyStatusToState).launchIn(modelScope)
    }

    private fun applyCryptoCurrencyStatusToState(status: CryptoCurrencyStatus) {
        commonState.update {
            it.copy(cryptoCurrencyStatus = status)
        }
        _uiState.update {
            it.copy(
                amountToCreateAccount = (status.value as? CryptoCurrencyStatus.NoAccount)
                    ?.amountToCreateAccount
                    ?.format(
                        {
                            crypto(
                                symbol = status.currency.symbol,
                                decimals = status.currency.decimals,
                            )
                        },
                    ),
                balance = (status.value as? CryptoCurrencyStatus.Loaded)?.amount?.format(
                    {
                        crypto(
                            symbol = status.currency.symbol,
                            decimals = status.currency.decimals,
                        )
                    },
                ),
                isTopUpDataLoading = status.value.networkAddress == null,
            )
        }
    }

    private fun showBalanceLoadingProgress(value: Boolean) {
        _uiState.update {
            it.copy(isRefreshing = value)
        }
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet {
        return requireNotNull(
            value = userWalletBuilderFactory.create(scanResponse = scanResponse).build(),
            lazyMessage = { "User wallet not created" },
        )
    }
}