package com.tangem.di;

import com.tangem.data.network.Server;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import retrofit2.Retrofit;

@Singleton
@Component(modules = {NetworkModule.class})
public interface AppComponent {

    @Named(Server.ApiInfura.URL_INFURA)
    Retrofit getRetrofitInfura();

    @Named(Server.ApiEstimatefee.URL_ESTIMATEFEE)
    Retrofit getRetrofitEstimatefee();

    @Named(Server.ApiTangem.URL_TANGEM)
    Retrofit getRetrofitTangem();

    @Named(Server.ApiCoinmarket.URL_COINMARKET)
    Retrofit getRetrofitCoinmarketcap();

}