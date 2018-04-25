module org.zenika.handson.jigsaw.api.local.impl {
    requires org.zenika.handson.jigsaw.api;
    requires transitive java.logging;
    requires me.xdrop.fuzzywuzzy;
    opens img;

    provides org.zenika.handson.jigsaw.api.CharactersApi
            with org.zenika.handson.jigsaw.api.local.impl.InMemoryCharactersApi;
}