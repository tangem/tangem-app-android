package com.tangem.tap.di.domain

import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.StoriesRepository
import com.tangem.domain.stories.ShouldShowStoriesInteractor
import com.tangem.domain.settings.repositories.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object StoriesDomainModule {

    @Provides
    @Singleton
    fun provideShouldShowStoriesInteractor(
        storiesRepository: StoriesRepository,
        getStoryContentUseCase: GetStoryContentUseCase,
    ): ShouldShowStoriesInteractor {
        return ShouldShowStoriesInteractor(storiesRepository, getStoryContentUseCase)
    }

    @Provides
    @Singleton
    fun provideGetStoryContentUseCase(
        storiesRepository: StoriesRepository,
        settingsRepository: SettingsRepository,
    ): GetStoryContentUseCase {
        return GetStoryContentUseCase(storiesRepository, settingsRepository)
    }
}