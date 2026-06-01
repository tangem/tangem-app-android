package com.tangem.features.rating

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

interface RatingComponent : ComposableContentComponent {

    class Params(
        val onLoadRating: suspend () -> Int?,
        val onSubmitRating: suspend (rating: Int, feedback: String) -> Unit,
    )

    interface Factory {
        fun create(context: AppComponentContext, params: Params): RatingComponent
    }
}