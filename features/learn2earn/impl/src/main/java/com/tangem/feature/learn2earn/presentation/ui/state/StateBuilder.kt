package com.tangem.feature.learn2earn.presentation.ui.state

/**
* [REDACTED_AUTHOR]
 */
internal fun Learn2earnState.Companion.init(uiActions: Learn2earnUiActions): Learn2earnState {
    return Learn2earnState(
        storyScreenState = StoryScreenState(
            isVisible = false,
            onClick = uiActions.onButtonStoryClick,
        ),
        mainScreenState = MainScreenState(
            isVisible = false,
            onClick = uiActions.onButtonMainClick,
            description = MainScreenState.Description.Learn(0),
            logoState = MainScreenState.LogoState.Idle,
            showProgress = false,
        ),
    )
}

internal fun Learn2earnState.updateStoriesVisibility(isVisible: Boolean): Learn2earnState {
    return if (storyScreenState.isVisible == isVisible) {
        this
    } else {
        copy(
            storyScreenState = storyScreenState.copy(
                isVisible = isVisible,
            ),
        )
    }
}

internal fun Learn2earnState.updateGetBonusVisibility(isVisible: Boolean): Learn2earnState {
    return if (mainScreenState.isVisible == isVisible) {
        this
    } else {
        copy(
            mainScreenState = mainScreenState.copy(
                isVisible = isVisible,
            ),
        )
    }
}

internal fun Learn2earnState.updateViewsVisibility(isVisible: Boolean): Learn2earnState {
    return updateStoriesVisibility(isVisible)
        .updateGetBonusVisibility(isVisible)
}

internal fun Learn2earnState.changeGetBounsDescription(description: MainScreenState.Description): Learn2earnState {
    return if (mainScreenState.description == description) {
        this
    } else {
        copy(
            mainScreenState = mainScreenState.copy(
                description = description,
            ),
        )
    }
}

internal fun Learn2earnState.showDialog(dialog: MainScreenState.Dialog): Learn2earnState {
    return if (mainScreenState.dialog == dialog) {
        this
    } else {
        copy(
            mainScreenState = mainScreenState.copy(
                dialog = dialog,
            ),
        )
    }
}

internal fun Learn2earnState.hideDialog(): Learn2earnState {
    return if (mainScreenState.dialog == null) {
        this
    } else {
        copy(
            mainScreenState = mainScreenState.copy(
                dialog = null,
            ),
        )
    }
}

internal fun Learn2earnState.updateProgress(showProgress: Boolean): Learn2earnState {
    return if (mainScreenState.showProgress == showProgress) {
        this
    } else {
        copy(
            mainScreenState = mainScreenState.copy(
                showProgress = showProgress,
                logoState = if (showProgress) {
                    MainScreenState.LogoState.InProgress
                } else {
                    MainScreenState.LogoState.Idle
                },
            ),
        )
    }
}
