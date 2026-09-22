package com.tangem.data.networks.di

import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.data.networks.entity.NetworkStatusDM
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dev.onenowy.moshipolymorphicadapter.NamePolymorphicAdapterFactory

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkStatusMoshiModule {

    @Provides
    @IntoSet
    fun provideNetworkStatusMoshiConfigurer(): NetworkMoshiConfigurer = NetworkMoshiConfigurer { builder ->
        builder.add(
            NamePolymorphicAdapterFactory.of(NetworkStatusDM::class.java)
                .withSubtype(NetworkStatusDM.Verified::class.java, "amounts")
                .withSubtype(NetworkStatusDM.NoAccount::class.java, "amount_to_create_account"),
        )
    }
}