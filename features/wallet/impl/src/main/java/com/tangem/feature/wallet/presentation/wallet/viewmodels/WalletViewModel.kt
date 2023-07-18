package com.tangem.feature.wallet.presentation.wallet.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.card.GetAccessCodeSavingStatusUseCase
import com.tangem.domain.card.GetBiometricsStatusUseCase
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.SetAccessCodeRequestPolicyUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.userwallets.UserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.domain.tokens.error.TokensError
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.utils.LoadingItemsProvider.getLoadingMultiCurrencyTokens
import com.tangem.feature.wallet.presentation.wallet.utils.TokenErrorToWalletStateConverter
import com.tangem.feature.wallet.presentation.wallet.utils.TokenListToWalletStateConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Wallet screen view model
 *
* [REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
@HiltViewModel
internal class WalletViewModel @Inject constructor(
    private val saveWalletUseCase: SaveWalletUseCase,
    private val getBiometricsStatusUseCase: GetBiometricsStatusUseCase,
    private val setAccessCodeRequestPolicyUseCase: SetAccessCodeRequestPolicyUseCase,
    private val getAccessCodeSavingStatusUseCase: GetAccessCodeSavingStatusUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val scanCardProcessor: ScanCardProcessor,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    /** Feature router */
    var router: InnerWalletRouter by Delegates.notNull()

    /** Screen state */
    var uiState by mutableStateOf(getInitialState())
        private set

    private var getTokenListJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }
// [REDACTED_TODO_COMMENT]
    private fun getInitialState(): WalletStateHolder {
        val state = WalletPreviewData.multicurrencyWalletScreenState.copy(
            onBackClick = ::onBackClick,
            topBarConfig = createTopBarConfig(),
            walletsListConfig = WalletPreviewData.multicurrencyWalletScreenState.walletsListConfig.copy(
                onWalletChange = ::selectWallet,
            ),
            contentItems = getLoadingMultiCurrencyTokens(),
        )

        val selectedWalletIndex = state.walletsListConfig.selectedWalletIndex
        val selectedWalletId = WalletPreviewData.wallets.keys.elementAt(selectedWalletIndex)
        launchGetTokenListJob(selectedWalletId)

        return state
    }

    private fun onBackClick() {
        router.popBackStack()
    }

    private fun createTopBarConfig(): WalletTopBarConfig {
        return WalletTopBarConfig(
            onScanCardClick = ::onScanCardClick,
            onMoreClick = { router.openDetailsScreen() },
        )
    }

    private fun onScanCardClick() {
        val prevRequestPolicyStatus = getBiometricsStatusUseCase()

        // Update access the code policy according access code saving status
        setAccessCodeRequestPolicyUseCase(isBiometricsRequestPolicy = getAccessCodeSavingStatusUseCase())

        viewModelScope.launch(dispatchers.io) {
            scanCardProcessor.scan(allowsRequestAccessCodeFromRepository = true)
                .doOnSuccess {
                    // If card's public key is null then user wallet will be null
                    val userWallet = UserWalletBuilder(scanResponse = it).build()

                    if (userWallet != null) {
                        saveWalletUseCase(userWallet)
                            .onLeft {
                                // Rollback policy if card saving was failed
                                setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                            }
                    } else {
                        // Rollback policy if card saving was failed
                        setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                    }
                }
                .doOnFailure {
                    // Rollback policy if card scanning was failed
                    setAccessCodeRequestPolicyUseCase(prevRequestPolicyStatus)
                }
        }
    }
// [REDACTED_TODO_COMMENT]
    private fun selectWallet(index: Int) {
        if (uiState.walletsListConfig.selectedWalletIndex == index) return

        Log.i("WalletViewModel", "selectWallet: $index")

        uiState = when (val state = uiState) {
            is WalletStateHolder.MultiCurrencyContent -> state.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
            is WalletStateHolder.SingleCurrencyContent -> state.copy(
                walletsListConfig = uiState.walletsListConfig.copy(selectedWalletIndex = index),
            )
        }

        val selectedWalletId = WalletPreviewData.wallets.keys.elementAt(index)
        launchGetTokenListJob(selectedWalletId)
    }

    private fun launchGetTokenListJob(userWalletId: UserWalletId) {
        getTokenListJob = getTokenListUseCase(userWalletId)
            .distinctUntilChanged()
            .mapLatest(::updateStateWithTokenListOrError)
            .onEach { uiState = it }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun updateStateWithTokenListOrError(tokenList: Either<TokensError, TokenList>): WalletStateHolder {
        val updateStateWithError = { error: TokensError ->
            val converter = TokenErrorToWalletStateConverter(uiState)

            converter.convert(error)
        }
        val updateState = { list: TokenList ->
            val converter = TokenListToWalletStateConverter(
                uiState,
                isWalletContentHidden = false, // TODO: https://tangem.atlassian.net/browse/AND-4007
                fiatCurrencyCode = "USD", // TODO: https://tangem.atlassian.net/browse/AND-4006
                fiatCurrencySymbol = "$", // TODO: https://tangem.atlassian.net/browse/AND-4006
            )

            converter.convert(list)
        }

        return tokenList.fold(ifLeft = updateStateWithError, ifRight = updateState)
    }
}
