package com.tangem.network.common

/**
 * Created by Anton Zhilenkov on 18/04/2022.
 */
interface NetworkInternalException

sealed class NetworkException(message: String?) : Throwable(message), NetworkInternalException
