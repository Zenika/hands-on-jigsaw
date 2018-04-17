#!/usr/bin/env bash
./gradlew clean jar
[ -d `pwd`/marveljre ] && rm -Rf `pwd`/marveljre
jlink --module-path $JAVA_HOME/jmods:`pwd`/api-marvel/build/libs/api-marvel.jar:`pwd`/marvel-local-impl/build/libs/marvel-local-impl.jar:`pwd`/marvel-http-server/build/libs/marvel-http-server.jar:`pwd`/diffutils/build/libs/diffutils.jar:`pwd`/fuzzywuzzy/build/libs/fuzzywuzzy.jar   \
--add-modules org.zenika.handson.jigsaw.api.local.impl,org.zenika.handson.jigsaw.http,me.xdrop.fuzzywuzzy,me.xdrop.diffutils \
--output `pwd`/marveljre --launcher marvelous=org.zenika.handson.jigsaw.http/org.zenika.handson.jigsaw.http.Application \
--compress=2 --vm=server --class-for-name --strip-debug --no-header-files --no-man-pages
# add VM options
#sed -i s/JLINK_VM_OPTIONS=/"JLINK_VM_OPTIONS=\"-Xms128m -Xmx128m -Ddev=false\""/g ./marveljre/bin/marvelous
sed -i s/JLINK_VM_OPTIONS=/"JLINK_VM_OPTIONS=\"-Dport=8080 -Ddev=false\""/g ./marveljre/bin/marvelous
