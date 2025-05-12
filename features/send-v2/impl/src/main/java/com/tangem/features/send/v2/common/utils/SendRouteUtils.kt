package com.tangem.features.send.v2.common.utils

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.common.CommonSendRoute

/**
 * Workaround to try fix duplicate route crash
 */
internal fun Router.safeNextClick(
    currentRoute: CommonSendRoute,
    nextRoute: CommonSendRoute,
    childStack: Value<ChildStack<CommonSendRoute, ComposableContentComponent>>,
    popBack: () -> Unit,
) {
    if (currentRoute.isEditMode) {
        popBack()
    } else {
        val isAlreadyInStack = childStack.value.items.any { it.configuration == nextRoute }
        if (isAlreadyInStack) {
            popTo(nextRoute)
        } else {
            push(nextRoute)
        }
    }
}