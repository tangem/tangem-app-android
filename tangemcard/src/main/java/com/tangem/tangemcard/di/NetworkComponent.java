package com.tangem.tangemcard.di;

import com.tangem.tangemcard.data.network.Server;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import retrofit2.Retrofit;

@Singleton
@Component(modules = {NetworkModule.class})
public interface NetworkComponent {

    @Named(Server.ApiTangem.URL_TANGEM)
    Retrofit getRetrofitTangem();

}