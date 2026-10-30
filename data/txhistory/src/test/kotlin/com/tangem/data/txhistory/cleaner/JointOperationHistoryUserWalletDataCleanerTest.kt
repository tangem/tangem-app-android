package com.tangem.data.txhistory.cleaner

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.dao.JointOperationDao
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointOperationHistoryUserWalletDataCleanerTest {

    private val jointOperationDao: JointOperationDao = mockk(relaxUnitFun = true)
    private val historySyncStateDao: HistorySyncStateDao = mockk(relaxUnitFun = true)

    private val cleaner = JointOperationHistoryUserWalletDataCleaner(
        jointOperationDao = jointOperationDao,
        historySyncStateDao = historySyncStateDao,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointOperationDao, historySyncStateDao)
    }

    @Test
    fun `GIVEN deleted wallets WHEN clear THEN their joint operations and sync state are removed`() = runTest {
        // Act
        cleaner.clear(listOf(UserWalletId("01"), UserWalletId("02")))

        // Assert
        coVerify(exactly = 1) { jointOperationDao.delete(listOf("01", "02")) }
        coVerify(exactly = 1) {
            historySyncStateDao.delete(type = "JOINT_OPERATION", userWalletIds = listOf("01", "02"))
        }
    }
}