package com.tangem.di

import com.tangem.data.network.Server
import dagger.Component
import retrofit2.Retrofit
import java.net.Socket
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [NetworkModule::class])
interface NetworkComponent {

    @get:Named(Server.ApiInfura.URL_INFURA)
    val retrofitInfura: Retrofit

    @get:Named(Server.ApiMaticTesnet.URL_MATIC_TESTNET)
    val retrofitMaticTesnet: Retrofit

    @get:Named(Server.ApiEstimatefee.URL_ESTIMATEFEE)
    val retrofitEstimatefee: Retrofit

    @get:Named(Server.ApiCoinmarket.URL_COINMARKET)
    val retrofitCoinmarketcap: Retrofit

    @get:Named(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
    val retrofitGitHubUserContent: Retrofit

    @get:Named(Server.ApiRootstock.URL_ROOTSTOCK)
    val retrofitRootstock: Retrofit

    @get:Named(Server.ApiBlockcypher.URL_BLOCKCYPHER)
    val retrofitBlockcypher: Retrofit

    @get:Named(Server.ApiSoChain.URL)
    val retrofitSoChain: Retrofit

    @get:Named(Server.ApiBlockchainInfo.URL_BLOCKCHAININFO)
    val retrofitBlockchainInfo: Retrofit

    @get:Named(Server.ApiDucatus.URL_DUCATUS)
    val retrofitDucatus: Retrofit

    @get:Named(Server.ApiBlockchair.URL_BLOCKCHAIR)
    val retrofitBlockchair: Retrofit

    @get:Named("socket")
    val socket: Socket
}