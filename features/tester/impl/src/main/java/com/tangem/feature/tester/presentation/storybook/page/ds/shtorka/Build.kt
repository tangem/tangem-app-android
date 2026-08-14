package com.tangem.feature.tester.presentation.storybook.page.ds.shtorka

import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.feature.tester.presentation.storybook.entity.TangemShtorkaStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemShtorkaStory>.build(): TangemShtorkaStory {
    return TangemShtorkaStory(
        isMaterial = true,
        materialStyle = TangemSurface.MaterialStyle.Default,
        isBlurEnabled = true,
        showDragHandle = true,
        isContentScrollable = true,
        onMaterialToggle = { updateStory { it.copy(isMaterial = !it.isMaterial) } },
        onMaterialStyleChange = { style -> updateStory { it.copy(materialStyle = style) } },
        onBlurToggle = { updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) } },
        onDragHandleToggle = { updateStory { it.copy(showDragHandle = !it.showDragHandle) } },
        onContentScrollableToggle = { updateStory { it.copy(isContentScrollable = !it.isContentScrollable) } },
    )
}

internal val tangemShtorkaStoryFactory
    get() = storyPageFactory(StateUpdater<TangemShtorkaStory>::build)