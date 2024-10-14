package com.tangem.features.managetokens.entity.managetokens

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import kotlinx.collections.immutable.ImmutableList

internal data class OnboardingManageTokensUM(
    val onBack: () -> Unit,
    val isInitialBatchLoading: Boolean,
    val isNextBatchLoading: Boolean,
    val items: ImmutableList<CurrencyItemUM>,
    val loadMore: () -> Boolean,
    val search: SearchBarUM,
    val scrollToTop: StateEvent<Unit> = consumedEvent(),
    val actionButtonConfig: ActionButtonConfig,
) {
    sealed class ActionButtonConfig {
        abstract val onClick: () -> Unit
        abstract val showProgress: Boolean

        data class Continue(
            override val onClick: () -> Unit,
            override val showProgress: Boolean = false,
            val showTangemIcon: Boolean,
        ) : ActionButtonConfig()

        data class Later(
            override val onClick: () -> Unit,
            override val showProgress: Boolean = false,
        ) : ActionButtonConfig()

        fun copySealed(showProgress: Boolean): ActionButtonConfig {
            return when (this) {
                is Continue -> copy(showProgress = showProgress)
                is Later -> copy(showProgress = showProgress)
            }
        }
    }
}