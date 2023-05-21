# demo_webflux
demo project to test webflux, r2dbc, jooq features

order status:
- new - initial invoice status
- pending - some amount received and waiting for confirmations
- expired - look for the “amount” field to verify payment. The full amount may not have been paid.
- completed - paid in full
- mismatch - overpaid
- error - some error has occurred
- cancelled - no payment received within allotted time