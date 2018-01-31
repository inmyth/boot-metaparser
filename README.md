### Ripple Metaparser, Spring Boot


A rest server that sends a request to Ripple Data Api and parses the result into high level data.


#### Get Account Transaction History 

https://ripple.com/build/data-api-v2/#get-account-transaction-history

`/accounts/{account}/transactions`
 
Example: 
`/accounts/rGMNHZyj7NizdpDYW4mLZeeWEXeMm6Vv1y/transactions?result=tesSUCCESS&limit=10&start=2018-01-26&end=2018-01-29&descending=true&host=data.ripple.com`

*host* 

data.ripple.com

or any other Ripple Data Api url



