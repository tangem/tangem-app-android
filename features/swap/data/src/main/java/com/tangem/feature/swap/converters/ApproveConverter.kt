package com.tangem.feature.swap.converters

import com.tangem.datasource.api.oneinch.models.ApproveCalldataResponse
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.utils.converter.Converter

class ApproveConverter : Converter<ApproveCalldataResponse, ApproveModel> {

    override fun convert(value: ApproveCalldataResponse): ApproveModel {
        return ApproveModel(
            data = value.data,
            gasPrice = value.gasPrice,
            toAddress = value.toAddress,
        )
    }
}
