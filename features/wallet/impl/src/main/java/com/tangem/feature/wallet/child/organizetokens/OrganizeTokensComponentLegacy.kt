package com.tangem.feature.wallet.child.organizetokens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.organizetokens.model.OrganizeTokensModelLegacy
import com.tangem.feature.wallet.child.organizetokens.ui.OrganizeTokensScreen
import kotlinx.coroutines.launch

internal class OrganizeTokensComponentLegacy(
    appComponentContext: AppComponentContext,
    params: Params,
    onBack: () -> Unit,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: OrganizeTokensModelLegacy = getOrCreateModel(params)

    init {
        componentScope.launch {
            model.onBack.collect { onBack() }
        }
    }

    data class Params(val userWalletId: UserWalletId)

    @Composable
    override fun Content(modifier: Modifier) {
        val uiState by model.uiState.collectAsStateWithLifecycle()

        OrganizeTokensScreen(state = uiState)
    }
}