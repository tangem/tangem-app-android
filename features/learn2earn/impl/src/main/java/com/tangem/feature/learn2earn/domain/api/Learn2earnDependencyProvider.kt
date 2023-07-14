package com.tangem.feature.learn2earn.domain.api

/**
 * Interface, a wrapper that allows us to get values from the AppStateHolder located in the app module
 *
* [REDACTED_AUTHOR]
 */
interface Learn2earnDependencyProvider {

    fun getLocaleProvider(): () -> String

    fun getWebViewAuthCredentialsProvider(): () -> String?
}
