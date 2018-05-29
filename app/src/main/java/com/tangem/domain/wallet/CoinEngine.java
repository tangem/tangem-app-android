package com.tangem.domain.wallet;

import android.net.Uri;

import com.tangem.domain.cardReader.CardProtocol;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Created by Ilia on 15.02.2018.
 */

public abstract class CoinEngine {

    public abstract String GetNextNode(TangemCard mCard);

    public abstract int GetNextNodePort(TangemCard mCard);

    public abstract String GetNode(TangemCard mCard);

    public abstract int GetNodePort(TangemCard mCard);

    public abstract void SwitchNode(TangemCard mCard);

    public abstract boolean AwaitingConfirmation(TangemCard card);

    public abstract boolean HasBalanceInfo(TangemCard card);

    public abstract boolean IsBalanceNotZero(TangemCard card);

    public abstract boolean IsBalanceAlterNotZero(TangemCard card);

    public abstract boolean CheckAmount(TangemCard card, String amount) throws Exception;

    public abstract int GetTokenDecimals(TangemCard card);

    public abstract String GetContractAddress(TangemCard card);

    public abstract byte[] Sign(String feeValue, String amountValue, String toValue, TangemCard mCard, CardProtocol protocol) throws Exception;

    public abstract boolean CheckUnspentTransaction(TangemCard mCard);

    public abstract Uri getShareWalletURIExplorer(TangemCard mCard);

    public abstract Long GetBalanceLong(TangemCard mCard);

    public abstract Uri getShareWalletURI(TangemCard mCard);

    public abstract String EvaluteFeeEquivalent(TangemCard mCard, String fee);

    public abstract boolean CheckAmountValie(TangemCard mCard, String amount, String fee, Long minFeeInInternalUnits);

    public abstract boolean InOutPutVisible();

    public abstract String GetBalance(TangemCard mCard);

    public abstract String GetBalanceWithAlter(TangemCard mCard);

    public abstract String GetBalanceCurrency(TangemCard card);

    public abstract String GetFeeCurrency();

    public abstract boolean IsNeedCheckNode();

    public abstract String GetBalanceEquivalent(TangemCard mCard);

    public abstract String GetBalanceValue(TangemCard mCard);

    public abstract String GetAmountDescription(TangemCard mCard, String amount) throws Exception;

    public abstract String GetAmountEqualentDescriptor(TangemCard mCard, String value);

    public abstract boolean ValdateAddress(String address, TangemCard catd);

    public abstract String calculateAddress(TangemCard mCard, byte[] pkUncompressed) throws NoSuchProviderException, NoSuchAlgorithmException;

    public abstract String ConvertByteArrayToAmount(TangemCard mCard, byte[] bytes) throws Exception;

    public abstract byte[] ConvertAmountToByteArray(TangemCard mCard, String amount) throws Exception;

}