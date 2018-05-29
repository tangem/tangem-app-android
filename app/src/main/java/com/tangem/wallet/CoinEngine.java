package com.tangem.wallet;

import android.net.Uri;

import com.tangem.domain.cardReader.CardProtocol;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by Ilia on 15.02.2018.
 */

public abstract class CoinEngine {

    public abstract String GetNextNode(Tangem_Card mCard);

    public abstract int GetNextNodePort(Tangem_Card mCard);

    public abstract String GetNode(Tangem_Card mCard);

    public abstract int GetNodePort(Tangem_Card mCard);

    public abstract void SwitchNode(Tangem_Card mCard);

    public abstract boolean AwaitingConfirmation(Tangem_Card card);

    public abstract boolean HasBalanceInfo(Tangem_Card card);

    public abstract boolean IsBalanceNotZero(Tangem_Card card);

    public abstract boolean IsBalanceAlterNotZero(Tangem_Card card);

    public abstract boolean CheckAmount(Tangem_Card card, String amount) throws Exception;

    public abstract int GetTokenDecimals(Tangem_Card card);

    public abstract String GetContractAddress(Tangem_Card card);

    public abstract byte[] Sign(String feeValue, String amountValue, String toValue, Tangem_Card mCard, CardProtocol protocol) throws Exception;

    public abstract boolean CheckUnspentTransaction(Tangem_Card mCard);

    public abstract Uri getShareWalletURIExplorer(Tangem_Card mCard);

    public abstract Long GetBalanceLong(Tangem_Card mCard);

    public abstract Uri getShareWalletURI(Tangem_Card mCard);

    public abstract String EvaluteFeeEquivalent(Tangem_Card mCard, String fee);

    public abstract boolean CheckAmountValie(Tangem_Card mCard, String amount, String fee, Long minFeeInInternalUnits);

    public abstract boolean InOutPutVisible();

    public abstract String GetBalance(Tangem_Card mCard);

    public abstract String GetBalanceWithAlter(Tangem_Card mCard);

    public abstract String GetBalanceCurrency(Tangem_Card card);

    public abstract String GetFeeCurrency();

    public abstract boolean IsNeedCheckNode();

    public abstract String GetBalanceEquivalent(Tangem_Card mCard);

    public abstract String GetBalanceValue(Tangem_Card mCard);

    public abstract String GetAmountDescription(Tangem_Card mCard, String amount) throws Exception;

    public abstract String GetAmountEqualentDescriptor(Tangem_Card mCard, String value);

    public abstract boolean ValdateAddress(String address, Tangem_Card catd);

    public abstract String calculateAddress(Tangem_Card mCard, byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException;

    public abstract String ConvertByteArrayToAmount(Tangem_Card mCard, byte[] bytes) throws Exception;

    public abstract byte[] ConvertAmountToByteArray(Tangem_Card mCard, String amount) throws Exception;

}