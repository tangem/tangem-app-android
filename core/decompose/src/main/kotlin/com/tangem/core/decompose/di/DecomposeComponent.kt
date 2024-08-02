package com.tangem.core.decompose.di

import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent

/**
 * Interface for the Decompose component.
 *
 * It is annotated as [ComponentScoped], meaning it has a lifecycle that is scoped to the component.
 */
@ComponentScoped
@DefineComponent(parent = SingletonComponent::class)
interface DecomposeComponent {

    /**
     * Builder interface for the component.
     */
    @DefineComponent.Builder
    interface Builder {

        /**
         * Sets the router for the component.
         *
         * @param router The router to set.
         * @return The builder instance.
         */
        fun router(@BindsInstance router: Router): Builder

        /**
         * Sets the UI message sender for the component.
         *
         * @param uiMessageSender The UI message sender to set.
         * @return The builder instance.
         */
        fun uiMessageSender(@BindsInstance uiMessageSender: UiMessageSender): Builder

        /**
         * Sets the parameters container for the component.
         *
         * @param container The parameters container to set.
         * @return The builder instance.
         */
        fun paramsContainer(@BindsInstance container: ParamsContainer): Builder

        /**
         * Builds the Decompose component.
         *
         * @return The built Decompose component.
         */
        fun build(): DecomposeComponent
    }
}