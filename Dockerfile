FROM openjdk:8-alpine

COPY target/uberjar/xero-syncer.jar /xero-syncer/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/xero-syncer/app.jar"]
