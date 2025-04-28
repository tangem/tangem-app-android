package com.tangem.tap.routing.utils

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.tap.routing.component.RoutingComponent.Child

internal fun <C : ComposableContentComponent, P : Any, F : ComponentFactory<P, C>> createComponentChild(
    context: AppComponentContext,
    params: P,
    componentFactory: F,
): Child {
    return Child.ComposableComponent(componentFactory.create(context, params))
}