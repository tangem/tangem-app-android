package com.tangem.features.jointaccount.common.displayname.model

import androidx.compose.runtime.Stable
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.jointaccount.common.displayname.JointAccountDisplayNameComponent
import com.tangem.features.jointaccount.common.displayname.state.JointAccountDisplayNameStateController
import com.tangem.features.jointaccount.common.displayname.state.transformers.UpdateButtonIconTransformer
import com.tangem.features.jointaccount.common.displayname.state.transformers.UpdateButtonLoadingTransformer
import com.tangem.features.jointaccount.common.displayname.state.transformers.UpdateDisplayNameInitialStateTransformer
import com.tangem.features.jointaccount.common.displayname.state.transformers.UpdateDisplayNameTransformer
import com.tangem.features.jointaccount.common.displayname.ui.state.JointAccountDisplayNameUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class JointAccountDisplayNameModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountDisplayNameStateController,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val params = paramsContainer.require<JointAccountDisplayNameComponent.Params>()

    val uiState: StateFlow<JointAccountDisplayNameUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        observeWalletInteractionIcon()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateDisplayNameInitialStateTransformer(
                buttonText = params.buttonText,
                initialName = params.initialName.orEmpty(),
                onNameChange = ::onNameChange,
                onContinueClick = ::onContinueClick,
                onBackClick = ::onBackClick,
            ),
        )
    }

    private fun observeWalletInteractionIcon() {
        userWalletsListRepository.userWallets
            .map { wallets -> wallets?.firstOrNull { it.walletId == params.userWalletId } }
            .map { wallet -> wallet?.let(::walletInterationIcon) }
            .distinctUntilChanged()
            .onEach { iconRes -> stateController.update(UpdateButtonIconTransformer(iconRes = iconRes)) }
            .launchIn(modelScope)
    }

    private fun onNameChange(name: String) {
        stateController.update(UpdateDisplayNameTransformer(name = name))
    }

    private fun onContinueClick() {
        val state = uiState.value

        if (state.isButtonLoading) return
        stateController.update(UpdateButtonLoadingTransformer(isLoading = true))
        params.onContinueClick(state.name)

        // TODO: replace the imitation with the real card-signature (NFC) session once its task exists
        modelScope.launch {
            delay(timeMillis = SIGNING_IMITATION_DURATION)
            stateController.update(UpdateButtonLoadingTransformer(isLoading = false))
        }
    }

    private fun onBackClick() {
        router.pop()
    }

    private companion object {
        const val SIGNING_IMITATION_DURATION = 2_000L
    }
}