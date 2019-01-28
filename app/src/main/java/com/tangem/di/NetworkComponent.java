package com.tangem.di;

import com.tangem.data.network.Server;

import java.net.Socket;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import retrofit2.Retrofit;

@Singleton
@Component(modules = {NetworkModule.class})
public interface NetworkComponent {

    @Named(Server.ApiInfura.URL_INFURA)
    Retrofit getRetrofitInfura();

    @Named(Server.ApiEstimatefee.URL_ESTIMATEFEE)
    Retrofit getRetrofitEstimatefee();

    @Named(Server.ApiCoinmarket.URL_COINMARKET)
    Retrofit getRetrofitCoinmarketcap();

    @Named(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
    Retrofit getRetrofitGithubusercontent();

    @Named(Server.ApiRootstock.URL_ROOTSTOCK)
    Retrofit getRetrofitRootstock();

    @Named("socket")
    Socket getSocket();

}