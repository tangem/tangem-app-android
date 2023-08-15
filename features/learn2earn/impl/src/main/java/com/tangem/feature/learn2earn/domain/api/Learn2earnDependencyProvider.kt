package com.tangem.feature.learn2earn.domain.api

import com.tangem.common.Provider
import com.tangem.domain.common.CardTypesResolver
import kotlinx.coroutines.flow.Flow

/**
 * Interface, a wrapper that allows us to get values from the AppStateHolder located in the app module
 *
[REDACTED_AUTHOR]
 */
interface Learn2earnDependencyProvider {

    fun getCardTypeResolverFlow(): Flow<CardTypesResolver?>

    fun getWebViewAuthCredentialsProvider(): Provider<String?>
}