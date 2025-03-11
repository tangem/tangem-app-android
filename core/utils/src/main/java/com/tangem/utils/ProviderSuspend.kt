package com.tangem.utils

/**
 * Provider for suspend lazy initialization
 *
 * @param action initialization action
 *
[REDACTED_AUTHOR]
 */
class ProviderSuspend<T>(action: suspend () -> T) : suspend () -> T by action