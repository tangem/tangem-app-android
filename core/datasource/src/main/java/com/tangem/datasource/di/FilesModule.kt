package com.tangem.datasource.di

import com.tangem.datasource.files.AndroidFileReader
import com.tangem.datasource.files.FileReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface FilesModule {

    @Binds
    @Singleton
    fun bindFileReader(androidFileReader: AndroidFileReader): FileReader
}