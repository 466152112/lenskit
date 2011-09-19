package org.grouplens.lenskit.dtree.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.grouplens.lenskit.dtree.DataNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * XML implementation of {@link DataNode}.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
@Immutable
public class XMLDataNode implements DataNode {
    
    private static String nodeTypeString(short type) {
        switch (type) {
        case Node.ATTRIBUTE_NODE:
            return "Attr";
        case Node.CDATA_SECTION_NODE:
            return "CDATASection";
        case Node.COMMENT_NODE:
            return "Comment";
        case Node.DOCUMENT_FRAGMENT_NODE:
            return "DocumentFragment";
        case Node.DOCUMENT_NODE:
            return "Document";
        case Node.DOCUMENT_TYPE_NODE:
            return "DocumentType";
        case Node.ELEMENT_NODE:
            return "Element";
        case Node.ENTITY_NODE:
            return "Entity";
        case Node.ENTITY_REFERENCE_NODE:
            return "EntityReference";
        case Node.NOTATION_NODE:
            return "Notation";
        case Node.PROCESSING_INSTRUCTION_NODE:
            return "ProcessingInstruction";
        case Node.TEXT_NODE:
            return "Text";
        default:
            return String.format("<unknown:%d>", type);
        }
    }
    
    /**
     * Wrap an XML node in a data tree.
     * @see #wrap(Properties, Node)
     */
    public static XMLDataNode wrap(Node node) {
        return wrap(null, node);
    }
    
    /**
     * Wrap an XML node in a data tree with property interpolation.
     * @param props A set of properties to interpolate.
     * @param node An XML document or element node to be wrapped.
     * @return A data tree wrapping this node.
     * @throws IllegalArgumentException if an unwrappable node is passed.
     */
    public static XMLDataNode wrap(@Nullable Properties props, Node node) {
        if (node == null)
            return null;
        
        switch (node.getNodeType()) {
        case Node.ELEMENT_NODE:
            return new XMLDataNode(props, node);
        case Node.DOCUMENT_NODE:
            return new XMLDataNode(props, ((Document) node).getDocumentElement());
        default:
            throw new IllegalArgumentException("Unwrappable node type "
                    + nodeTypeString(node.getNodeType()));    
        }
    }
    
    private final Properties properties;
    private final Node xml;
    private List<DataNode> kids;
    
    public XMLDataNode(Properties props, Node node) {
        properties = new Properties(props);
        xml = node;
    }

    @Override
    public String getName() {
        return xml.getNodeName();
    }

    @Override
    public String getValue() {
        if (getChildren().isEmpty()) {
            String val = xml.getTextContent();
            if (properties != null)
                val = interpolate(val, properties);
            return val.trim();
        } else {
            return "";
        }
    }

    @Override
    public synchronized List<DataNode> getChildren() {
        if (kids == null) {
            NodeList xmlkids = xml.getChildNodes();
            final int nk = xmlkids.getLength();
            kids = new ArrayList<DataNode>(nk);
            for (int i = 0; i < nk; i++) {
                Node n = xmlkids.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    kids.add(wrap(properties, n));
                }
            }
        }
        return kids;
    }
    
    private static class NameFilter implements Predicate<DataNode> {
        private final String name;
        public NameFilter(String n) {
            name = n;
        }
        
        @Override
        public boolean apply(DataNode node) {
            return node.getName().equals(name);
        }
    }

    @Override
    public List<DataNode> getChildren(String name) {
        Predicate<DataNode> filter = new NameFilter(name);
        return new ArrayList<DataNode>(Collections2.filter(getChildren(), filter));
    }

    /**
     * Interpolate properties into a string.
     * @param text
     * @param props
     * @return
     */
    static String interpolate(String text, Properties props) {
        Pattern ppat = Pattern.compile("\\$\\{([\\p{L}\\p{Nd}]+(?:\\.[\\p{L}\\p{Nd}]+)*)\\}");
        Matcher m = ppat.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String prop = m.group(1);
            String val = props.getProperty(prop);
            if (val == null) {
                m.appendReplacement(sb, "\\${$1}");
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(val));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
