package com.timesheet.test.flow;

import com.google.common.collect.ImmutableList;
import com.timesheet.flow.IssueInvoiceFlow;
import com.timesheet.flow.PayInvoiceFlow;
import com.timesheet.state.InvoiceState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.SignatureException;
import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PayInvoiceFlowTests {
    private static final CordaX500Name BOC_NAME = new CordaX500Name("BankOfCorda", "London", "GB");
    MockNetwork network;
    StartedMockNode contractor;
    StartedMockNode megaCorp;
    StartedMockNode bankOfCordaNode;
    StartedMockNode oracle;
    LocalDate today;
    byte b = 1;
    OpaqueBytes dummyRef = OpaqueBytes.of(b);

    @Before
    public void setup() {
        network = new MockNetwork(new MockNetworkParameters(
                ImmutableList.of(
                        TestCordapp.findCordapp("com.timesheet.contract"),
                        TestCordapp.findCordapp("com.timesheet.flow"),
                        TestCordapp.findCordapp("net.corda.finance.contracts.asset")
                )
        ));
        CordaX500Name oracleName = new CordaX500Name("Oracle", "London", "GB");
        contractor = network.createPartyNode(null);
        megaCorp = network.createPartyNode(null);
        oracle = network.createPartyNode(oracleName);
        bankOfCordaNode = network.createPartyNode(BOC_NAME);
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        ImmutableList.of(contractor, megaCorp).forEach(it -> {
            it.registerInitiatedFlow(IssueInvoiceFlow.Acceptor.class);
        });
        network.runNetwork();
        today = LocalDate.now();
    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    public InvoiceState submitInvoice(int hoursWorked) throws ExecutionException, InterruptedException {
        Future future = contractor.startFlow(new IssueInvoiceFlow.Initiator(hoursWorked, today, megaCorp.getInfo().getLegalIdentities().get(0)));
        network.runNetwork();
        SignedTransaction signedTransaction = (SignedTransaction) future.get();
        return (InvoiceState) signedTransaction.getTx().getOutputStates().get(0);
    }

    @Test
    public void SignedTransactionReturnedByTheFlowIsOnlySignedByTheInitiator() throws ExecutionException, InterruptedException, SignatureException {
        InvoiceState invoice = submitInvoice(8);

        PayInvoiceFlow.Initiator flow = new PayInvoiceFlow.Initiator(invoice.getLinearID().getId());
        Future future = megaCorp.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();
        stx.verifySignaturesExcept(contractor.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorage() throws ExecutionException, InterruptedException {
        InvoiceState invoice = submitInvoice(8);

        PayInvoiceFlow.Initiator flow = new PayInvoiceFlow.Initiator(invoice.getLinearID().getId());
        Future future = megaCorp.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();

        Assert.assertEquals(stx, megaCorp.getServices().getValidatedTransactions().getTransaction(stx.getId()));
        Assert.assertEquals(stx, contractor.getServices().getValidatedTransactions().getTransaction(stx.getId()));

    }

    @Test
    public void recordedTransactionHasTheCorrectValuesForThePayments() throws ExecutionException, InterruptedException {
        int hours = 8;
        InvoiceState invoice = submitInvoice(hours);

        PayInvoiceFlow.Initiator flow = new PayInvoiceFlow.Initiator(invoice.getLinearID().getId());
        Future future = megaCorp.startFlow(flow);
        network.runNetwork();

        SignedTransaction stx = (SignedTransaction) future.get();

        for (StartedMockNode node: ImmutableList.of(contractor, megaCorp)
        ) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions().getTransaction(stx.getId());

            InvoiceState recordedInvoice = recordedTx.getTx().outputsOfType(InvoiceState.class).get(0);
            Assert.assertEquals(recordedInvoice.getHoursWorked(), hours);
            Assert.assertEquals(recordedInvoice.getContractor(), contractor.getInfo().getLegalIdentities().get(0));
            Assert.assertEquals(recordedInvoice.getCompany(), megaCorp.getInfo().getLegalIdentities().get(0));


        }

    }
}
