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
    private val kycRepository: KycRepository,
) : Model() {

    private val _uiState: MutableStateFlow<KycStartInfo?> = MutableStateFlow(null)
    val uiState = _uiState.asStateFlow()

    fun getKycToken() {
        modelScope.launch {
            kycRepository.getKycStartInfo().getOrNull()?.let { _uiState.emit(it) }
        }
    }
}