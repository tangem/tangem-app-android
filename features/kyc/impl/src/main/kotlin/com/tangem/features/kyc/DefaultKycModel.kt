package com.tangem.features.kyc

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.pay.KycStartInfo
import com.tangem.domain.pay.usecase.KycStartInfoUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
class DefaultKycModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val kycStartInfoUseCase: KycStartInfoUseCase,
) : Model() {

    private val _uiState: MutableStateFlow<KycStartInfo?> = MutableStateFlow(null)
    val uiState = _uiState.asStateFlow()

    fun getKycToken() {
        modelScope.launch {
            kycStartInfoUseCase().getOrNull()?.let { _uiState.emit(it) }
        }
    }
}