package org.grouplens.lenskit.eval.graph;

import org.grouplens.grapht.graph.Edge;
import org.grouplens.grapht.graph.Graph;
import org.grouplens.grapht.graph.Node;
import org.grouplens.grapht.spi.*;
import org.grouplens.lenskit.core.GraphtUtils;
import org.grouplens.lenskit.core.Parameter;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Class to manage traversing nodes. It is not used to handle the root node, but rather handles
 * the rest of them.
*/
class GraphDumper implements SatisfactionVisitor<Void> {
    private static final Logger logger = LoggerFactory.getLogger(GraphDumper.class);
    private static final String ROOT_ID = "root";

    private final GraphWriter writer;
    private final Graph graph;
    private final Map<Node, String> nodeIds;
    private final Queue<Node> queue;

    private Node currentNode;

    public GraphDumper(Graph g, GraphWriter gw) {
        writer = gw;
        graph = g;
        nodeIds = new HashMap<Node, String>();
        queue = new LinkedList<Node>();
    }

    /**
     * Set the root node for this dumper. This must be called before any other methods.
     * @param root The root node.
     * @return The ID of the root node.
     */
    public String setRoot(Node root) {
        if (!nodeIds.isEmpty()) {
            throw new IllegalStateException("root node already specificied");
        }
        nodeIds.put(root, ROOT_ID);
        writer.putNode(new NodeBuilder(ROOT_ID).setLabel("root")
                                               .setShape("diamond")
                                               .build());
        return ROOT_ID;
    }

    /**
     * Add a node to the queue.  If the node is already enqueued, the previous ID number
     * is returned.
     * @param node The node to enqueue
     * @return The node's ID.
     */
    public String enqueue(Node node) {
        if (nodeIds.isEmpty()) {
            throw new IllegalStateException("root node has not been set");
        }
        String id = nodeIds.get(node);
        if (id == null) {
            queue.add(node);
            id = "N" + nodeIds.size();
            nodeIds.put(node, id);
        }
        return id;
    }

    /**
     * Run the queue.
     */
    public void run() {
        while (!queue.isEmpty()) {
            currentNode = queue.remove();
            final CachedSatisfaction csat = currentNode.getLabel();
            assert csat != null;
            final Satisfaction sat = csat.getSatisfaction();
            sat.visit(this);
        }
        currentNode = null;
    }

    private String currentNodeId() {
        if (currentNode == null) {
            throw new IllegalStateException("dumper not running");
        }
        return nodeIds.get(currentNode);
    }

    private Satisfaction currentSatisfaction() {
        if (currentNode == null) {
            throw new IllegalStateException("dumper not running");
        }
        CachedSatisfaction csat = currentNode.getLabel();
        assert csat != null;
        return csat.getSatisfaction();
    }

    @Override
    public Void visitNull() {
        NodeBuilder nb = new NodeBuilder(currentNodeId());
        nb.setShape("ellipse");
        Class<?> type = currentSatisfaction().getErasedType();
        if (DataAccessObject.class.isAssignableFrom(type)) {
            nb.setLabel("DAO");
        } else {
            nb.setLabel("null");
        }
        writer.putNode(nb.build());
        return null;
    }

    @Override
    public Void visitClass(Class<?> clazz) {
        putComponentNode(clazz, null);
        return null;
    }

    @Override
    public Void visitInstance(Object instance) {
        writer.putNode(new NodeBuilder(currentNodeId())
                               .setLabel(instance.toString())
                               .setShape("ellipse")
                               .build());
        return null;
    }

    /**
     * Output the intermediate "provided" node from the current node.
     * @return The ID of the provider node.
     */
    private String putProvidedNode() {
        ComponentNodeBuilder cnb;
        cnb = new ComponentNodeBuilder(currentNodeId(), currentSatisfaction().getErasedType());
        cnb.setShareable(GraphtUtils.isShareable(currentNode))
           .setIsProvided(true);
        writer.putNode(cnb.build());
        String pid = currentNodeId() + "P";
        writer.putEdge(new EdgeBuilder(currentNodeId(), pid)
                               .set("style", "dashed")
                               .set("dir", "back")
                               .set("arrowhead", "empty")
                               .build());
        return pid;
    }

    @Override
    public Void visitProviderClass(Class<? extends Provider<?>> pclass) {
        String pid = putProvidedNode();
        putComponentNode(pclass, pid);
        return null;
    }

    @Override
    public Void visitProviderInstance(Provider<?> provider) {
        String pid = putProvidedNode();
        writer.putNode(new NodeBuilder(pid)
                               .setLabel(provider.toString())
                               .setShape("ellipse")
                               .set("style", "dashed")
                               .build());
        return null;
    }

    private void putComponentNode(Class<?> type, String pid) {
        String id = pid == null ? currentNodeId() : pid;
        ComponentNodeBuilder lbl = new ComponentNodeBuilder(id, type);
        lbl.setShareable(pid == null && GraphtUtils.isShareable(currentNode));
        lbl.setIsProvider(pid != null);
        for (Edge e: graph.getOutgoingEdges(currentNode)) {
            Desire dep = e.getDesire();
            assert dep != null;
            Annotation q = dep.getInjectionPoint().getAttributes().getQualifier();
            Node t = e.getTail();
            if (q != null && q.annotationType().getAnnotation(Parameter.class) != null) {
                logger.debug("dumping parameter {}", q);
                CachedSatisfaction tcsat = t.getLabel();
                assert tcsat != null;
                Satisfaction tsat = tcsat.getSatisfaction();
                Object val = tsat.visit(new AbstractSatisfactionVisitor<Object>(null) {
                    @Override
                    public Object visitInstance(Object instance) {
                        return instance;
                    }
                });
                if (val == null) {
                    logger.warn("parameter {} not bound", q);
                }
                lbl.addParameter(q, val);
            } else {
                logger.debug("dumping dependency {}", dep);
                lbl.addDependency(dep);
                String tid = enqueue(t);
                String port = String.format("%s:%d", id, lbl.getLastDependencyPort());
                writer.putEdge(new EdgeBuilder(port, tid)
                                       .set("arrowhead", "vee")
                                       .build());
            }
        }
        writer.putNode(lbl.build());
    }
}
