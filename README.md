### Ripple Metaparser, Spring Boot


A rest server that sends a request to Ripple Data Api and parses the result into high level data.

Executable jar should be available in /target. See Build for running. 

[Version log](./VERSIONS)

#### Get Account Transaction History 

https://ripple.com/build/data-api-v2/#get-account-transaction-history

`/accounts/{account}/transactions`
 
Example: 
`/accounts/rGMNHZyj7NizdpDYW4mLZeeWEXeMm6Vv1y/transactions?result=tesSUCCESS&limit=10&start=2018-01-26&end=2018-01-29&descending=true&host=data.ripple.com`

*host* 

data.ripple.com

or any other Ripple Data Api url


### Build
Both ripple-core and ripple-bouncycastle have to be added as maven dependency

- Build them with `mvn clean package` in each root 

- Install them to the project. Run install pointing to their resulting jar files

```
../apache-maven-3.5.2/bin/mvn install:install-file -Dfile=../ripple-lib-java/ripple-bouncycastle/target/ripple-bouncycastle-0.0.1-SNAPSHOT.jar -DgroupId=com.ripple -DartifactId=ripple-bouncycastle -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar


 ../../apache-maven-3.5.2/bin/mvn install:install-file -Dfile=../../ripple-lib-java/ripple-core/target/ripple-core-0.0.1-SNAPSHOT.jar -DgroupId=com.ripple -DartifactId=ripple-core -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar
```

- Add them to pom.xml


```
<dependency>
	<groupId>com.ripple</groupId>
	<artifactId>ripple-bouncycastle</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<scope>compile</scope>
</dependency>
		
<dependency>
	<groupId>com.ripple</groupId>
	<artifactId>ripple-core</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<scope>compile</scope>
</dependency>

```

- Finally on root folder run

`mvn clean package` 

the jar file is in `/target`

- Run 

`java -jar jarfile.jar`




