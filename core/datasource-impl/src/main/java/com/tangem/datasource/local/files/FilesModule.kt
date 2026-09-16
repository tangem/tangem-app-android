package com.tangem.datasource.local.files

import com.tangem.core.local.files.FileReader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FilesModule {

    @Binds
    fun bindFileReader(androidFileReader: AndroidFileReader): FileReader
}