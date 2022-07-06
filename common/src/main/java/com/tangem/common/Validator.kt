package com.tangem.common

/**
 * Created by Anton Zhilenkov on 18/04/2022.
 */
interface Validator<Data, Error> {
    fun validate(data: Data? = null): Error?
}
