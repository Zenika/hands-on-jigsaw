FROM azul/zulu-openjdk-debian:10 AS builder
ENV JAVA_HOME=/usr/lib/jvm/zulu-10-amd64 \
    LANG=C.UTF-8

WORKDIR /build

COPY gradle /build/gradle
COPY gradlew /build/
COPY settings.gradle /build/
COPY build.gradle /build/

COPY api-marvel /build/api-marvel
COPY marvel-http-server /build/marvel-http-server
COPY marvel-local-impl /build/marvel-local-impl
COPY diffutils /build/diffutils
COPY fuzzywuzzy /build/fuzzywuzzy

RUN ./gradlew --no-daemon clean jar

RUN jlink --module-path $JAVA_HOME/jmods:/build/api-marvel/build/libs/api-marvel.jar:/build/marvel-local-impl/build/libs/marvel-local-impl.jar:/build/marvel-http-server/build/libs/marvel-http-server.jar:/build/diffutils/build/libs/diffutils.jar:/build/fuzzywuzzy/build/libs/fuzzywuzzy.jar   \
--add-modules org.zenika.handson.jigsaw.api.local.impl,org.zenika.handson.jigsaw.http,me.xdrop.fuzzywuzzy,me.xdrop.diffutils \
--output /build/marveljre --launcher marvelous=org.zenika.handson.jigsaw.http/org.zenika.handson.jigsaw.http.Application \
--compress=2 --vm=server --class-for-name --strip-debug --no-header-files --no-man-pages
# add VM options
#RUN sed -i s/JLINK_VM_OPTIONS=/"JLINK_VM_OPTIONS=\"-Dport=8080 -Ddev=false\""/g ./marveljre/bin/marvelous
RUN sed -i s/JLINK_VM_OPTIONS=/"JLINK_VM_OPTIONS=\"-Ddev=false\""/g ./marveljre/bin/marvelous

FROM debian:9-slim
LABEL maintainer="louis.tournayre@zenika.com, mathieu.mure@zenika.com"
LABEL authors="louis.tournayre@zenika.com, mathieu.mure@zenika.com"
LABEL version="1.0"
WORKDIR /marveljre
COPY --from=0 /build/marveljre /marveljre
EXPOSE 8080
ENTRYPOINT ["/marveljre/bin/marvelous"]
