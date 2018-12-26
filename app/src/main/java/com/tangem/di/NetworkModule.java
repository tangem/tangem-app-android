package com.tangem.di;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.data.network.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

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
    @Named(Server.ApiInfura.URL_INFURA)
    Retrofit provideRetrofitInfura() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiInfura.URL_INFURA)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

//    //@Singleton // TODO:check
//    @Provides
//    @Named("Insight")
//    Retrofit provideRetrofitInsight(String insightURL) {
//        return new Retrofit.Builder()
//                .baseUrl(insightURL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//    }

    @Singleton
    @Provides
    @Named(Server.ApiEstimatefee.URL_ESTIMATEFEE)
    Retrofit provideRetrofitEstimatefee() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiEstimatefee.URL_ESTIMATEFEE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Singleton
    @Provides
    @Named(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
    Retrofit provideGithubusercontent() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiUpdateVersion.URL_UPDATE_VERSION)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient())
                .build();
    }

    @Singleton
    @Provides
    @Named(Server.ApiCoinmarket.URL_COINMARKET)
    Retrofit provideRetrofitCoinmarketcap() {
        return new Retrofit.Builder()
                .baseUrl(Server.ApiCoinmarket.URL_COINMARKET)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
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

    @Provides
    @Named("socket")
    Socket provideSocket() {
        Socket socket = new Socket();
        try {
            socket.setSoTimeout(2000);
            try {
                socket.bind(new InetSocketAddress(0));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return socket;
    }

}