package com.tangem.feature.wallet.child.wallet.model

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.TangemPayCustomerInfoError
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TangemPayMainInfoManager @Inject constructor(
    private val tangemPayMainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
) {

    val mainScreenCustomerInfo:
        StateFlow<Map<UserWalletId, Either<TangemPayCustomerInfoError, MainScreenCustomerInfo>>>
        field = MutableStateFlow(mapOf())

    suspend fun refreshTangemPayInfo(userWalletId: UserWalletId) {
        mainScreenCustomerInfo.update { currentMap ->
            val info = tangemPayMainScreenCustomerInfoUseCase(userWalletId)
            currentMap.toMutableMap().apply { this[userWalletId] = info }
        }
    }
}