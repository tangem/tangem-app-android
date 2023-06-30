package com.tangem.domain.common

/**
 * Provider for lazy initialization
 *
 * @param action initialization action
 *
[REDACTED_AUTHOR]
 */
class Provider<T>(action: () -> T) : () -> T by action