package com.tangem.tap.features.twins.redux

import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.domain.twins.TwinCardNumber

data class CreateTwinWalletState(
    val scanResponse: ScanResponse?,
    val step: CreateTwinWalletStep = CreateTwinWalletStep.FirstStep,
    val twinCardNumber: TwinCardNumber?,
    val createTwinWallet: CreateTwinWallet?,
    val showAlert: Boolean,
    val allowRecreatingWallet: Boolean? = null
)

enum class CreateTwinWalletStep { FirstStep, SecondStep, ThirdStep }

enum class CreateTwinWallet { CreateWallet, RecreateWallet }
