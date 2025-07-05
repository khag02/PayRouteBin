FROM gradle:8.12-jdk23-alpine AS build

WORKDIR /demo
COPY . .
RUN gradle installApp

CMD ["sh", "build/install/demo/bin/q2"]
