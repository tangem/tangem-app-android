package com.tangem.features.kyc

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.features.kyc.entity.WebSdkKycUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WEB_VIEW_URL = "file:///android_asset/sumsub_web_sdk.html"

@Stable
@ModelScoped
internal class WebSdkKycModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val kycRepository: KycRepository,
) : Model() {

    val uiState: StateFlow<WebSdkKycUM>
        field = MutableStateFlow(getInitialState())

    init {
        modelScope.launch {
            kycRepository.getKycStartInfo()
                .onRight { result -> uiState.update { it.copy(accessToken = result.token, isLoading = false) } }
                .onLeft { router.pop() }
        }
    }

    private fun getInitialState(): WebSdkKycUM {
        return WebSdkKycUM(
            accessToken = null,
            url = WEB_VIEW_URL,
            onBackClick = router::pop,
            isLoading = true,
            onLoadingFinished = ::onLoadingFinished,
        )
    }

    private fun onLoadingFinished() {
        modelScope.launch { uiState.update { it.copy(isLoading = false) } }
    }
}