package com.tangem.tap.routing.utils

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.RouteBundleParams
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.tap.DecomposeFragment
import com.tangem.tap.routing.RoutingComponent.Child
import com.tangem.utils.Provider

internal fun AppRoute.asFragmentChild(fragmentProvider: Provider<Fragment>): Child {
    val provider = Provider {
        val bundle = (this as? RouteBundleParams)?.getBundle()

        fragmentProvider().apply {
            arguments = bundle
        }
    }

    return Child.LegacyFragment(path, provider)
}

internal fun <C : ComposableContentComponent, P : Any, F : ComponentFactory<P, C>> AppRoute.asComponentChild(
    contextProvider: Provider<AppComponentContext>,
    params: P,
    componentFactory: F,
): Child {
    val fragmentProvider = Provider {
        DecomposeFragment.newInstance(
            tag = path,
            contextProvider = contextProvider,
            params = params,
            componentFactory = componentFactory,
        )
    }

    return Child.LegacyFragment(path, fragmentProvider)
}
