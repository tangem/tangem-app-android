package com.tangem.features.allservices.impl.entries

import com.tangem.core.decompose.context.AppComponentContext

/** Base of the screen's entries: each runs on a child context the screen creates for it, keyed by [key]. */
internal abstract class AllServicesEntryComponent(
    context: AppComponentContext,
    override val key: String,
) : AllServicesEntry, AppComponentContext by context