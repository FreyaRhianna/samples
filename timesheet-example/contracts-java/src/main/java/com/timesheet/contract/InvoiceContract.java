package com.timesheet.contract;

import com.google.common.collect.ImmutableList;
import com.timesheet.schema.InvoiceSchema;
import com.timesheet.state.InvoiceState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class InvoiceContract implements Contract {
    public static String ID = "com.timesheet.contract.InvoiceContract";
    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties command = tx.getCommands().get(0);
        if(command.getValue() instanceof  Commands.Create){
            requireThat( require -> {
               require.using("No inputs should be consumed when issuing an Invoice.", tx.getInputs().isEmpty());
               require.using("Only one output state should be created.", tx.getOutputs().size() == 1);
               InvoiceState out = tx.outputsOfType(InvoiceState.class).get(0);
               require.using("The lender and the borrower cannot be the same entity.", out.getContractor() != out.getCompany() );
               List<PublicKey> publicKeys = new ArrayList<>();
               for (AbstractParty participant: out.getParticipants()
               ) {
                   publicKeys.add(participant.getOwningKey());
               };
               require.using("All of the participants must be signers.", command.getSigners().containsAll(publicKeys));

               //Invoice specific constraints
                require.using("The Invoice's value must be non-negative.", out.getHoursWorked() > 0);
                require.using("The invoice should not yet be paid", !out.getPaid());
                return null;
            });
        }else if(command.getValue() instanceof Commands.Pay){
            requireThat(require -> {
               require.using("Exactly one invoice state should be consumed when paying an Invoice.", tx.inputsOfType(InvoiceState.class).size() == 1);
               require.using("Expect only one invoice state output from paying an invoice.", tx.outputsOfType(InvoiceState.class).size() == 1);
               require.using("Expect at least one output in addition to invoice.", tx.getOutputs().size() >= 2);

                //Invoice-specific requirements
                // TODO: Support partial payments
               InvoiceState out =  tx.outputsOfType(InvoiceState.class).get(0);
               require.using("The invoice should now be paid.", out.getPaid());
               return null;
            });
        }
    }
    interface Commands extends CommandData {
        class Create implements Commands{
            private Party contractor;
            private Party company;
            private Double rate;

            public Create(Party contractor, Party company, Double rate){
                this.contractor = contractor;
                this.company = company;
                this.rate = rate;
            }
        }
        class Pay implements Commands{

        }

    }
}
