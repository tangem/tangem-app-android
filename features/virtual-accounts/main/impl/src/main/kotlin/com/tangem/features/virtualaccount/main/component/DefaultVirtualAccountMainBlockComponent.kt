package com.tangem.features.virtualaccount.main.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM
import com.tangem.features.virtualaccount.main.ui.VirtualAccountMainBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val VIRTUAL_ACCOUNT_CONTENT_TYPE = "VirtualAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultVirtualAccountMainBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : VirtualAccountMainBlockComponent, AppComponentContext by context {

    override fun LazyListScope.virtualAccountMainContent(
        state: VirtualAccountMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        item(
            key = VIRTUAL_ACCOUNT_CONTENT_TYPE,
            contentType = VIRTUAL_ACCOUNT_CONTENT_TYPE,
        ) {
            VirtualAccountMainBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.animateContentSize(),
            )
        }
    }

    @AssistedFactory
    interface Factory : VirtualAccountMainBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultVirtualAccountMainBlockComponent
    }
}