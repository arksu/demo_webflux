# demo_webflux
demo project to test webflux, r2dbc, jooq features

order status:
- new - initial invoice status
- pending - some amount received and waiting for confirmations
- not_enough - the full amount may not have been paid.
- completed - paid in full
- mismatch - overpaid
- error - some error has occurred
- cancelled - no payment received within allotted time

merchant -> invoice -> client redirect to widget 
client (widget) -> select currency (with rates, actual amounts) -> create order
order -> get actual rate -> customer (wallet)
customer -> blockchain -> trx -> wallet
check wallet trx amount -> 
    equal order amount -> complete order
    overpaid -> complete order, alert support
    less than required -> ignore trx, wallet hold as waited

