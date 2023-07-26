package com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

private const val LOADING_DELAY = 4_000L

@HiltViewModel
internal class TokenDetailsViewModel @Inject constructor() : ViewModel() {

    var router: InnerTokenDetailsRouter by Delegates.notNull()

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
    )

    private fun onBackClick() {
        router.popBackStack()
    }
}
