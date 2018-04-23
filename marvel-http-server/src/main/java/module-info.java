module org.zenika.handson.jigsaw.http {
    requires java.logging;
    requires jdk.httpserver;
    requires org.zenika.handson.jigsaw.api;
    requires org.zenika.handson.jigsaw.api.local.impl;
    uses org.zenika.handson.jigsaw.api.CharactersApi;
}