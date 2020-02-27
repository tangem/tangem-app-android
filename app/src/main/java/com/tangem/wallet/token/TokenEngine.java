package com.tangem.wallet.token;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;

import com.google.common.base.Strings;
import com.tangem.App;
import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiBlockcypher;
import com.tangem.data.network.ServerApiInfura;
import com.tangem.data.network.model.BlockcypherFee;
import com.tangem.data.network.model.BlockcypherResponse;
import com.tangem.data.network.model.BlockcypherTxref;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.tasks.SignTask;
import com.tangem.util.CryptoUtil;
import com.tangem.util.DecimalDigitsInputFilter;
import com.tangem.wallet.BTCUtils;
import com.tangem.wallet.BalanceValidator;
import com.tangem.wallet.CoinData;
import com.tangem.wallet.CoinEngine;
import com.tangem.wallet.ECDSASignatureETH;
import com.tangem.wallet.EthTransaction;
import com.tangem.wallet.Keccak256;
import com.tangem.wallet.R;
import com.tangem.wallet.TangemContext;
import com.tangem.wallet.eth.EthData;

import org.bitcoinj.core.ECKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Created by Ilia on 20.03.2018.
 */

public class TokenEngine extends CoinEngine {

    private static final String TAG = TokenEngine.class.getSimpleName();
    public TokenData coinData = null;

    public TokenEngine(TangemContext ctx) throws Exception {
        super(ctx);
        if (ctx.getCoinData() == null) {
            coinData = new TokenData();
            ctx.setCoinData(coinData);
        } else if (ctx.getCoinData() instanceof TokenData) {
            coinData = (TokenData) ctx.getCoinData();
        } else if (ctx.getCoinData() instanceof EthData) {
            // special case with receive card data substitution from server at the moment
            Bundle B = new Bundle();
            ctx.getCoinData().saveToBundle(B);
            coinData = new TokenData();
            coinData.loadFromBundle(B);
            ctx.setCoinData(coinData);
        } else {
            throw new Exception("Invalid type of Blockchain data for " + this.getClass().getSimpleName());
        }
    }

    public TokenEngine() {
        super();
    }

    public Blockchain getBlockchain() {
        return Blockchain.Token;
    }

    protected int getChainIdNum() {
        return EthTransaction.ChainEnum.Mainnet.getValue();
    }

    @Override
    public boolean awaitingConfirmation() {
        return !coinData.getUnconfirmedTXCount().equals(coinData.getConfirmedTXCount()) || App.pendingTransactionsStorage.hasTransactions(ctx.getCard());
    }

    @Override
    public Amount getBalance() {
        if (!hasBalanceInfo()) {
            return null;
        }
        try {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                return convertToAmount(coinData.getBalanceInInternalUnits());
            } else {
                return convertToAmount(coinData.getBalanceAlterInInternalUnits());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getBalanceHTML() {
        if (hasBalanceInfo()) {
            try {
                return " " + convertToAmount(coinData.getBalanceInInternalUnits()).toDescriptionString(getTokenDecimals()) + "<br><small><small>+ " + convertToAmount(coinData.getBalanceAlterInInternalUnits()).toDescriptionString(getChainDecimals()) + " for fee</small></small>";
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    @Override
    public String getBalanceCurrency() {
        String currency = ctx.getCard().getTokenSymbol();
        if (Strings.isNullOrEmpty(currency))
            return "NoN";
        if (hasBalanceInfo()) {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                return currency;
            } else {
                return this.getBlockchain().getCurrency();
            }
        } else {
            return currency;
        }
    }

    @Override
    public InputFilter[] getAmountInputFilters() {
        if (!hasBalanceInfo()) return null;
        if (coinData.getBalanceInInternalUnits().notZero()) {
            return new InputFilter[]{new DecimalDigitsInputFilter(getTokenDecimals())};
        } else {
            return new InputFilter[]{new DecimalDigitsInputFilter(getChainDecimals())};
        }
    }

    @Override
    public String getFeeCurrency() {
        return this.getBlockchain().getCurrency();
    }

    protected static int getChainDecimals() {
        return 18;
    }

    protected int getTokenDecimals() {
        return ctx.getCard().getTokensDecimal();
    }

    protected String getContractAddress(TangemCard card) {
        return card.getContractAddress();
    }

    public boolean isNeedCheckNode() {
        return false;
    }

    @Override
    public boolean validateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }

        if (!address.startsWith("0x") && !address.startsWith("0X")) {
            return false;
        }

        if (address.length() != 42) {
            return false;
        }

        return true;
    }

    protected boolean isBalanceAlterNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceAlterInInternalUnits() == null) return false;
        return coinData.getBalanceAlterInInternalUnits().notZero();
    }

