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
    val saveChanges: () -> Unit,
    val isSavingInProgress: Boolean,
)
