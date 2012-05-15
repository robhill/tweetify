package tweetify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class XmlUtil {

    public static String getElementContents(Element element, String elementName) throws IOException {
        Element first = getFirstElement(element, elementName);
        if (first != null) {
            return first.getTextContent().trim();
        } else {
            return null;
        }
    }

    public static int getElementContentsAsInteger(Element element, String elementName) throws IOException {
        int results = 0;
        Element first = getFirstElement(element, elementName);
        if (first != null) {
            try {
                results = Integer.parseInt(first.getTextContent());
            } catch (NumberFormatException ex) {
            }
        }
        return results;
    }

    public static Element getFirstElement(Element element, String elementName) throws IOException {
        NodeList list = element.getElementsByTagName(elementName);
        if (list.getLength() >= 1) {
            Element subElement = (Element) list.item(0);
            return subElement;
        } else {
            return null;
        }
    }

    public static Node getDescendent(Node node, String nodeName) throws IOException {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(nodeName)) {
                return child;
            }
        }
        return null;
    }

    public static List<Node> getDescendents(Node node, String nodeName) throws IOException {
        List<Node> childList = new ArrayList<Node>();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(nodeName)) {
                childList.add(child);
            }
        }
        return childList;
    }

    public static void dump(Node node) {
        System.out.println("Node: " + node.getNodeName());
        NamedNodeMap nnm = node.getAttributes();
        if (nnm != null) {
            for (int i = 0; i < nnm.getLength(); i++) {
                Node n = nnm.item(i);
                System.out.println("   " + n.getNodeName() + ":" + n.getNodeValue());
            }
        }
    }

    public static String getDescendentText(Node node, String name) throws IOException {
        Node d = getDescendent(node, name);
        if (d != null) {
            return d.getTextContent().trim();
        }
        return null;
    }

    public static float getDescendentTextAsFloat(Node node, String name, float defaultValue) throws IOException {
        Node d = getDescendent(node, name);
        if (d != null) {
            String sval  = d.getTextContent().trim();
            return Float.parseFloat(sval);
        }
        return defaultValue;
    }

    public static int getDescendentTextAsInt(Node node, String name, int defaultValue) throws IOException {
        Node d = getDescendent(node, name);
        if (d != null) {
            String sval  = d.getTextContent().trim();
            return Integer.parseInt(sval);
        }
        return defaultValue;
    }


    public static String entify(String s) {
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        s = s.replaceAll("&", "&amp;");
        return s;
    }
}

