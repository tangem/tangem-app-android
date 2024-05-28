package com.tangem.features.details

import androidx.fragment.app.Fragment
import com.tangem.core.decompose.context.AppComponentContext

interface DetailsEntryPoint {

    fun entryFragment(parentContext: AppComponentContext): Fragment
}