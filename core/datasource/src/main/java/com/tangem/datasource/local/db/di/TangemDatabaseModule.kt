package com.tangem.datasource.local.db.di

import android.content.Context
import androidx.room.Room
import com.tangem.datasource.local.db.TangemDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object TangemDatabaseModule {

    private const val DATABASE_NAME = "tangem_database.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TangemDatabase {
        return Room.databaseBuilder(
            context,
            TangemDatabase::class.java,
            DATABASE_NAME,
        ).build()
    }
}