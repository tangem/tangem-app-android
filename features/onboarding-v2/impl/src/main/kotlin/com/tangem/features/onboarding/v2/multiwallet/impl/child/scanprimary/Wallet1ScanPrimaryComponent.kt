package com.tangem.features.onboarding.v2.multiwallet.impl.child.scanprimary

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.scanprimary.model.Wallet1ScanPrimaryModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.scanprimary.ui.Wallet1ScanPrimary
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class Wallet1ScanPrimaryComponent(
    context: AppComponentContext,
    params: MultiWalletChildParams,
    onDone: () -> Unit,
) : AppComponentContext by context, ComposableContentComponent {

    private val model: Wallet1ScanPrimaryModel = getOrCreateModel(params)

    init {
        params.innerNavigation.update {
            it.copy(
                stackSize = 4,
                stackMaxSize = 8,
            )
        }

        params.parentParams.titleProvider.changeTitle(
            text = resourceReference(R.string.onboarding_navbar_title_creating_backup),
        )

        componentScope.launch { model.onDone.collect { onDone() } }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Wallet1ScanPrimary(
            isRing = model.isRing,
            onScanPrimaryClick = model::onScanPrimaryClick,
        )
    }
}