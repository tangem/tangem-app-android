package com.tangem.datasource.socket

import com.tangem.core.remote.json.NetworkJson
import com.tangem.core.socket.DefaultSocketChannelFactory
import com.tangem.core.socket.SocketChannelFactory
import com.tangem.core.socket.transport.OkHttpSocketTransport
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SocketModule {

    private const val PING_INTERVAL_SECONDS = 15L

    @Provides
    @Singleton
    fun provideSocketChannelFactory(
        environment: AndroidSocketEnvironment,
        @NetworkJson json: Json,
        appScope: AppCoroutineScope,
    ): SocketChannelFactory = DefaultSocketChannelFactory(
        transport = OkHttpSocketTransport(
            OkHttpClient.Builder().pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS).build(),
        ),
        environment = environment,
        json = json,
        scope = appScope,
    )
}