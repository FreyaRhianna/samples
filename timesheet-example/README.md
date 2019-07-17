<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Example CorDapp

This CordApp demonstrates the use of schemas in a transaction.

In this CorDapp, two nodes can exchange an IOU that encapsulates a QueryableState which returns an instance of a schema object. This schema object is the invoice and holds all of the information related to the IOU.

# To run the Cordapp

To issue an invoice from the contractor to the company, go to the Contractor node and input:
    
    flow start IssueInvoiceFlow hoursWorked: 5, date: 1234 , otherParty: MegaCorp 1
    
In order for the invoice to be paid, you first need to retrieve the UUID of the invoice. From the MegaCorp 1 node run:

    run vaultQuery contractStateType: com.timesheet.state.InvoiceState

Find the linearID of the invoice state (it should be 32 characters). Using that, run from the MegaCorp 1 node:

    flow start PayInvoiceFlow invoiceId: <UUID of invoice state>