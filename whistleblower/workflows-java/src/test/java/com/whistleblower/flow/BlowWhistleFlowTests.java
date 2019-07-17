package com.whistleblower.flow;

import com.google.common.collect.ImmutableList;
import com.whistleblower.state.BlowWhistleState;
import net.corda.core.contracts.AttachmentResolutionException;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class BlowWhistleFlowTests {
    private MockNetwork network;
    private StartedMockNode whistleBlower;
    private StartedMockNode firstInvestigator;
    private StartedMockNode badCompany;

    protected Party partyFromAnonymous(StartedMockNode node, AnonymousParty anonParty){
        return node.getServices().getIdentityService().wellKnownPartyFromAnonymous(anonParty);
    }

    @Before
    public void setup(){
        network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
                TestCordapp.findCordapp("com.whistleblower.contract"),
                TestCordapp.findCordapp("com.whistleblower.flow"))));
        whistleBlower = network.createNode();
        firstInvestigator = network.createNode();
        badCompany = network.createNode();
        ImmutableList list = ImmutableList.of(whistleBlower,firstInvestigator,badCompany);
        for (Object node : list) {
            StartedMockNode mockNode = (StartedMockNode) node;
            mockNode.registerInitiatedFlow(BlowWhistleFlowResponder.class);
        }
        network.runNetwork();
    }

    @After
    public void tearDown(){
        network.stopNodes();
    }

    protected SignedTransaction blowWhistle() throws ExecutionException, InterruptedException {
        BlowWhistleFlow flow = new BlowWhistleFlow(badCompany.getInfo().getLegalIdentities().get(0), firstInvestigator.getInfo().getLegalIdentities().get(0));
        Future future = whistleBlower.startFlow(flow);
        network.runNetwork();
        System.out.println("MAde it here");
        return (SignedTransaction) future.get();
    }
    @Test
    public void flowCompleteSuccessfully() throws ExecutionException, InterruptedException {
            blowWhistle();
    }

    @Test
    public void bothPartiesRecordedTransactionAndState() throws ExecutionException, InterruptedException {
        SignedTransaction stx = blowWhistle();
        List<StartedMockNode> nodes = ImmutableList.of(whistleBlower, firstInvestigator);
        for (StartedMockNode node: nodes) {
            SignedTransaction recordedTx  =node.getServices().getValidatedTransactions().getTransaction(stx.getId());
            assertNotNull(recordedTx);

            List<StateAndRef<BlowWhistleState>> recordedStates = node.transaction( () ->
                    node.getServices().getVaultService().queryBy(BlowWhistleState.class).getStates()
            );
            assertEquals(1, recordedStates.size());
            assertEquals(stx.getId(), recordedStates.get(0).getRef().getTxhash());
        }


    }

    @Test
    public void inCreatedStateNeitherPartyIsUsingAWellKnownIdentity() throws ExecutionException, InterruptedException, TransactionResolutionException, SignatureException, AttachmentResolutionException {
        SignedTransaction stx = blowWhistle();
        BlowWhistleState state = stx.toLedgerTransaction(whistleBlower.getServices()).outputsOfType(BlowWhistleState.class).get(0);
        List<PublicKey> whistleBlowerKeys = new ArrayList<>();
        for (Party identity: whistleBlower.getInfo().getLegalIdentities()
             ) {
            whistleBlowerKeys.add(identity.getOwningKey());

        }
        assert(!whistleBlowerKeys.contains(state.getWhistleBlower().getOwningKey()));
        List<PublicKey> investigatorKeys = new ArrayList<>();
        for (Party identity: firstInvestigator.getInfo().getLegalIdentities()
        ) {
            investigatorKeys.add(identity.getOwningKey());

        }
        assert(!investigatorKeys.contains(state.getInvestigator().getOwningKey()));

    }

    @Test
    public void partiesHaveExchangedCertsLinkingConfidentialIDsToWellKnownIDs() throws ExecutionException, InterruptedException, TransactionResolutionException, SignatureException, AttachmentResolutionException {
        SignedTransaction stx = blowWhistle();
        BlowWhistleState state = stx.toLedgerTransaction(whistleBlower.getServices()).outputsOfType(BlowWhistleState.class).get(0);
        assertNotNull(partyFromAnonymous(whistleBlower, state.getInvestigator()));
        assertNotNull(partyFromAnonymous(firstInvestigator, state.getWhistleBlower()));

    }


        @Test
    public void thirdPartyCannotLinkConfIdToWellKnownId() throws ExecutionException, InterruptedException, TransactionResolutionException, SignatureException, AttachmentResolutionException {
        SignedTransaction stx = blowWhistle();
        BlowWhistleState state = stx.toLedgerTransaction(whistleBlower.getServices()).outputsOfType(BlowWhistleState.class).get(0);

        List<AnonymousParty> anonParties = ImmutableList.of(state.getInvestigator(), state.getWhistleBlower());
        for (AnonymousParty anonParty: anonParties) {
                assertNull(partyFromAnonymous(badCompany, anonParty));

        }
        ;
    }
}



