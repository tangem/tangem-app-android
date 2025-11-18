#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.di#end

import ${PACKAGE_NAME}.fetcher.Default${NAME}Fetcher
import ${PACKAGE_NAME}.fetcher.${NAME}Fetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ${NAME}FetcherModule {

    @Binds
    @Singleton
    fun bind${NAME}Fetcher(impl: Default${NAME}Fetcher): ${NAME}Fetcher
}