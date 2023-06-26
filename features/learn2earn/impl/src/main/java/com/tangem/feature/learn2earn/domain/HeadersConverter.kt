package com.tangem.feature.learn2earn.domain

import com.tangem.utils.converter.TwoWayConverter

/**
 * A converter that helps prepare a list of headers to put in an intent and retrieve them later.
 *
[REDACTED_AUTHOR]
 */
internal class HeadersConverter : TwoWayConverter<Map<String, String>, ArrayList<String>> {

    override fun convert(value: Map<String, String>): ArrayList<String> {
        val list = value.toList().map {
            "${it.first}$SPLIT_SIGN${it.second}"
        }
        return ArrayList(list)
    }

    override fun convertBack(value: ArrayList<String>): Map<String, String> {
        return value.map { it.split(SPLIT_SIGN) }.associate { it[0] to it[1] }
    }

    private companion object {
        const val SPLIT_SIGN = ":"
    }
}