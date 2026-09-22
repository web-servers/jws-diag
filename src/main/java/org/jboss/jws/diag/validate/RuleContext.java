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
    private final String serverXmlProblem;

    public static RuleContext fromDisk(Path catalinaBase) {
        Document serverXml = null;
        Document tomcatUsersXml = null;
        String serverXmlProblem = null;

        Path serverXmlPath = catalinaBase.resolve("conf/server.xml");
        try {
            serverXml = parseXml(serverXmlPath);
            if (serverXml == null) {
                serverXmlProblem = "server.xml not found at " + serverXmlPath;
            }
        } catch (SAXException e) {
            System.err.println("[ERROR] server.xml is malformed and could not be parsed: " + e.getMessage());
            System.err.println("Please check the file for XML syntax errors.");
            serverXmlProblem = "server.xml is malformed and could not be parsed: " + e.getMessage();
        } catch (ParserConfigurationException | IOException e) {
            System.err.println("[ERROR] Could not read server.xml: " + e.getMessage());
            serverXmlProblem = "could not read server.xml: " + e.getMessage();
        }

        try {
            tomcatUsersXml = parseXml(catalinaBase.resolve("conf/tomcat-users.xml"));
        } catch (SAXException e) {
            System.err.println("[ERROR] tomcat-users.xml is malformed and could not be parsed: " + e.getMessage());
            System.err.println("Please check the file for XML syntax errors.");
        } catch (ParserConfigurationException | IOException e) {
            System.err.println("[ERROR] Could not read tomcat-users.xml: " + e.getMessage());
        }

        return new RuleContext(catalinaBase, serverXml, tomcatUsersXml, System.getProperty("user.name"),
                serverXmlProblem);
    }

    public RuleContext(Path catalinaBase, Document serverXml, Document tomcatUsersXml, String username) {
        this(catalinaBase, serverXml, tomcatUsersXml, username, null);
    }

    private RuleContext(Path catalinaBase, Document serverXml, Document tomcatUsersXml, String username,
                        String serverXmlProblem) {
        this.catalinaBase = catalinaBase;
        this.serverXml = serverXml;
        this.tomcatUsersXml = tomcatUsersXml;
        this.username = username;
        this.serverXmlProblem = serverXmlProblem;
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

    /**
     * Why server.xml could not be used, or null when it was read. Only set by
     * {@link #fromDisk(Path)}; a context built directly from a parsed document has none.
     */
    String getServerXmlProblem() {
        return serverXmlProblem;
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