    @Override
    public boolean isBalanceNotZero() {
        if (coinData == null) return false;
        if (coinData.getBalanceInInternalUnits() == null && coinData.getBalanceAlterInInternalUnits() == null)
            return false;
        return (coinData.getBalanceInInternalUnits() != null && coinData.getBalanceInInternalUnits().notZero() ) ||
                (coinData.getBalanceAlterInInternalUnits() != null && coinData.getBalanceAlterInInternalUnits().notZero());
    }


    @Override
    public String getBalanceEquivalent() {
        if (!hasBalanceInfo()) {
            return "";
        }
        try {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                // TODO: check why Rate=EthRate
                return "";//convertToAmount(coinData.getBalanceInInternalUnits()).toEquivalentString(coinData.getRate());
            } else {
                if (coinData.getBalanceAlterInInternalUnits() == null) return "";
                return convertToAmount(coinData.getBalanceAlterInInternalUnits()).toEquivalentString(coinData.getRateAlter());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String calculateAddress(byte[] pkUncompressed) {
        Keccak256 kec = new Keccak256();
        int lenPk = pkUncompressed.length;
        if (lenPk < 2) {
            throw new IllegalArgumentException("Uncompress public key length is invalid");
        }
        byte[] cleanKey = new byte[lenPk - 1];
        for (int i = 0; i < cleanKey.length; ++i) {
            cleanKey[i] = pkUncompressed[i + 1];
        }
        byte[] r = kec.digest(cleanKey);

        byte[] address = new byte[20];
        for (int i = 0; i < 20; ++i) {
            address[i] = r[i + 12];
        }

        return String.format("0x%s", BTCUtils.toHex(address));
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) throws Exception {
        if (internalAmount.getCurrency().equals("wei")) {
            BigDecimal d = internalAmount.divide(new BigDecimal("1000000000000000000"), getChainDecimals(), RoundingMode.DOWN);
            return new Amount(d, this.getBlockchain().getCurrency());
        } else if (internalAmount.getCurrency().equals(ctx.getCard().getTokenSymbol())) {
            BigDecimal p = new BigDecimal(10);
            p = p.pow(getTokenDecimals());
            BigDecimal d = internalAmount.divide(p);
            return new Amount(d, ctx.getCard().getTokenSymbol());
        }
        throw new Exception(String.format("Can't convert '%s' to '%s'", internalAmount.getCurrency(), ctx.getCard().getTokenSymbol()));
    }

    @Override
    public Amount convertToAmount(String strAmount, String currency) {
        return new Amount(strAmount, currency);
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        if (amount.getCurrency().equals(this.getBlockchain().getCurrency())) {
            BigDecimal d = amount.multiply(new BigDecimal("1000000000000000000"));
            return new InternalAmount(d, "wei");
        } else if (amount.getCurrency().equals(ctx.getCard().getTokenSymbol())) {
            BigDecimal p = new BigDecimal(10);
            p = p.pow(getTokenDecimals());
            BigDecimal d = amount.multiply(p);
            return new InternalAmount(d, ctx.getCard().getTokenSymbol());
        }
        throw new Exception(String.format("Can't convert '%s' to '%s'", amount.getCurrency(), ctx.getCard().getTokenSymbol()));
    }

    @Override
    public InternalAmount convertToInternalAmount(byte[] bytes) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public byte[] convertToByteArray(InternalAmount amount) throws Exception {
        throw new Exception("Not implemented");
    }

    @Override
    public CoinData createCoinData() {
        return new TokenData();
    }

    @Override
    public String getUnspentInputsDescription() {
        return "";
    }

//    @Override
//    public String getFeeEquivalentDescriptor(String value) {
//        BigDecimal d = new BigDecimal(value);
//        return EthEngine.getAmountEquivalentDescription(d, coinData.getRateAlter());
//    }

    @Override
    public boolean hasBalanceInfo() {
        return coinData != null && coinData.getBalanceInInternalUnits() != null && coinData.getBalanceAlterInInternalUnits() != null;
    }


    @Override
    public Uri getWalletExplorerUri() {
        return Uri.parse("https://etherscan.io/token/" + getContractAddress(ctx.getCard()) + "?a=" + ctx.getCoinData().getWallet());
    }

    @Override
    public Uri getShareWalletUri() {
        if (ctx.getCard().getDenomination() != null) {
            return Uri.parse("ethereum:" + ctx.getCoinData().getWallet());// + "?value=" + mCard.getDenomination() +"e18");
        } else {
            return Uri.parse("ethereum:" + ctx.getCoinData().getWallet());
        }
    }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.general_wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait);
        } else if (!isBalanceAlterNotZero()) {
            ctx.setMessage(ctx.getString(R.string.confirm_transaction_error_not_enough_eth_for_fee));
        } else {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (!hasBalanceInfo()) return false;
        Amount balance;
        try {
            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                balance = convertToAmount(coinData.getBalanceInInternalUnits());
            } else if (amount.getCurrency().equals(this.getBlockchain().getCurrency()) && coinData.getBalanceInInternalUnits().isZero()) {
                balance = convertToAmount(coinData.getBalanceAlterInInternalUnits());
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return amount.compareTo(balance) <= 0;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        if (!hasBalanceInfo()) return false;

        try {
            Amount balanceETH = convertToAmount(coinData.getBalanceAlterInInternalUnits());

            if (fee == null || amount == null || fee.isZero() || amount.isZero())
                return false;


            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                // token transaction
                if (fee.compareTo(balanceETH) > 0)
                    return false;
            } else if (amount.getCurrency().equals(this.getBlockchain().getCurrency()) && coinData.getBalanceInInternalUnits().isZero()) {
                // standard ETH transaction
//                try {
                if (isFeeIncluded && (amount.compareTo(balanceETH) > 0 || fee.compareTo(balanceETH) > 0))
                    return false;

                if (!isFeeIncluded && amount.add(fee).compareTo(balanceETH) > 0)
                    return false;

//                } catch (NumberFormatException e) {
//                    e.printStackTrace();
//                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public boolean validateBalance(BalanceValidator balanceValidator) {
        if (getBalance() == null) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_unknown_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance);
            return false;
        }

        if (!coinData.getUnconfirmedTXCount().equals(coinData.getConfirmedTXCount())) {
            balanceValidator.setScore(0);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_transaction_in_progress);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_wait_for_confirmation);
            return false;
        }

        if (coinData.isBalanceReceived()) {
            balanceValidator.setScore(100);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_balance);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain);
            if (getBalance().isZero()) {
                balanceValidator.setFirstLine(R.string.balance_validator_first_line_empty_wallet);
                balanceValidator.setSecondLine(R.string.empty_string);
            }
        }

        if ((ctx.getCard().getOfflineBalance() != null) && !coinData.isBalanceReceived() && (ctx.getCard().getRemainingSignatures() == ctx.getCard().getMaxSignatures()) && getBalance().notZero()) {
            balanceValidator.setScore(80);
            balanceValidator.setFirstLine(R.string.balance_validator_first_line_verified_offline);
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_verify_online);
        }

        return true;
    }


    @Override
    public String evaluateFeeEquivalent(String fee) {
        try {
            Amount feeValue = new Amount(fee, getFeeCurrency());
            return feeValue.toEquivalentString(coinData.getRate());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

//
//        BigDecimal gweFee = new BigDecimal(fee);
//        gweFee = gweFee.divide(new BigDecimal("1000000000"));
//        gweFee = gweFee.setScale(18, RoundingMode.DOWN);
//        return getFeeEquivalentDescriptor(mCard, gweFee.toString());
    }

    @Override
    public SignTask.TransactionToSign constructTransaction(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        if (amountValue.getCurrency().equals(this.getBlockchain().getCurrency())) {
            return constructTransactionCoin(feeValue, amountValue, IncFee, targetAddress);
        } else {
            return constructTransactionToken(feeValue, amountValue, IncFee, targetAddress);
        }
    }

    protected SignTask.TransactionToSign constructTransactionCoin(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception {
        Log.e(TAG, "Construct " + this.getBlockchain().getCurrency() + " payment " + amountValue.toString() + " with fee " + feeValue.toString() + (IncFee ? " including" : " excluding"));

        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();
        BigInteger weiAmount = convertToInternalAmount(amountValue).toBigIntegerExact();

        if (IncFee) {
            weiAmount = weiAmount.subtract(weiFee);
        }

        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(21000));
        BigInteger gasLimit = BigInteger.valueOf(21000);
        Integer chainId = this.getChainIdNum(); // Token support on main net only!!!

        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        final EthTransaction tx = EthTransaction.create(to, weiAmount, nonceValue, gasPrice, gasLimit, chainId);

        return new SignTask.TransactionToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

                if (!f) {
                    Log.e(this.getClass().getSimpleName() + "-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
                if (v != 27 && v != 28) {
                    Log.e(TAG, "invalid v");
                    throw new Exception("Error in " + this.getClass().getSimpleName() + " - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e(TAG, this.getClass().getSimpleName() + " V: " + String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;
            }
        };
    }

    protected SignTask.TransactionToSign constructTransactionToken(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception {
        Log.e(TAG, "Construct TOKEN transaction " + amountValue.toString() + " with fee " + feeValue.toString() + (IncFee ? " including" : " excluding"));

        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();
//        boolean flag = (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer);
//        Issuer issuer = ctx.getCard().getIssuer();


//        BigInteger gigaK = BigInteger.valueOf(1000000000L);

        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();

        InternalAmount amountDec = convertToInternalAmount(amountValue);
        BigInteger amount = amountDec.toBigInteger(); //new BigInteger(amountValue, 10);


        int gasLimitInt = 60000;
        if (amountValue.getCurrency().equals("DGX") || amountValue.getCurrency().equals("CGT")) {
            gasLimitInt = 300000;
        }

        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(gasLimitInt));
        BigInteger gasLimit = BigInteger.valueOf(gasLimitInt);
        Integer chainId = this.getChainIdNum();
        BigInteger amountZero = BigInteger.ZERO;
        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        String contractAddress = getContractAddress(ctx.getCard());

        if (contractAddress.startsWith("0x") || contractAddress.startsWith("0X")) {
            contractAddress = contractAddress.substring(2);
        }

        String amountLeadZero = amount.toString(16);
        if (amountLeadZero.startsWith("0x") || amountLeadZero.startsWith("0X")) {
            amountLeadZero = amountLeadZero.substring(2);
        }

        while (amountLeadZero.length() < 64) {
            amountLeadZero = "0" + amountLeadZero;
        }

        String cmd = "a9059cbb000000000000000000000000" + to + amountLeadZero; //TODO only for BAT


        byte[] data = BTCUtils.fromHex(cmd);
        EthTransaction tx = EthTransaction.create(contractAddress, amountZero, nonceValue, gasPrice, gasLimit, chainId, data);

        return new SignTask.TransactionToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for " + this.getClass().getSimpleName());
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

                if (!f) {
                    Log.e(this.getClass().getSimpleName() + "-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
                if (v != 27 && v != 28) {
                    Log.e(TAG, "invalid v");
                    throw new Exception("Error in " + this.getClass().getSimpleName() + " - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e(TAG, this.getClass().getSimpleName() + " V: " + String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendTransaction(txForSend);
                return txForSend;

            }
        };

    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiInfura serverApiInfura = new ServerApiInfura();
        final ServerApiBlockcypher serverApiBlockcypher = new ServerApiBlockcypher();

        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                switch (method) {
                    case ServerApiInfura.INFURA_ETH_GET_BALANCE: {
                        String balanceCap = infuraResponse.getResult();
                        balanceCap = balanceCap.substring(2);
                        BigInteger l = new BigInteger(balanceCap, 16);
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceAlterInInternalUnits(new CoinEngine.InternalAmount(l, "wei"));

//                        Log.i("$TAG eth_get_balance", balanceCap)
                    }
                    break;

                    case ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT: {
                        String nonce = infuraResponse.getResult();
                        nonce = nonce.substring(2);
                        BigInteger count = new BigInteger(nonce, 16);
                        coinData.setConfirmedTXCount(count);


//                        Log.i("$TAG eth_getTransCount", nonce)
                    }
                    break;

                    case ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT: {
                        String pending = infuraResponse.getResult();
                        pending = pending.substring(2);
                        BigInteger count = new BigInteger(pending, 16);
                        coinData.setUnconfirmedTXCount(count);

//                        Log.i("$TAG eth_getPendingTxCount", pending)
                    }
                    break;
//
                    case ServerApiInfura.INFURA_ETH_CALL: {
                        try {

                            String balanceCap = infuraResponse.getResult();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            coinData.setBalanceInInternalUnits(new CoinEngine.InternalAmount(l, ctx.getCard().tokenSymbol));
//                              Log.i("$TAG eth_call", balanceCap)


                            if (blockchainRequestsCallbacks.allowAdvance()) {
                                serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_GET_BALANCE, 67, coinData.getWallet(), "", "");
                                serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_GET_TRANSACTION_COUNT, 67, coinData.getWallet(), "", "");
                                serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_GET_PENDING_COUNT, 67, coinData.getWallet(), "", "");
                            } else {
                                ctx.setError("Terminated by user");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                }
                if (serverApiInfura.isRequestsSequenceCompleted()&& serverApiBlockcypher.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String method, String message) {
                Log.e(TAG, "onFail: " + method + " " + message);
                ctx.setError(message);
                if (serverApiInfura.isRequestsSequenceCompleted()&& serverApiBlockcypher.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(false);
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }
        };
        serverApiInfura.setResponseListener(responseListener);

        ServerApiBlockcypher.ResponseListener blockcypherListener = new ServerApiBlockcypher.ResponseListener() {
            @Override
            public void onSuccess(String method, BlockcypherResponse blockcypherResponse) {
                Log.i(TAG, "onSuccess: " + method);
                try {
                    //TODO: change request logic, 2000 tx max
                    if (blockcypherResponse.getTxrefs() != null) {
                        for (BlockcypherTxref txref : blockcypherResponse.getTxrefs()) {
                            if (txref.getTx_input_n() != -1) { //sent only
                                coinData.incSentTransactionsCount();
                            }
                        }
                    }

                    if (blockcypherResponse.getUnconfirmed_txrefs() != null) {
                        for (BlockcypherTxref unconfirmedTxref : blockcypherResponse.getUnconfirmed_txrefs()) {
                            if (unconfirmedTxref.getTx_input_n() != -1) {
                                coinData.incSentTransactionsCount();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "FAIL BLOCKCYPHER_ADDRESS Exception");
                }

                if (serverApiInfura.isRequestsSequenceCompleted()&& serverApiBlockcypher.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            public void onSuccess(String method, BlockcypherFee blockcypherFee) {
                Log.e(TAG, "Wrong response type for requestBalanceAndUnspentTransactions");
            }

            @Override
            public void onFail(String method, String message) {
                Log.i(TAG, "onFail: " + method + " " + message);
            }
        };
        serverApiBlockcypher.setResponseListener(blockcypherListener);

        if (validateAddress(getContractAddress(ctx.getCard()))) {
            serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_CALL, 67, coinData.getWallet(), getContractAddress(ctx.getCard()), "");
        } else {
            ctx.setError("Smart contract address not defined");
            blockchainRequestsCallbacks.onComplete(false);
        }

        serverApiBlockcypher.requestData(ctx.getBlockchain().getID(), ServerApiBlockcypher.BLOCKCYPHER_ADDRESS, ctx.getCoinData().getWallet(), "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request requestData eth gasPrice listener
        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                String gasPrice = infuraResponse.getResult();
                gasPrice = gasPrice.substring(2);

                // rounding gas price to integer gwei
                BigInteger l = new BigInteger(gasPrice, 16);

                Log.i(TAG, "Infura gas price: " + gasPrice + " (" + l.toString() + ")");
                BigInteger m;
                if (!amount.getCurrency().equals(Blockchain.Ethereum.getCurrency()))
                    if (amount.getCurrency().equals("DGX") || amount.getCurrency().equals("CGT")) {
                        m = BigInteger.valueOf(300000);
                    } else {
                        m = BigInteger.valueOf(60000);
                    }
                else m = BigInteger.valueOf(21000);

                Log.i(TAG, "fee multiplier: " + m.toString());

                CoinEngine.InternalAmount weiMinFee = new CoinEngine.InternalAmount(l.multiply(m), "wei");
                CoinEngine.InternalAmount weiNormalFee = new CoinEngine.InternalAmount(l.multiply(BigInteger.valueOf(12)).divide(BigInteger.valueOf(10)).multiply(m), "wei");
                CoinEngine.InternalAmount weiMaxFee = new CoinEngine.InternalAmount(l.multiply(BigInteger.valueOf(15)).divide(BigInteger.valueOf(10)).multiply(m), "wei");
                Log.i(TAG, "min fee   : " + weiMinFee.toValueString() + " wei");
                Log.i(TAG, "normal fee: " + weiNormalFee.toValueString() + " wei");
                Log.i(TAG, "max fee   : " + weiMaxFee.toValueString() + " wei");

                try {
                    coinData.minFee = convertToAmount(weiMinFee);
                    coinData.normalFee = convertToAmount(weiNormalFee);
                    coinData.maxFee = convertToAmount(weiMaxFee);
                    Log.i(TAG, "min fee   : " + coinData.minFee.toString());
                    Log.i(TAG, "normal fee: " + coinData.normalFee.toString());
                    Log.i(TAG, "max fee   : " + coinData.maxFee.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onFail(String method, String message) {
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiInfura.setResponseListener(responseListener);

        serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_GAS_PRICE, 67, coinData.getWallet(), "", "");
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {

        String txStr = String.format("0x%s", BTCUtils.toHex(txForSend));

        ServerApiInfura serverApiInfura = new ServerApiInfura();
        // request requestData eth gasPrice listener
        ServerApiInfura.ResponseListener responseListener = new ServerApiInfura.ResponseListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                if (method.equals(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION)) {
                    if (infuraResponse.getResult()==null || infuraResponse.getResult().isEmpty()) {
                        ctx.setError("Rejected by node: " + infuraResponse.getError());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        BigInteger nonce = coinData.getConfirmedTXCount();
                        nonce = nonce.add(BigInteger.valueOf(1));
                        coinData.setConfirmedTXCount(nonce);
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (method.equals(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION)) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };

        serverApiInfura.setResponseListener(responseListener);

        serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION, 67, coinData.getWallet(), "", txStr);

    }

    @Override
    public boolean needMultipleLinesForBalance() {
        return true;
    }

    @Override
    public boolean allowSelectFeeInclusion() {
        return getBalance().getCurrency().equals(Blockchain.Ethereum.getCurrency());
    }

    public int pendingTransactionTimeoutInSeconds() { return 10; }
}
