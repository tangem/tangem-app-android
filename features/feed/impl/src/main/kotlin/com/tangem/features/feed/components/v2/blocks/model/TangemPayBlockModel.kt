package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.isCreated
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Whether the Tangem Pay pitch is worth making: it is, only while no wallet has a spending account and

 * that Pay is available there, or a customer still in KYC, whom the pitch leads back to it.
 *
 * Only wallets that actually carry an [Account.Payment] are asked for a status, so a user without
 * Tangem Pay never spins up a payment pipeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
internal class TangemPayBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    multiAccountListSupplier: MultiAccountListSupplier,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model() {

    val isVisible: StateFlow<Boolean> = multiAccountListSupplier()
        .map { lists ->
            lists.filter { list -> list.accounts.any { it is Account.Payment } }.map { it.userWalletId }
        }
        .distinctUntilChanged()
        .flatMapLatest { walletIds ->
            if (walletIds.isEmpty()) {
                flowOf(false)
            } else {
                combine(
                    flows = walletIds.map { walletId ->
                        paymentAccountStatusSupplier(walletId)
                            .map<AccountStatus.Payment, PaymentAccountStatusValue?> { status -> status.value }
                            .onStart { emit(null) }
                    },
                ) { values ->
                    values.none { it?.hasSpendingAccount == true } && values.any { it?.isPitchable == true }
                }
            }
        }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = false)
}

private val PaymentAccountStatusValue.hasSpendingAccount: Boolean
    get() = isCreated && this !is PaymentAccountStatusValue.UnderReview

private val PaymentAccountStatusValue.isPitchable: Boolean
    get() = this is PaymentAccountStatusValue.NotCreated || this is PaymentAccountStatusValue.UnderReview