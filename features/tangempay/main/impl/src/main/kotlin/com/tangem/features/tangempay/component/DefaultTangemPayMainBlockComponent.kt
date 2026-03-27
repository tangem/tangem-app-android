package com.tangem.features.tangempay.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.tangempay.ui.TangemPayMainBlockContent
import com.tangem.features.tangempay.ui.TangemPayMainBlockItem
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
            if (LocalRedesignEnabled.current) {
                TangemPayMainBlockContent(state, isBalanceHidden, modifier)
            } else {
                TangemPayMainBlockItem(state, isBalanceHidden, modifier)
            }
        }
    }

    @AssistedFactory
    interface Factory : TangemPayMainBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultTangemPayMainBlockComponent
    }
}