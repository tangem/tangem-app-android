package com.tangem.feature.swap.domain.di

import com.tangem.domain.transaction.usecase.CreateTransactionDataExtrasUseCase
import com.tangem.domain.transaction.usecase.EstimateFeeUseCase
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessTxUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForTokenUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.*
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.fee.CexSwapFeeCalculator
import com.tangem.feature.swap.domain.fee.DexSwapFeeCalculator
import com.tangem.feature.swap.domain.fee.PatchEthGasLimitForSwap
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.feature.swap.domain.transfer.SwapTransferInteractor
import com.tangem.feature.swap.domain.transfer.SwapTransferInteractorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class SwapDomainModule {

    @Provides
    @Singleton
    fun provideAllowPermissionsHandler(): AllowPermissionsHandler {
        return AllowPermissionsHandlerImpl()
    }

    @Provides
    @Singleton
    fun provideGetSwapUiModeUseCase(
        swapFeatureToggles: SwapFeatureToggles,
        swapRepository: SwapRepository,
    ): GetSwapUiModeUseCase = GetSwapUiModeUseCase(
        swapFeatureToggles = swapFeatureToggles,
        swapRepository = swapRepository,
    )

    @Provides
    @Singleton
    @SwapDexGasLimit
    fun provideDexPatchEthGasLimitForSwap(): PatchEthGasLimitForSwap {
        return PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.DEX_PERCENTAGE)
    }

    @Provides
    @Singleton
    @SwapSendGasLimit
    fun provideSendPatchEthGasLimitForSwap(): PatchEthGasLimitForSwap {
        return PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.SEND_PERCENTAGE)
    }

    @Provides
    @Singleton
    fun provideDexSwapFeeCalculator(
        getFeeUseCase: GetFeeUseCase,
        getEthSpecificFeeUseCase: GetEthSpecificFeeUseCase,
        getFeeForTokenUseCase: GetFeeForTokenUseCase,
        createTransactionExtrasUseCase: CreateTransactionDataExtrasUseCase,
        walletManagersFacade: WalletManagersFacade,
        @SwapDexGasLimit patchEthGasLimitForSwap: PatchEthGasLimitForSwap,
    ): DexSwapFeeCalculator = DexSwapFeeCalculator(
        getFeeUseCase = getFeeUseCase,
        getEthSpecificFeeUseCase = getEthSpecificFeeUseCase,
        getFeeForTokenUseCase = getFeeForTokenUseCase,
        createTransactionExtrasUseCase = createTransactionExtrasUseCase,
        walletManagersFacade = walletManagersFacade,
        patchEthGasLimitForSwap = patchEthGasLimitForSwap,
    )

    @Provides
    @Singleton
    fun provideCexSwapFeeCalculator(
        estimateFeeUseCase: EstimateFeeUseCase,
        estimateFeeForTokenUseCase: EstimateFeeForTokenUseCase,
        estimateFeeForGaslessTxUseCase: EstimateFeeForGaslessTxUseCase,
        @SwapSendGasLimit patchEthGasLimitForSwap: PatchEthGasLimitForSwap,
    ): CexSwapFeeCalculator = CexSwapFeeCalculator(
        estimateFeeUseCase = estimateFeeUseCase,
        estimateFeeForTokenUseCase = estimateFeeForTokenUseCase,
        estimateFeeForGaslessTxUseCase = estimateFeeForGaslessTxUseCase,
        patchEthGasLimitForSwap = patchEthGasLimitForSwap,
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal interface SwapDomainBindModule {

    @Binds
    @Singleton
    fun provideSwapInteractor(swapInteractor: SwapInteractorImpl): SwapInteractor

    @Binds
    @Singleton
    fun provideSwapTransferInteractor(swapTransferInteractor: SwapTransferInteractorImpl): SwapTransferInteractor
}