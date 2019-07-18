package com.timesheet.service;

// We sub-class 'SingletonSerializeAsToken' to ensure that instances of this class are never serialised by Kryo.
// When a flow is check-pointed, the annotated @Suspendable methods and any object referenced from within those
// annotated methods are serialised onto the stack. Kryo, the reflection based serialisation framework we use, crawls
// the object graph and serialises anything it encounters, producing a graph of serialised objects.
// This can cause issues. For timesheet, we do not want to serialise large objects on to the stack or objects which may
// reference databases or other external services (which cannot be serialised!). Therefore we mark certain objects with
// tokens. When Kryo encounters one of these tokens, it doesn't serialise the object. Instead, it creates a
// reference to the type of the object. When flows are de-serialised, the token is used to connect up the object
// reference to an instance which should already exist on the stack.

import com.timesheet.contract.InvoiceContract;
import kotlin.Pair;
import liquibase.util.csv.CSVReader;
import net.corda.core.contracts.Command;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.node.AppServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import net.corda.core.transactions.FilteredTransaction;
import net.corda.core.transactions.FilteredTransactionVerificationException;

import java.io.IOException;
import java.io.StringReader;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;

@CordaService
public class SalaryRateOracle extends SingletonSerializeAsToken {
    private AppServiceHub services;
    private HashMap<Pair<String,String>,Double> payRateTable = new HashMap<>();
    private PublicKey myKey;
    SalaryRateOracle(){
        myKey = services.getMyInfo().getLegalIdentities().get(0).getOwningKey();
        CSVReader reader =  new CSVReader(new StringReader(getClass().getResourceAsStream("/payRate.csv").toString()));
        try {
            List<String[]> lines = reader.readAll();
            for (String[] line: lines
                 ) {
                payRateTable.put(new Pair(line[0].trim(), line[1].trim()),Double.parseDouble(line[2].trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns the salary for the given contractor at that company
    // TODO: Figure out what to do about missing pay rates
    public Rate query(RateOf rateOf){
        return new Rate(rateOf, payRateTable.get(new Pair(rateOf.getContractor().getName().getOrganisation(), rateOf.getCompany().getName().getOrganisation())));
    }

    // Signs over a transaction if the specified Nth prime for a particular N is correct.
    // This function takes a filtered transaction which is a partial Merkle tree. Any parts of the transaction which
    // the oracle doesn't need to see in order to verify the correctness of the nth prime have been removed. In this
    // case, all but the [PrimeContract.Create] commands have been removed. If the Nth prime is correct then the oracle
    // signs over the Merkle root (the hash) of the transaction.
    public TransactionSignature sign(FilteredTransaction ftx){
        try {
            ftx.verify();
        } catch (FilteredTransactionVerificationException e) {
            e.printStackTrace();
        }


        Boolean isValidMerkleTree = ftx.checkWithFun((Object elem)->{
            if(elem instanceof Command){
                if(((Command) elem).getValue() instanceof InvoiceContract.Commands.Create){
                    InvoiceContract.Commands.Create cmdData = (InvoiceContract.Commands.Create) ((Command) elem).getValue();
                    return (((Command) elem).getSigners().contains(myKey) && query(new RateOf(cmdData.getContractor(), cmdData.getCompany())).getVal() == cmdData.getRate());
                }
            }
            return false;
        });

        if(isValidMerkleTree){
            return services.createSignature(ftx, myKey);
        }else{
            throw new IllegalArgumentException("SalaryRateOracle signature requested over invalid transaction.");
        }
    }

}