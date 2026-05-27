package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import arrow.core.getOrElse
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.managetokens.ValidateDerivationPathUseCase
import com.tangem.domain.managetokens.model.exceptoin.DerivationPathValidationException
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.component.CustomTokenDerivationInputComponent
import com.tangem.features.managetokens.entity.customtoken.CustomDerivationInputUM
import com.tangem.features.managetokens.entity.customtoken.SelectedDerivationPath
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.dialog.CustomDerivationInputDialog
import com.tangem.features.managetokens.utils.CardanoDerivationPathValidator
import com.tangem.features.managetokens.utils.DynamicAddressesDerivationValidator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

internal class DefaultCustomTokenDerivationInputComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: CustomTokenDerivationInputComponent.Params,
    private val validateDerivationPathUseCase: ValidateDerivationPathUseCase,
    private val dynamicAddressesRepository: DynamicAddressesRepository,
    private val dynamicAddressesFeatureToggles: DynamicAddressesFeatureToggles,
) : CustomTokenDerivationInputComponent, AppComponentContext by context {

    private val cardanoDerivationPathValidator = CardanoDerivationPathValidator()
    private val dynamicAddressesDerivationValidator = DynamicAddressesDerivationValidator(
        dynamicAddressesRepository = dynamicAddressesRepository,
        dynamicAddressesFeatureToggles = dynamicAddressesFeatureToggles,
    )

    private val state: MutableStateFlow<CustomDerivationInputUM> = MutableStateFlow(
        value = getInitialState(),
    )

    init {
        observeValueUpdates()
    }

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun Dialog() {
        val state by state.collectAsStateWithLifecycle()

        CustomDerivationInputDialog(
            model = state,
            onDismiss = ::dismiss,
        )
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeValueUpdates() {
        state
            .map { it.value.text }
            .distinctUntilChanged()
            .sample(periodMillis = 1_000)
            .flatMapLatest { value ->
                flow { emit(validateValue(value)) }
            }
            .launchIn(componentScope)
    }

    private fun getInitialState(): CustomDerivationInputUM = CustomDerivationInputUM(
        value = TextFieldValue(),
        error = null,
        updateValue = ::updateValue,
        isConfirmEnabled = false,
        onConfirm = ::confirm,
    )

    private suspend fun validateValue(value: String) {
        validateDerivationPathUseCase(value).getOrElse { e ->
            updateWithValidationError(e)
            return
        }

        val isInvalidForCardano = cardanoDerivationPathValidator.isInvalidForCardano(
            networkId = params.selectedNetwork.id,
            path = value,
        )
        if (isInvalidForCardano) {
            state.update { state ->
                state.copy(
                    error = resourceReference(R.string.custom_token_invalid_derivation_path),
                    isConfirmEnabled = false,
                )
            }
            return
        }

        val isInvalidForDA = dynamicAddressesDerivationValidator.isInvalidForDynamicAddresses(
            userWalletId = params.mode.userWalletId,
            networkId = params.selectedNetwork.id,
            path = value,
        )
        if (isInvalidForDA) {
            state.update { state ->
                state.copy(
                    error = resourceReference(R.string.dynamic_addresses_custom_token_error_on_addition),
                    isConfirmEnabled = false,
                )
            }
            return
        }

        state.update { state ->
            state.copy(
                error = null,
                isConfirmEnabled = true,
            )
        }
    }

    private fun updateWithValidationError(e: DerivationPathValidationException) {
        state.update { state ->
            state.copy(
                error = when (e) {
                    DerivationPathValidationException.Empty -> null
                    DerivationPathValidationException.Invalid -> {
                        resourceReference(R.string.custom_token_invalid_derivation_path)
                    }
                },
                isConfirmEnabled = false,
            )
        }
    }

    private fun updateValue(value: TextFieldValue) {
        state.update { state ->
            state.copy(value = value)
        }
    }

    private fun confirm() {
        if (state.value.error != null && !state.value.isConfirmEnabled) {
            return
        }

        val value = state.value.value.text
        val model = SelectedDerivationPath(
            id = params.selectedNetwork.id,
            value = Network.DerivationPath.Custom(value),
            name = value,
            isDefault = false,
        )

        params.onConfirm(model)
    }

    @AssistedFactory
    interface Factory : CustomTokenDerivationInputComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CustomTokenDerivationInputComponent.Params,
        ): DefaultCustomTokenDerivationInputComponent
    }
}