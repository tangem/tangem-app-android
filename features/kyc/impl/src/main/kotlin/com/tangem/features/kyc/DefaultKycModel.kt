package com.tangem.features.kyc

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.repository.KycRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
class DefaultKycModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    kycRepositoryFactory: KycRepository.Factory,
) : Model() {

    private val kycRepository = kycRepositoryFactory.create()

    private val _uiState: MutableStateFlow<KycStartInfo?> = MutableStateFlow(null)
    val uiState = _uiState.asStateFlow()

    fun getKycToken(params: KycComponent.Params) {
        modelScope.launch {
            kycRepository.getKycStartInfo(address = params.targetAddress, cardId = params.cardId).getOrNull()
                ?.let { _uiState.emit(it) }
        }
    }
}