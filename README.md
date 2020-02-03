# sampleproject
To start the application

mvn clean install
java -jar target/sampleproject-1.0-SNAPSHOT.jar db migrate sample.yml
java -jar target/sampleproject-1.0-SNAPSHOT.jar server sample.yml
or in IDE: com.maxk.sampleproject.SampleApplication server sample.yml
