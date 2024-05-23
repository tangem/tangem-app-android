package com.tangem.features.details

import androidx.fragment.app.Fragment
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.details.component.preview.PreviewDetailsComponent

internal class PreviewDetailsEntryPoint : DetailsEntryPoint {

    override fun entryFragment(parentContext: AppComponentContext): Fragment {
        val component = PreviewDetailsComponent()

        return DetailsFragment.newInstance(component)
    }
}
