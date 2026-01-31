cd posb-service
mvn clean install -Dpackaging=docker && ^
docker build -f ./target/Dockerfile -t payments-posb-service:%1 ./target && ^
docker tag payments-posb-service:%1 dockerrepo.icecash.mobi:5000/payments/posb-service:%1 && ^
docker login dockerrepo.icecash.mobi:5000 && ^
docker push dockerrepo.icecash.mobi:5000/payments/posb-service:%1 && ^
cd ..
