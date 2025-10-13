#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.di#end

import ${PACKAGE_NAME}.producer.Default${NAME}Producer
import ${PACKAGE_NAME}.producer.${NAME}Producer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ${NAME}ProducerFactoryModule {

    @Binds
    @Singleton
    fun bind${NAME}ProducerFactory(
        impl: Default${NAME}Producer.Factory,
    ): ${NAME}Producer.Factory
}