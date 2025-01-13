package com.tangem.utils

/**
 * Provider for suspend lazy initialization
 *
 * @param action initialization action
 *
 * @author Nikita Samoilov on 13/01/2025
 */
class ProviderSuspend<T>(action: suspend () -> T) : suspend () -> T by action
