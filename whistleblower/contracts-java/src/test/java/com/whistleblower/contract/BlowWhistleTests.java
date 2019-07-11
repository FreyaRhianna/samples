package com.whistleblower.contract;

import com.google.common.collect.ImmutableList;
import com.whistleblower.state.BlowWhistleState;
import net.corda.core.contracts.CommandData;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class BlowWhistleTests {
    private MockServices ledgerServices = new MockServices(ImmutableList.of("com.whistleblower"));
    private Party badCompany = new TestIdentity(new CordaX500Name("Bad Company", "Eldoret", "KE")).getParty();
    private AnonymousParty whistleBlower = new TestIdentity(new CordaX500Name("Whistle Blower", "Nairobi", "KE")).getParty().anonymise();
    private AnonymousParty investigator = new TestIdentity(new CordaX500Name("Investigator", "Kisumu", "KE")).getParty().anonymise();

    @Test
    public void ABlowWhistleStateTransactionMustHaveABlowWhistleContractCommand(){
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.output(BlowWhistleContract.ID, new BlowWhistleState(badCompany,whistleBlower,investigator));
                tx.command(ImmutableList.of(whistleBlower.getOwningKey(), investigator.getOwningKey()), new DummyCommandData());
                tx.fails();
                return null;
            });

            ledger.transaction(tx -> {
                tx.output(BlowWhistleContract.ID, new BlowWhistleState(badCompany,whistleBlower,investigator));
                tx.command(ImmutableList.of(whistleBlower.getOwningKey(), investigator.getOwningKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

    @Test
    public void ABlowWhistleTransactionShouldHaveZeroInputsAndASingleBlowWhistleStateOutput(){
        ledger(ledgerServices, ledger -> {
            ledger.transaction(tx -> {
                tx.input(BlowWhistleContract.ID, new BlowWhistleState(badCompany, whistleBlower, investigator));
                tx.output(BlowWhistleContract.ID, new BlowWhistleState(badCompany, whistleBlower, investigator));
                tx.command(ImmutableList.of(whistleBlower.getOwningKey(), investigator.getOwningKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
                tx.failsWith("A BlowWhistle transaction should have zero inputs.");
                return null;
            });
            ledger.transaction(tx ->{
                tx.output(BlowWhistleContract.ID, new DummyState(0));
                tx.command(ImmutableList.of(whistleBlower.getOwningKey(), investigator.getOwningKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
                tx.failsWith("A BlowWhistle transaction should have a BlowWhistleState output.");
                return null;
            });
            ledger.transaction(tx ->{
                tx.output(BlowWhistleContract.ID, new BlowWhistleState(badCompany, whistleBlower, investigator));
                tx.output(BlowWhistleContract.ID, new DummyState(0));
                tx.command(ImmutableList.of(whistleBlower.getOwningKey(), investigator.getOwningKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
                tx.failsWith("A BlowWhistle transaction should have no other outputs.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.output(BlowWhistleContract.ID, new BlowWhistleState(badCompany, whistleBlower, investigator));
                tx.command(ImmutableList.of(whistleBlower.getOwningKey(), investigator.getOwningKey()), new BlowWhistleContract.Commands.BlowWhistleCmd());
                tx.verifies();
                return null;
            });
            return null;
        });
    }

}

class DummyCommandData implements CommandData {}
/*


    @Test
    fun `A BlowWhistle transaction should be signed by the whistle-blower and the investigator`() {
        // No whistle-blower signature.
        ledgerServices.ledger {
            transaction {
                output(BlowWhistleContract.ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(investigator.owningKey, BlowWhistleCmd())
                fails()
            }
        }
        // No investigator signature.
        ledgerServices.ledger {
            transaction {
                output(BlowWhistleContract.ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(whistleBlower.owningKey, BlowWhistleCmd())
                fails()
            }
        }

        ledgerServices.ledger {
            transaction {
                output(BlowWhistleContract.ID, BlowWhistleState(badCompany, whistleBlower, investigator))
                command(listOf(whistleBlower.owningKey, investigator.owningKey), BlowWhistleCmd())
                verifies()
            }
        }
    }
}


 */