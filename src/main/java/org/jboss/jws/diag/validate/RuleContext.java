package org.jboss.jws.diag.validate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RuleContext {
    private final Path catalinaBase;
    private final Document serverXml;
    private final Document tomcatUsersXml;

    public static RuleContext from(Path catalinaBase) {
        return new RuleContext(
                catalinaBase,
                parseXml(catalinaBase.resolve("conf/server.xml")),
                parseXml(catalinaBase.resolve("conf/tomcat-users.xml"))
        );
    }

    public RuleContext(Path catalinaBase, Document serverXml, Document tomcatUsersXml) {
        this.catalinaBase = catalinaBase;
        this.serverXml = serverXml;
        this.tomcatUsersXml = tomcatUsersXml;
    }

    public Path getCatalinaBase() {
        return catalinaBase;
    }

    public Document getServerXml() {
        return serverXml;
    }

    public Document getTomcatUsersXml() {
        return tomcatUsersXml;
    }

    private static Document parseXml(Path path) {
        if (!Files.exists(path)) {
            return null;
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(path.toFile());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return null;
        }

    }
}
