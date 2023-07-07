package com.tangem.common

/**
 * Provider for lazy initialization
 *
 * @param action initialization action
 *
 * @author Andrew Khokhlov on 29/06/2023
 */
class Provider<T>(action: () -> T) : () -> T by action
