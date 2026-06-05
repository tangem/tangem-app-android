package com.tangem.data.pay.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.visa.TangemPayPendingOrdersStore
import com.tangem.datasource.local.visa.entity.TangemPayPendingOrderDM
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayPendingOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class DefaultTangemPayPendingOrdersRepositoryTest {

    private val store: TangemPayPendingOrdersStore = mockk(relaxUnitFun = true)

    private val repository = DefaultTangemPayPendingOrdersRepository(store = store)

    @Nested
    inner class GetAllFlow {

        @Test
        fun `GIVEN store emits DM list WHEN getAllFlow THEN maps each item to domain`() = runTest {
            every { store.getAllFlow() } returns flowOf(listOf(ORDER_DM))

            val result = repository.getAllFlow().first()

            assertThat(result).containsExactly(ORDER_DOMAIN)
        }
    }

    @Nested
    inner class GetAll {

        @Test
        fun `GIVEN store returns DM list WHEN getAll THEN maps each item to domain`() = runTest {
            coEvery { store.getAll() } returns listOf(ORDER_DM)

            val result = repository.getAll()

            assertThat(result).containsExactly(ORDER_DOMAIN)
        }
    }

    @Nested
    inner class GetByCard {

        @Test
        fun `GIVEN store returns DM list WHEN getByCard THEN delegates cardId and maps to domain`() = runTest {
            coEvery { store.getByCard(CARD_ID) } returns listOf(ORDER_DM)

            val result = repository.getByCard(CARD_ID)

            assertThat(result).containsExactly(ORDER_DOMAIN)
        }
    }

    @Nested
    inner class Save {

        @Test
        fun `WHEN save THEN converts domain order to DM and stores it`() = runTest {
            val dmSlot = slot<TangemPayPendingOrderDM>()
            coEvery { store.save(capture(dmSlot)) } returns Unit

            repository.save(ORDER_DOMAIN)

            assertThat(dmSlot.captured).isEqualTo(ORDER_DM)
        }
    }

    @Nested
    inner class Remove {

        @Test
        fun `WHEN remove THEN delegates orderId to store`() = runTest {
            repository.remove(ORDER_ID)

            coVerify(exactly = 1) { store.remove(ORDER_ID) }
        }
    }

    private companion object {
        const val ORDER_ID = "order-1"
        const val CARD_ID = "card-1"
        const val USER_WALLET_ID = "aabbcc112233"

        val ORDER_DM = TangemPayPendingOrderDM(
            orderId = ORDER_ID,
            userWalletId = USER_WALLET_ID,
            cardId = CARD_ID,
            type = "Reissue",
            status = "New",
        )

        val ORDER_DOMAIN = TangemPayPendingOrder(
            orderId = ORDER_ID,
            userWalletId = UserWalletId(USER_WALLET_ID),
            cardId = CARD_ID,
            type = TangemPayPendingOrder.Type.REISSUE,
            status = OrderStatus.NEW,
        )
    }
}