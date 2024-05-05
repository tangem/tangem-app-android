package com.tangem.core.decompose.di

/**
 * Interface for owning a Hilt component builder.
 */
interface HiltComponentBuilderOwner {

    /**
     * Provides access to the Hilt component builder instance.
     */
    val hiltComponentBuilder: DecomposeComponent.Builder
}