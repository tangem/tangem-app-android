package com.tangem.lib.visa;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.5.2.
 */
@SuppressWarnings("rawtypes")
class TangemPaymentAccountRegistry extends Contract {
    public static final String BINARY = "608060405234801561001057600080fd5b50604051610c47380380610c4783398101604081905261002f91610054565b600080546001600160a01b0319166001600160a01b0392909216919091179055610084565b60006020828403121561006657600080fd5b81516001600160a01b038116811461007d57600080fd5b9392505050565b610bb4806100936000396000f3fe608060405234801561001057600080fd5b506004361061007d5760003560e01c806368d7111a1161005b57806368d7111a146100f0578063747fc3701461010357806379da564f14610116578063c45a01551461013657600080fd5b80630cef7172146100825780633509c7c5146100c85780635cd26737146100dd575b600080fd5b6100ab6100903660046108fd565b6002602052600090815260409020546001600160a01b031681565b6040516001600160a01b0390911681526020015b60405180910390f35b6100db6100d6366004610918565b610149565b005b6100db6100eb366004610918565b610201565b6100db6100fe3660046108fd565b6103b0565b6100db610111366004610918565b610506565b6101296101243660046108fd565b6106db565b6040516100bf919061094b565b6000546100ab906001600160a01b031681565b600054604080516080810190915260428082526001600160a01b03909216331491610a576020830139906101995760405162461bcd60e51b81526004016101909190610998565b60405180910390fd5b506001600160a01b03811660009081526001602052604090206101bc9083610705565b50806001600160a01b0316826001600160a01b03167f65b303fd420aefd0541d9300bb10190bb8e3320a0e7c2ce4dec6b6bfb953027f60405160405180910390a35050565b6000546040516385bb392360e01b81523360048201526001600160a01b03909116906385bb392390602401602060405180830381865afa158015610249573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061026d91906109e7565b6040518060600160405280603a8152602001610b45603a9139906102a45760405162461bcd60e51b81526004016101909190610998565b506001600160a01b038216600090815260016020526040902033906102c99082610721565b6040518060600160405280603b8152602001610ad0603b9139906103005760405162461bcd60e51b81526004016101909190610998565b506001600160a01b03821660009081526001602052604090206103239082610705565b6040518060600160405280603b8152602001610ad0603b91399061035a5760405162461bcd60e51b81526004016101909190610998565b50604080516001600160a01b03808416825280861660208301528416918101919091527ffba993cae385d1da628b11b1b150a27262bca00bb2674980516b45ad7d331eaa906060015b60405180910390a1505050565b6000546040516385bb392360e01b81523360048201526001600160a01b03909116906385bb392390602401602060405180830381865afa1580156103f8573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061041c91906109e7565b6040518060600160405280603a8152602001610b45603a9139906104535760405162461bcd60e51b81526004016101909190610998565b506001600160a01b0381811660009081526002602090815260409182902054825160608101909352603a8084529316159290610b0b90830139906104aa5760405162461bcd60e51b81526004016101909190610998565b506001600160a01b03811660008181526002602052604080822080546001600160a01b03191633908117909155905190929183917f02e4debb2ab36299bfcc998345f66f73428adbbda9ef5efef399d75a67a4b78a9190a35050565b6000546040516385bb392360e01b81523360048201526001600160a01b03909116906385bb392390602401602060405180830381865afa15801561054e573d6000803e3d6000fd5b505050506040513d601f19601f8201168201806040525081019061057291906109e7565b6040518060600160405280603a8152602001610b45603a9139906105a95760405162461bcd60e51b81526004016101909190610998565b506001600160a01b0382811660009081526002602090815260409182902054825160608101909352603780845233949190911684149291610a9990830139906106055760405162461bcd60e51b81526004016101909190610998565b506001600160a01b0382811660009081526002602090815260409182902054825160608101909352603a8084529316159290610b0b908301399061065c5760405162461bcd60e51b81526004016101909190610998565b506001600160a01b03838116600081815260026020908152604080832080546001600160a01b03199081169091558786168085529382902080549091169587169586179055805194855290840192909252908201527f362e9cf018894fbf5dfe9c5cbfb25c3e3ac8c4d5beed564352f1af2e2712dfc8906060016103a3565b6001600160a01b03811660009081526001602052604090206060906106ff90610736565b92915050565b600061071a836001600160a01b038416610743565b9392505050565b600061071a836001600160a01b038416610792565b6060600061071a83610885565b600081815260018301602052604081205461078a575081546001818101845560008481526020808220909301849055845484825282860190935260409020919091556106ff565b5060006106ff565b6000818152600183016020526040812054801561087b5760006107b6600183610a09565b85549091506000906107ca90600190610a09565b905080821461082f5760008660000182815481106107ea576107ea610a2a565b906000526020600020015490508087600001848154811061080d5761080d610a2a565b6000918252602080832090910192909255918252600188019052604090208390555b855486908061084057610840610a40565b6001900381819060005260206000200160009055905585600101600086815260200190815260200160002060009055600193505050506106ff565b60009150506106ff565b6060816000018054806020026020016040519081016040528092919081815260200182805480156108d557602002820191906000526020600020905b8154815260200190600101908083116108c1575b50505050509050919050565b80356001600160a01b03811681146108f857600080fd5b919050565b60006020828403121561090f57600080fd5b61071a826108e1565b6000806040838503121561092b57600080fd5b610934836108e1565b9150610942602084016108e1565b90509250929050565b6020808252825182820181905260009190848201906040850190845b8181101561098c5783516001600160a01b031683529284019291840191600101610967565b50909695505050505050565b60006020808352835180602085015260005b818110156109c6578581018301518582016040015282016109aa565b506000604082860101526040601f19601f8301168501019250505092915050565b6000602082840312156109f957600080fd5b8151801515811461071a57600080fd5b818103818111156106ff57634e487b7160e01b600052601160045260246000fd5b634e487b7160e01b600052603260045260246000fd5b634e487b7160e01b600052603160045260246000fdfe353630307c52656769737472793a206f6e6c79207061796d656e74206163636f756e7420666163746f72792063616e2063616c6c20746869732066756e6374696f6e353630347c52656769737472793a2070726576696f757320636172642773207061796d656e74206163636f756e74206d69736d61746368353630327c52656769737472793a207061796d656e74206163636f756e74206265696e672072656d6f766564206e6f7420696e2074686520736574353630347c52656769737472793a207061796d656e74206163636f756e7420697320616c72656164792073657420666f72207468652063617264353630317c52656769737472793a206f6e6c79207061796d656e74206163636f756e742063616e2063616c6c20746869732066756e6374696f6ea26469706673582212203c97923f6f14a45f904087dcdced3b09f33cd32821438cd9cbf0add5824acaf064736f6c63430008160033";

