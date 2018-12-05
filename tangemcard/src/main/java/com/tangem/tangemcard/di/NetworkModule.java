package com.tangem.tangemcard.di;

import com.tangem.tangemcard.data.network.Server;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
class NetworkModule {

    @Singleton
    @Provides
    @Named(Server.ApiTangem.URL_TANGEM)
    Retrofit provideRetrofitTangem() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiTangem.URL_TANGEM)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient())
                .build();
    }

    private OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder().
                addInterceptor(createHttpLoggingInterceptor()).
                build();
    }

    private HttpLoggingInterceptor createHttpLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }


}