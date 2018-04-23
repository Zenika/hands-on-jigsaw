open module org.zenika.handson.jigsaw.http {
    requires java.logging;
//    requires org.zenika.handson.jigsaw.api;
    requires org.zenika.handson.jigsaw.api.local.impl;
    requires jdk.httpserver;
    uses org.zenika.handson.jigsaw.api.CharactersApi;

}