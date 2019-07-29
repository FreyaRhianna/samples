package com.whistleblower.contract;

import com.whistleblower.state.BlowWhistleState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BlowWhistleContract implements Contract {
    public static final String ID = "com.whistleblower.contract.BlowWhistleContract";


    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        final CommandWithParties<Commands.BlowWhistleCmd> command = requireSingleCommand(tx.getCommands(), Commands.BlowWhistleCmd.class);
        requireThat(require ->{
            require.using("A BlowWhistle transaction should have zero inputs.", tx.getInputs().isEmpty());
            require.using("A BlowWhistle transaction should have a BlowWhistleState output.",tx.outputsOfType(BlowWhistleState.class).size()== 1);
            require.using("A BlowWhistle transaction should have no other outputs.", tx.getOutputs().size() == 1);
            final BlowWhistleState state = tx.outputsOfType(BlowWhistleState.class).get(0);
            require.using("A BlowWhistle transaction should be signed by the whistle-blower and the investigator.", command.getSigners().containsAll(state.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
            return null;
        });
    }

    public interface Commands extends CommandData {
        class BlowWhistleCmd implements Commands {}
    }
}

/*

 */
