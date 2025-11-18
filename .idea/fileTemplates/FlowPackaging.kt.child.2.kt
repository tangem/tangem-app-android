#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.di#end

import ${PACKAGE_NAME}.producer.${NAME}Producer
import ${PACKAGE_NAME}.supplier.${NAME}Supplier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ${NAME}SupplierModule {

    @Provides
    @Singleton
    fun provide${NAME}Supplier(factory: ${NAME}Producer.Factory): ${NAME}Supplier {
        return object : ${NAME}Supplier(
            factory = factory,
            keyCreator = { TODO("Not yet implemented") },
        ) {}
    }
}