package com.tangem.features.feed.nav

import com.tangem.core.decompose.context.AppComponentContext

/**
 * Creates the screen of one [FeedRoute] type. The owning feature binds an implementation into the
 * feed's screen registry, keyed by the route class:
 *
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * internal interface FooScreenModule {
 *
 *     @Binds
 *     @IntoMap
 *     @ClassKey(FeedRoute.Foo::class)
 *     fun bindFooScreenFactory(impl: FooScreenFactory): FeedScreenFactory
 * }
 * ```
 *
 * The feed host resolves pushed routes through the registry, so adding a screen requires no host
 * code edits.
 */
interface FeedScreenFactory {

    /**
     * @param context child context of the feed stack; its `router` pushes [FeedRoute]s to the feed
     * stack and falls through to the global router for everything else
     * @param route the pushed route; the factory is registered per route class, so a safe cast to
     * the owned type is expected
     */
    fun create(context: AppComponentContext, route: FeedRoute): FeedScreenComponent
}