FROM azul/zulu-openjdk-debian:10 AS builder
ENV JAVA_HOME=/usr/lib/jvm/zulu-10-amd64 \
    LANG=C.UTF-8

WORKDIR /build

RUN mkdir jars

COPY api-marvel/target/api-marvel-*.jar /build/jars/
COPY marvel-http-server/target/marvel-http-server-*.jar /build/jars/
COPY marvel-local-impl/target/marvel-local-impl-*.jar /build/jars/
COPY diffutils/target/diffutils-*.jar /build/jars/
COPY fuzzywuzzy/target/fuzzywuzzy-*.jar /build/jars/

RUN jlink -p $JAVA_HOME/jmods:/build/jars \
        --add-modules org.zenika.handson.jigsaw.api.local.impl,org.zenika.handson.jigsaw.http,me.xdrop.fuzzywuzzy,me.xdrop.diffutils \
        --output /build/marveljre --launcher marvelous=org.zenika.handson.jigsaw.http/org.zenika.handson.jigsaw.http.Application \
        --compress=2 --vm=server --class-for-name --strip-debug --no-header-files --no-man-pages

RUN sed -i s/JLINK_VM_OPTIONS=/"JLINK_VM_OPTIONS=\"-Ddev=false\""/g ./marveljre/bin/marvelous

FROM debian:9-slim
LABEL maintainer="louis.tournayre@zenika.com, mathieu.mure@zenika.com"
LABEL authors="louis.tournayre@zenika.com, mathieu.mure@zenika.com, said.bouras@zenika.com"
LABEL version="1.0"

WORKDIR /marveljre

COPY --from=builder /build/marveljre /marveljre

EXPOSE 8080

ENTRYPOINT ["/marveljre/bin/marvelous"]