    public static final String FUNC_CHANGEPAYMENTACCOUNTCARD = "changePaymentAccountCard";

    public static final String FUNC_CHANGEPAYMENTACCOUNTOWNER = "changePaymentAccountOwner";

    public static final String FUNC_FACTORY = "factory";

    public static final String FUNC_INITPAYMENTACCOUNTCARD = "initPaymentAccountCard";

    public static final String FUNC_INITPAYMENTACCOUNTOWNER = "initPaymentAccountOwner";

    public static final String FUNC_PAYMENTACCOUNTBYCARD = "paymentAccountByCard";

    public static final String FUNC_PAYMENTACCOUNTSBYOWNER = "paymentAccountsByOwner";

    public static final Event PAYMENTACCOUNTCARDCHANGED_EVENT = new Event("PaymentAccountCardChanged",
            Arrays.asList(new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }));

    public static final Event PAYMENTACCOUNTCARDREGISTERED_EVENT = new Event("PaymentAccountCardRegistered",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));

    public static final Event PAYMENTACCOUNTOWNERCHANGED_EVENT = new Event("PaymentAccountOwnerChanged",
            Arrays.asList(new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }));

    public static final Event PAYMENTACCOUNTOWNERREGISTERED_EVENT = new Event("PaymentAccountOwnerRegistered",
            Arrays.asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));

    @Deprecated
    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TangemPaymentAccountRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<PaymentAccountCardChangedEventResponse> getPaymentAccountCardChangedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTACCOUNTCARDCHANGED_EVENT, transactionReceipt);
        ArrayList<PaymentAccountCardChangedEventResponse> responses = new ArrayList<PaymentAccountCardChangedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentAccountCardChangedEventResponse typedResponse = new PaymentAccountCardChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.previousCard = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.newCard = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentAccountCardChangedEventResponse getPaymentAccountCardChangedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTACCOUNTCARDCHANGED_EVENT, log);
        PaymentAccountCardChangedEventResponse typedResponse = new PaymentAccountCardChangedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.previousCard = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.newCard = (String) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public static List<PaymentAccountCardRegisteredEventResponse> getPaymentAccountCardRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTACCOUNTCARDREGISTERED_EVENT, transactionReceipt);
        ArrayList<PaymentAccountCardRegisteredEventResponse> responses = new ArrayList<PaymentAccountCardRegisteredEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentAccountCardRegisteredEventResponse typedResponse = new PaymentAccountCardRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.card = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentAccountCardRegisteredEventResponse getPaymentAccountCardRegisteredEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTACCOUNTCARDREGISTERED_EVENT, log);
        PaymentAccountCardRegisteredEventResponse typedResponse = new PaymentAccountCardRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.card = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public static List<PaymentAccountOwnerChangedEventResponse> getPaymentAccountOwnerChangedEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTACCOUNTOWNERCHANGED_EVENT, transactionReceipt);
        ArrayList<PaymentAccountOwnerChangedEventResponse> responses = new ArrayList<PaymentAccountOwnerChangedEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentAccountOwnerChangedEventResponse typedResponse = new PaymentAccountOwnerChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.previousOwner = (String) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.newOwner = (String) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentAccountOwnerChangedEventResponse getPaymentAccountOwnerChangedEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTACCOUNTOWNERCHANGED_EVENT, log);
        PaymentAccountOwnerChangedEventResponse typedResponse = new PaymentAccountOwnerChangedEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.previousOwner = (String) eventValues.getNonIndexedValues().get(1).getValue();
        typedResponse.newOwner = (String) eventValues.getNonIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public static List<PaymentAccountOwnerRegisteredEventResponse> getPaymentAccountOwnerRegisteredEvents(TransactionReceipt transactionReceipt) {
        List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(PAYMENTACCOUNTOWNERREGISTERED_EVENT, transactionReceipt);
        ArrayList<PaymentAccountOwnerRegisteredEventResponse> responses = new ArrayList<PaymentAccountOwnerRegisteredEventResponse>(valueList.size());
        for (EventValuesWithLog eventValues : valueList) {
            PaymentAccountOwnerRegisteredEventResponse typedResponse = new PaymentAccountOwnerRegisteredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static PaymentAccountOwnerRegisteredEventResponse getPaymentAccountOwnerRegisteredEventFromLog(Log log) {
        EventValuesWithLog eventValues = staticExtractEventParametersWithLog(PAYMENTACCOUNTOWNERREGISTERED_EVENT, log);
        PaymentAccountOwnerRegisteredEventResponse typedResponse = new PaymentAccountOwnerRegisteredEventResponse();
        typedResponse.log = log;
        typedResponse.paymentAccount = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.owner = (String) eventValues.getIndexedValues().get(1).getValue();
        return typedResponse;
    }

    @Deprecated
    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TangemPaymentAccountRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TangemPaymentAccountRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(List.of(new Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(List.of(new Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(List.of(new Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TangemPaymentAccountRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String factory_) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(List.of(new Address(160, factory_)));
        return deployRemoteCall(TangemPaymentAccountRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public Flowable<PaymentAccountCardChangedEventResponse> paymentAccountCardChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentAccountCardChangedEventFromLog(log));
    }

    public Flowable<PaymentAccountCardChangedEventResponse> paymentAccountCardChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTACCOUNTCARDCHANGED_EVENT));
        return paymentAccountCardChangedEventFlowable(filter);
    }

    public Flowable<PaymentAccountCardRegisteredEventResponse> paymentAccountCardRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentAccountCardRegisteredEventFromLog(log));
    }

    public Flowable<PaymentAccountCardRegisteredEventResponse> paymentAccountCardRegisteredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTACCOUNTCARDREGISTERED_EVENT));
        return paymentAccountCardRegisteredEventFlowable(filter);
    }

    public Flowable<PaymentAccountOwnerChangedEventResponse> paymentAccountOwnerChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentAccountOwnerChangedEventFromLog(log));
    }

    public Flowable<PaymentAccountOwnerChangedEventResponse> paymentAccountOwnerChangedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTACCOUNTOWNERCHANGED_EVENT));
        return paymentAccountOwnerChangedEventFlowable(filter);
    }

    public Flowable<PaymentAccountOwnerRegisteredEventResponse> paymentAccountOwnerRegisteredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getPaymentAccountOwnerRegisteredEventFromLog(log));
    }

    public Flowable<PaymentAccountOwnerRegisteredEventResponse> paymentAccountOwnerRegisteredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAYMENTACCOUNTOWNERREGISTERED_EVENT));
        return paymentAccountOwnerRegisteredEventFlowable(filter);
    }

    public RemoteFunctionCall<TransactionReceipt> changePaymentAccountCard(String previousCard, String newCard) {
        final Function function = new Function(
                FUNC_CHANGEPAYMENTACCOUNTCARD,
                Arrays.asList(new Address(160, previousCard),
                        new Address(160, newCard)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changePaymentAccountOwner(String previousOwner, String newOwner) {
        final Function function = new Function(
                FUNC_CHANGEPAYMENTACCOUNTOWNER,
                Arrays.asList(new Address(160, previousOwner),
                        new Address(160, newOwner)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> factory() {
        final Function function = new Function(FUNC_FACTORY,
                List.of(),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> initPaymentAccountCard(String card) {
        final Function function = new Function(
                FUNC_INITPAYMENTACCOUNTCARD,
                List.of(new Address(160, card)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initPaymentAccountOwner(String paymentAccount, String owner) {
        final Function function = new Function(
                FUNC_INITPAYMENTACCOUNTOWNER,
                Arrays.asList(new Address(160, paymentAccount),
                        new Address(160, owner)),
                Collections.emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> paymentAccountByCard(String param0) {
        final Function function = new Function(FUNC_PAYMENTACCOUNTBYCARD,
                List.of(new Address(160, param0)),
                List.of(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<List> paymentAccountsByOwner(String owner) {
        final Function function = new Function(FUNC_PAYMENTACCOUNTSBYOWNER,
                List.of(new Address(160, owner)),
                List.of(new TypeReference<DynamicArray<Address>>() {
                }));
        return new RemoteFunctionCall<List>(function,
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public static class PaymentAccountCardChangedEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String previousCard;

        public String newCard;
    }

    public static class PaymentAccountCardRegisteredEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String card;
    }

    public static class PaymentAccountOwnerChangedEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String previousOwner;

        public String newOwner;
    }

    public static class PaymentAccountOwnerRegisteredEventResponse extends BaseEventResponse {
        public String paymentAccount;

        public String owner;
    }
}
