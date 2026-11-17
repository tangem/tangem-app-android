package com.tangem.features.tangempay.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.tangempay.ui.TangemPayMainBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val TANGEM_PAY_ACCOUNT_CONTENT_TYPE = "TangemPayAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultTangemPayMainBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : TangemPayMainBlockComponent, AppComponentContext by context {

    override fun LazyListScope.tangemPayMainContent(
        state: TangemPayMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        item(
            key = TANGEM_PAY_ACCOUNT_CONTENT_TYPE,
            contentType = TANGEM_PAY_ACCOUNT_CONTENT_TYPE,
        ) {
            TangemPayMainBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                isGrouped = false,
                modifier = modifier,
            )
        }
    }

    @Composable
    override fun GroupedRow(state: TangemPayMainUM, isBalanceHidden: Boolean, modifier: Modifier) {
        TangemPayMainBlockContent(
            state = state,
            isBalanceHidden = isBalanceHidden,
            isGrouped = true,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : TangemPayMainBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultTangemPayMainBlockComponent
    }
}