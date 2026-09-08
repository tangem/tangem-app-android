package com.tangem.features.tangempay.withdraw.bank.stub

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderFactory
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import kotlinx.coroutines.flow.StateFlow

/**
 * Temporary stand-in for wizard steps that are not implemented yet: shows the step header over an empty body so the
 * flow can be walked end to end. Each step task replaces its branch in the wizard container with a real component;
 * the last one deletes this class.
 */
internal class TangemPayBankWithdrawStepStubComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val draft by params.draft.collectAsStateWithLifecycle()
        val header = remember(draft) { BankWithdrawHeaderFactory.create(draft = draft, step = params.step) }
        BackHandler(onBack = router::pop)
        TangemPayBankWithdrawStepStubScreen(header = header, onBackClick = router::pop, modifier = modifier)
    }

    data class Params(
        val draft: StateFlow<BankWithdrawDraft>,
        val step: TangemPayBankWithdrawInnerRoute,
    )
}