package com.tangem.features.account.selector

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.account.AccountSelectorComponent
import com.tangem.features.account.impl.R
import com.tangem.features.account.selector.ui.AccountSelectorContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class DefaultAccountSelectorComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted private val params: AccountSelectorComponent.Params,
) : AppComponentContext by appComponentContext, AccountSelectorComponent {

    private val model: AccountSelectorModel = getOrCreateModel(params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.state.collectAsStateWithLifecycle()
        TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            ),
            onBack = ::dismiss,
            containerColor = TangemTheme.colors.background.primary,
            title = {
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.common_choose_wallet),
                    startIconRes = R.drawable.ic_back_24,
                    onStartClick = ::dismiss,
                )
            },
            content = {
                AccountSelectorContent(
                    state = state,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            },
        )
    }

    @AssistedFactory
    interface Factory : AccountSelectorComponent.Factory {
        override fun create(
            appComponentContext: AppComponentContext,
            params: AccountSelectorComponent.Params,
        ): DefaultAccountSelectorComponent
    }
}