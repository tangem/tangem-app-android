package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenInfoBlockState
import com.tangem.features.tokendetails.impl.R
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

private const val LOADING_DELAY = 4_000L

@HiltViewModel
internal class TokenDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[TokenDetailsRouter.SELECTED_CURRENCY_KEY]
        ?: error("no expected parameter CryptoCurrency found")

    var router by Delegates.notNull<InnerTokenDetailsRouter>()

    var uiState by mutableStateOf(getInitialState())
        private set

    init {
        // simulate loading state
        viewModelScope.launch {
            delay(LOADING_DELAY)
            uiState = uiState.copy(
                tokenBalanceBlockState = TokenDetailsPreviewData.balanceContent,
                marketPriceBlockState = TokenDetailsPreviewData.marketPriceContent,
            )
        }
    }

    private fun getInitialState() = TokenDetailsPreviewData.tokenDetailsState.copy(
        topAppBarConfig = TokenDetailsPreviewData.tokenDetailsTopAppBarConfig.copy(
            onBackClick = ::onBackClick,
        ),
        tokenInfoBlockState = TokenInfoBlockState(
            name = cryptoCurrency.name,
            iconUrl = requireNotNull(cryptoCurrency.iconUrl),
            currency = when (cryptoCurrency) {
                is CryptoCurrency.Coin -> TokenInfoBlockState.Currency.Native
                is CryptoCurrency.Token -> TokenInfoBlockState.Currency.Token(
                    networkName = cryptoCurrency.standardType.name,
                    blockchainName = cryptoCurrency.blockchainName,
// [REDACTED_TODO_COMMENT]
                    networkIcon = R.drawable.img_eth_22,
                )
            },
        ),
    )

    private fun onBackClick() {
        router.popBackStack()
    }
}
