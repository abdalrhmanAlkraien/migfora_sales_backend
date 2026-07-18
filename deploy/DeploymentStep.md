- aws sso login --profile migfora

- aws ecr get-login-password --region eu-central-1 --profile migfora | \
docker login --username AWS --password-stdin \
984456404229.dkr.ecr.eu-central-1.amazonaws.com



- docker buildx build --platform linux/amd64 -t migfora-sales-api:1.0.12 --load .

docker tag migfora-sales-api:1.0.12 984456404229.dkr.ecr.eu-central-1.amazonaws.com/migfora-sales-api:1.0.12

- docker push 984456404229.dkr.ecr.eu-central-1.amazonaws.com/migfora-sales-api:1.0.12

curl https://checkip.amazonaws.com

