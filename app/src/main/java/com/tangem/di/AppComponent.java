package com.tangem.di;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import retrofit2.Retrofit;

@Singleton
@Component(modules = {NetworkModule.class})
public interface AppComponent {

    @Named(NetworkModule.PROVIDE_RETROFIT_INFURA)
    Retrofit getRetrofitInfura();

    @Named(NetworkModule.PROVIDE_RETROFIT_ESTIMATEFEE)
    Retrofit getRetrofitEstimatefee();
}