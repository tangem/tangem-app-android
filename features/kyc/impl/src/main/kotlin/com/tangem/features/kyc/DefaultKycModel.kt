package com.tangem.features.kyc

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
class DefaultKycModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val kycRepository: KycRepository,
) : Model() {

    private val params: KycComponent.Params = paramsContainer.require()

    private val _uiState: MutableStateFlow<KycStartInfo?> = MutableStateFlow(null)
    val uiState = _uiState.asStateFlow()

    init {
        modelScope.launch {
            try {
                kycRepository.getKycStartInfo(params.userWalletId).getOrNull()?.let { _uiState.emit(it) }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}