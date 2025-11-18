#if (${PACKAGE_NAME} != "")package ${PACKAGE_NAME}.di#end

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import ${PACKAGE_NAME}.model.${NAME}Model
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface ${NAME}ModelModule {

    @Binds
    @IntoMap
    @ClassKey(${NAME}Model::class)
    fun bind${NAME}Model(model: ${NAME}Model): Model
}