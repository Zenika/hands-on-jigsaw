module org.zenika.handson.jigsaw.http {
    requires org.zenika.handson.jigsaw.api;
    requires org.zenika.handson.jigsaw.api.local.impl;
    uses org.zenika.handson.jigsaw.api.CharactersApi;

    requires vertx.web;
    requires vertx.core;
}