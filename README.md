## Revolut backend test: money transfer

Implementation a RESTful API (including data model and the backing implementation) for money transfers between accounts.

Project:
- com.google.code.gson:gson
- org.apache.logging.log4j:log4j-api
- org.apache.httpcomponents:httpclient
- JUnit 4

## Build
    mvn clean package

## Run
    java -jar ./target/revolut.task-1.0-SNAPSHOT.jar

## End Points
  POST /api/createAccount
    
    { 
     "body":"{\"name\":\"accountName\", \"sum\":\"100.1\"}"
    }	   
  POST /api/getBalance
    
    { 
     "body":"{\"accountId\":\"3013556246932186279\"}"
    }	
  POST /api/transfer
  
    { 
     "body":"{\"correlationId\":\"corrId\", \"fromId\":\"8144094592418148242\", \"toId\":\"3013556246932186279\", \"sum\":\"50\"}"
    }
   
