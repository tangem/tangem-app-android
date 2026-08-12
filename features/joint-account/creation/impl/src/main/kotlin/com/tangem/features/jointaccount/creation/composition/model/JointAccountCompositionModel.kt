package com.tangem.features.jointaccount.creation.composition.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.jointaccount.creation.composition.state.JointAccountCompositionStateController
import com.tangem.features.jointaccount.creation.composition.state.transformers.ChangeRequiredToSignTransformer
import com.tangem.features.jointaccount.creation.composition.state.transformers.ChangeTotalMembersTransformer
import com.tangem.features.jointaccount.creation.composition.state.transformers.RestoreCompositionDraftTransformer
import com.tangem.features.jointaccount.creation.composition.state.transformers.UpdateCompositionInitialStateTransformer
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraft
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class JointAccountCompositionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val stateController: JointAccountCompositionStateController,
) : Model() {

    private val params = paramsContainer.require<JointAccountCreationChildParams>()

    val uiState: StateFlow<JointAccountCompositionUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        restoreDraft()
    }

    private fun updateInitialState() {
        stateController.update(
            UpdateCompositionInitialStateTransformer(
                onTotalMembersDecrement = { onTotalMembersChange(delta = -1) },
                onTotalMembersIncrement = { onTotalMembersChange(delta = 1) },
                onRequiredToSignDecrement = { onRequiredToSignChange(delta = -1) },
                onRequiredToSignIncrement = { onRequiredToSignChange(delta = 1) },
                onContinueClick = ::onContinueClick,
                onBackClick = ::onBackClick,
            ),
        )
    }

    private fun restoreDraft() {
        val composition = params.draftHolder.draft.value.composition ?: return

        stateController.update(RestoreCompositionDraftTransformer(draft = composition))
    }

    private fun onTotalMembersChange(delta: Int) {
        stateController.update(ChangeTotalMembersTransformer(delta = delta))
    }

    private fun onRequiredToSignChange(delta: Int) {
        stateController.update(ChangeRequiredToSignTransformer(delta = delta))
    }

    private fun onContinueClick() {
        val state = uiState.value

        params.draftHolder.setComposition(
            JointAccountCreationDraft.Composition(
                totalMembers = state.totalMembers.value,
                requiredToSign = state.requiredToSign.value,
            ),
        )

        router.push(JointAccountCreationRoute.DisplayName)
    }

    private fun onBackClick() {
        router.pop()
    }
}