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
    private final String username;

    public static RuleContext fromDisk(Path catalinaBase) {
        Document serverXml = null;
        Document tomcatUsersXml = null;

        try {
            serverXml = parseXml(catalinaBase.resolve("conf/server.xml"));
        } catch (SAXException e) {
            System.err.println("[ERROR] server.xml is malformed and could not be parsed: " + e.getMessage());
            System.err.println("Please check the file for XML syntax errors.");
        } catch (ParserConfigurationException | IOException e) {
            System.err.println("[ERROR] Could not read server.xml: " + e.getMessage());
        }

        try {
            tomcatUsersXml = parseXml(catalinaBase.resolve("conf/tomcat-users.xml"));
        } catch (SAXException e) {
            System.err.println("[ERROR] tomcat-users.xml is malformed and could not be parsed: " + e.getMessage());
            System.err.println("Please check the file for XML syntax errors.");
        } catch (ParserConfigurationException | IOException e) {
            System.err.println("[ERROR] Could not read tomcat-users.xml: " + e.getMessage());
        }

        return new RuleContext(catalinaBase, serverXml, tomcatUsersXml, System.getProperty("user.name"));
    }

    public RuleContext(Path catalinaBase, Document serverXml, Document tomcatUsersXml, String username) {
        this.catalinaBase = catalinaBase;
        this.serverXml = serverXml;
        this.tomcatUsersXml = tomcatUsersXml;
        this.username = username;
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

    public String getUsername() {
        return username;
    }

    private static Document parseXml(Path path) throws SAXException, IOException, ParserConfigurationException {
        if (!Files.exists(path)) {
            System.err.println("[WARN] File not found: " + path);
            return null;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(path.toFile());
    }
}
