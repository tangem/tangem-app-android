package com.tangem.feature.learn2earn.presentation.ui.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.feature.learn2earn.impl.R

/**
[REDACTED_AUTHOR]
 */
data class Learn2earnState(
    val storyScreenState: StoryScreenState,
    val mainScreenState: MainScreenState,
) {
    companion object
}

data class StoryScreenState(
    val isVisible: Boolean,
    val onClick: () -> Unit,
)

data class MainScreenState(
    val isVisible: Boolean,
    val onClick: () -> Unit,
    val description: Description,
    val showProgress: Boolean,
    val logoState: LogoState,
    val dialog: Dialog? = null,
) {

    sealed class LogoState(val alpha: Float) {
        object Idle : LogoState(alpha = 1.0f)
        object InProgress : LogoState(alpha = 0.2f)
    }

    sealed class Description(val title: TextReference, val subtitle: TextReference) {

        class Learn(award: Int) : Description(
            TextReference.Res(R.string.common_learn_and_earn),
            TextReference.PluralRes(R.plurals.main_learn_subtitle, award, WrappedList(listOf(award))),
        )

        object GetBonus : Description(
            TextReference.Res(R.string.common_learn_and_earn),
            TextReference.Res(R.string.main_get_bonus_subtitle),
        )
    }

    sealed class Dialog {

        data class Claimed(
            val networkFullName: String,
            val onOk: () -> Unit,
            val onDismissRequest: () -> Unit,
        ) : Dialog()

        data class PromoCodeNotRegistered(
            val onOk: () -> Unit,
            val onCancel: () -> Unit,
            val onDismissRequest: () -> Unit,
        ) : Dialog()

        data class Error(
            val textReference: TextReference,
            val onOk: () -> Unit,
            val onDismissRequest: () -> Unit,
        ) : Dialog()
    }
}

class Learn2earnUiActions(
    val onButtonStoryClick: () -> Unit,
    val onButtonMainClick: () -> Unit,
)