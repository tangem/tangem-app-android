package com.tangem.features.onboarding.v2.multiwallet.impl.child.backup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.model.MultiWalletBackupModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.ui.MultiWalletBackup
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Suppress("UnusedPrivateMember")
class MultiWalletBackupComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    backButtonClickFlow: SharedFlow<Unit>,
    onBack: () -> Unit,
    onEvent: (event: Event) -> Unit,
) : AppComponentContext by context, MultiWalletChildComponent {

    private val model: MultiWalletBackupModel = getOrCreateModel(params)

    init {
        params.innerNavigation.update {
            it.copy(
                stackSize = 5,
                stackMaxSize = 8,
            )
        }

        params.parentParams.titleProvider.changeTitle(
            text = resourceReference(R.string.onboarding_navbar_title_creating_backup),
        )

        componentScope.launch {
            model.eventFlow.collect(onEvent)
        }

        componentScope.launch {
            backButtonClickFlow.collect { model.onBack() }
        }

        componentScope.launch {
            model.onBackFlow.collect { onBack() }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        BackHandler { model.onBack() }

        MultiWalletBackup(
            modifier = modifier,
            state = state,
        )
    }

    enum class Event {
        Done, OneDeviceAdded, TwoDeviceAdded
    }
}
