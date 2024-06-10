package com.tangem.core.decompose.context

import com.arkivanov.decompose.ComponentContext
import com.tangem.core.decompose.di.HiltComponentBuilderOwner
import com.tangem.core.decompose.navigation.NavigationOwner
import com.tangem.core.decompose.ui.UiMessageSenderOwner
import com.tangem.core.decompose.utils.ComponentScopeOwner
import com.tangem.core.decompose.utils.DispatchersOwner
import com.tangem.core.decompose.utils.TagsOwner

/**
 * Interface for the application component context.
 *
 * It combines several other interfaces related to navigation, dispatching, UI messaging, etc.
 */
interface AppComponentContext :
    ComponentContext,
    NavigationOwner,
    ComponentScopeOwner,
    DispatchersOwner,
    UiMessageSenderOwner,
    HiltComponentBuilderOwner,
    TagsOwner