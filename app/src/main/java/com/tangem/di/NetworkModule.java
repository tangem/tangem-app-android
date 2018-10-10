package com.tangem.di;

import com.tangem.data.network.Server;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
class NetworkModule {

    final static String PROVIDE_RETROFIT_INFURA = "provideRetrofitInfura";
    final static String PROVIDE_RETROFIT_ESTIMATEFEE = "provideRetrofitEstimatefee";

    @Singleton
    @Provides
    @Named(PROVIDE_RETROFIT_INFURA)
    Retrofit provideRetrofitInfura() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiInfura.URL_INFURA)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Singleton
    @Provides
    @Named(PROVIDE_RETROFIT_ESTIMATEFEE)
    Retrofit provideRetrofitEstimatefee() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiEstimatefee.URL_ESTIMATEFEE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

}