@file:Suppress("Filename")

package com.tangem.feature.swap.domain.di

import javax.inject.Qualifier

/**
 * Qualifier for the DEX-flavoured `PatchEthGasLimitForSwap` (12% gas-limit bump).
 *
 * For DEX, the gas limit calculated by the DEX provider for a given payload may shift during
 * mining; providers recommend padding the limit a bit so the transaction completes.
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class SwapDexGasLimit

/**
 * Qualifier for the send/CEX-flavoured `PatchEthGasLimitForSwap` (5% gas-limit bump).
 *
 * For CEX, the fee is calculated for a randomly generated address and the gas limit may differ
 * for the actual destination. Padding the limit slightly avoids underpaid transactions.
 */
@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class SwapSendGasLimit