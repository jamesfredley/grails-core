# Various properties that control which tests run
* onlyFunctionalTests
* onlyHibernate5Tests
* onlyMongodbTests
* skipNonFunctionalTests
* skipFunctionalTests
* skipHibernate5Tests
* skipMongodbTests

# Start a mongo docker container
docker run -d  --name mongo-on-docker  -p 27017:27017 mongo