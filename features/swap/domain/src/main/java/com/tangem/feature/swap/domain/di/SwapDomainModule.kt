package com.tangem.feature.swap.domain.di

import com.tangem.domain.transaction.usecase.*
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

    /**
     * [REDACTED_TASK_KEY] — DEX-flavoured 12% gas-limit bump. See [com.tangem.feature.swap.domain.fee.PatchEthGasLimitForSwap.DEX_PERCENTAGE].
     */
    @Provides
    @Singleton
    @SwapDexGasLimit
    fun provideDexPatchEthGasLimitForSwap(): PatchEthGasLimitForSwap {
        return PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.DEX_PERCENTAGE)
    }

    /**
     * [REDACTED_TASK_KEY] — send/CEX-flavoured 5% gas-limit bump. See [PatchEthGasLimitForSwap.SEND_PERCENTAGE].
     */
    @Provides
    @Singleton
    @SwapSendGasLimit
    fun provideSendPatchEthGasLimitForSwap(): PatchEthGasLimitForSwap {
        return PatchEthGasLimitForSwap(percentage = PatchEthGasLimitForSwap.SEND_PERCENTAGE)
    }

    /**
     * [REDACTED_TASK_KEY] — DEX swap fee calculator. Not yet consumed by `SwapInteractorImpl` — this binding
     * lives here so the new helper is injectable when the caller is migrated in a follow-up PR.
     */
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

    /**
     * [REDACTED_TASK_KEY] — CEX swap fee calculator. Not yet consumed by `SwapInteractorImpl` — this binding
     * lives here so the new helper is injectable when the caller is migrated in a follow-up PR.
     */
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