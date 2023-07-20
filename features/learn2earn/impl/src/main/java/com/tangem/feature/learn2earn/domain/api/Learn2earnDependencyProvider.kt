package com.tangem.feature.learn2earn.domain.api

import com.tangem.domain.common.CardTypesResolver

/**
 * Interface, a wrapper that allows us to get values from the AppStateHolder located in the app module
 *
[REDACTED_AUTHOR]
 */
interface Learn2earnDependencyProvider {

    fun getCardTypeResolver(): CardTypesResolver?

    fun getLocaleProvider(): () -> String

    fun getWebViewAuthCredentialsProvider(): () -> String?
}