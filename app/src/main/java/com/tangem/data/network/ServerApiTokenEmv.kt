package com.tangem.data.network

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.tangem.data.network.model.TokenEmvGetTransferFeeAnswer
import com.tangem.data.network.model.TokenEmvGetTransferFeeBody
import com.tangem.data.network.model.TokenEmvTransferAnswer
import com.tangem.data.network.model.TokenEmvTransferBody
import com.tangem.tangem_card.util.Log
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class ServerApiTokenEmv {
    private val TAG = ServerApiTokenEmv::class.java.simpleName

    private val tangemServer = "https://emvsupport.appspot.com/"

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

    fun transfer(tokenEmvTransferBody: TokenEmvTransferBody, transferObserver: SingleObserver<TokenEmvTransferAnswer>) {
        Log.i(TAG, "new transfer request")

        tokenEmvApi.transfer(tokenEmvTransferBody)
                .timeout(30, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(transferObserver)
    }

    fun getTransferFee(tokenEmvGetTransferFeeBody: TokenEmvGetTransferFeeBody, transferObserver: SingleObserver<TokenEmvGetTransferFeeAnswer>) {
        Log.i(TAG, "new get transfer fee request")

        tokenEmvApi.getTransferFee(tokenEmvGetTransferFeeBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(transferObserver)
    }
}