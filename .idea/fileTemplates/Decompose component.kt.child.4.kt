#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.di#end

import ${PACKAGE_NAME}.Default${NAME}Component
import ${PACKAGE_NAME}.${NAME}Component
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ${NAME}ComponentModule {

    @Binds
    @Singleton
    fun bind${NAME}ComponentFactory(
        factory: Default${NAME}Component.Factory,
    ): ${NAME}Component.Factory
}