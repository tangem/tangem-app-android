package com.tangem.data.network

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.tangem.data.network.model.TezosAccountResponse
import com.tangem.data.network.model.TokenEmvTransferBody
import com.tangem.tangem_card.util.Log
import io.reactivex.CompletableObserver
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiConsumer
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

class ServerApiTokenEmv {
    private val TAG = ServerApiTokenEmv::class.java.simpleName

    private val tangemServer = ""

    private val tokenEmvApi = Retrofit.Builder()
            .baseUrl(tangemServer)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) //logging for testing
            .client(OkHttpClient.Builder().addInterceptor(
                    HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            ).build())
            .build()
            .create(TokenEmvApi::class.java)

    fun transfer(tokenEmvTransferBody: TokenEmvTransferBody, transferObserver: CompletableObserver) {
        Log.i(TAG, "new transfer request")

        tokenEmvApi.transfer(tokenEmvTransferBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(transferObserver)
    }
}