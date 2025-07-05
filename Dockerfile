FROM gradle:8.12-jdk23-alpine AS build

WORKDIR /PayRouteBin
COPY . .
RUN gradle installApp

CMD ["sh", "build/install/PayRouteBin/bin/q2"]
