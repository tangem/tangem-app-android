package com.tangem.features.hotwallet.createmobilewallet.importoptions.di

import com.tangem.core.decompose.model.Model
import com.tangem.features.hotwallet.createmobilewallet.importoptions.model.ImportOptionsBottomSheetModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ImportOptionsBottomSheetModule {

    @Binds
    @IntoMap
    @ClassKey(ImportOptionsBottomSheetModel::class)
    fun bindImportOptionsBottomSheetModel(model: ImportOptionsBottomSheetModel): Model
}