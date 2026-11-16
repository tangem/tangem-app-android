package com.tangem.feature.tester.presentation.storybook.page.ds.pagination

import com.tangem.core.ui.ds2.pagination.TangemPagination
import com.tangem.feature.tester.presentation.storybook.entity.TangemPaginationStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

private const val DEFAULT_PAGE_COUNT = 10

internal fun StateUpdater<TangemPaginationStory>.build(): TangemPaginationStory {
    return TangemPaginationStory(
        background = TangemPagination.Background.None,
        pageCount = DEFAULT_PAGE_COUNT,
        onBackgroundChange = { background -> updateStory { it.copy(background = background) } },
        onPageCountChange = { count -> updateStory { it.copy(pageCount = count) } },
    )
}

internal val tangemPaginationStoryFactory
    get() = storyPageFactory(StateUpdater<TangemPaginationStory>::build)