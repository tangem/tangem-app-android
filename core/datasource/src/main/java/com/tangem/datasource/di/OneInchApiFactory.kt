package com.tangem.datasource.di

import com.tangem.datasource.api.oneinch.OneInchApi

class OneInchApiFactory {

    private val oneInchApiMap = mutableMapOf<String, OneInchApi>()

    fun putApi(networkId: String, api: OneInchApi) {
        oneInchApiMap[networkId] = api
    }

    fun getApi(networkId: String): OneInchApi {
        return oneInchApiMap[networkId] ?: error("no api found for networkId $networkId")
    }
}
