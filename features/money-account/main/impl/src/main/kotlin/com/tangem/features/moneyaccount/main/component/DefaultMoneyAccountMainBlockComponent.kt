package com.tangem.features.moneyaccount.main.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM
import com.tangem.features.moneyaccount.main.ui.MoneyAccountMainBlockContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

private const val MONEY_ACCOUNT_CONTENT_TYPE = "MoneyAccount"

@Suppress("UnusedPrivateProperty")
internal class DefaultMoneyAccountMainBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: Unit,
) : MoneyAccountMainBlockComponent, AppComponentContext by context {

    override fun LazyListScope.moneyAccountMainContent(
        state: MoneyAccountMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier,
    ) {
        item(
            key = MONEY_ACCOUNT_CONTENT_TYPE,
            contentType = MONEY_ACCOUNT_CONTENT_TYPE,
        ) {
            MoneyAccountMainBlockContent(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.animateContentSize(),
            )
        }
    }

    @AssistedFactory
    interface Factory : MoneyAccountMainBlockComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultMoneyAccountMainBlockComponent
    }
}