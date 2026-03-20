#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.di#end

import ${PACKAGE_NAME}.Default${NAME}FeatureToggles
import ${PACKAGE_NAME}.${NAME}FeatureToggles
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ${NAME}FeatureTogglesModule {

    @Binds
    @Singleton
    fun bind${NAME}FeatureToggles(impl: Default${NAME}FeatureToggles): ${NAME}FeatureToggles
}