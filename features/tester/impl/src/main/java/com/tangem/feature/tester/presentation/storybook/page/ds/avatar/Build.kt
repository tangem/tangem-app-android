package com.tangem.feature.tester.presentation.storybook.page.ds.avatar

import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.feature.tester.presentation.storybook.entity.TangemAvatarStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemAvatarStory>.build(): TangemAvatarStory {
    return TangemAvatarStory(
        variant = TangemAvatarStory.Variant.Account,
        fillColor = TangemAvatar.FillColor.DarkBlue,
        appearance = TangemAvatar.Appearance.Neutral,
        status = TangemAvatar.Status.Progress,
        hasPendingIndicator = false,
        hasUserInitial = true,
        isShimmerRounded = false,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant) } },
        onFillColorChange = { color -> updateStory { it.copy(fillColor = color) } },
        onAppearanceChange = { appearance -> updateStory { it.copy(appearance = appearance) } },
        onStatusChange = { status -> updateStory { it.copy(status = status) } },
        onPendingIndicatorToggle = { updateStory { it.copy(hasPendingIndicator = !it.hasPendingIndicator) } },
        onUserInitialToggle = { updateStory { it.copy(hasUserInitial = !it.hasUserInitial) } },
        onShimmerRoundedToggle = { updateStory { it.copy(isShimmerRounded = !it.isShimmerRounded) } },
    )
}

internal val tangemAvatarStoryFactory
    get() = storyPageFactory(StateUpdater<TangemAvatarStory>::build)