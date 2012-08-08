package org.grouplens.lenskit.core;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import org.grouplens.grapht.graph.Edge;
import org.grouplens.grapht.graph.Graph;
import org.grouplens.grapht.graph.Node;
import org.grouplens.grapht.spi.Attributes;
import org.grouplens.grapht.spi.CachedSatisfaction;
import org.grouplens.grapht.spi.Desire;
import org.grouplens.lenskit.data.dao.DataAccessObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;

/**
 * Helper utilities for Grapht integration.
 * @author Michael Ekstrand
 * @since 0.11
 */
class GraphtUtils {
    private GraphtUtils() {}

    public static boolean isDAONode(@Nullable Node n) {
        CachedSatisfaction label = n == null ? null : n.getLabel();
        return label != null &&
                DataAccessObject.class.isAssignableFrom(
                        label.getSatisfaction().getErasedType());
    }

    /**
     * Find the DAO node in a graph.
     * @param graph The graph to search.
     * @return The DAO node, if one exists, or {@code null}.
     * @throws IllegalArgumentException if there is more than one DAO node in the graph.
     */
    public static Node findDAONode(Graph graph) {
        Set<Node> nodes = graph.getNodes();
        Iterator<Node> daoNodes = Iterators.filter(nodes.iterator(), new Predicate<Node>() {
            @Override
            public boolean apply(@Nullable Node input) {
                return isDAONode(input);
            }
        });
        Node node = null;
        if (daoNodes.hasNext()) {
            node = daoNodes.next();
        }
        if (daoNodes.hasNext()) {
            throw new IllegalArgumentException("graph has multiple DAO nodes");
        }
        return node;
    }

    /**
     * Remove transient edges from a set.
     * @param edges The set of edges.
     * @return A new set containing only the non-transient edges.
     */
    public static Set<Edge> removeTransient(Set<Edge> edges) {
        return Sets.filter(edges, new Predicate<Edge>() {
            @Override
            public boolean apply(@Nullable Edge input) {
                Desire desire = input == null ? null : input.getDesire();
                return desire != null && desireIsTransient(desire);
            }
        });
    }

    /**
     * Determine whether a desire is transient.
     * @param d The desire to test.
     * @return {@code true} if the desire is transient.
     */
    public static boolean desireIsTransient(@Nonnull Desire d) {
        Attributes attrs = d.getInjectionPoint().getAttributes();
        return attrs.getAttribute(Transient.class) != null;
    }

    /**
     * Function to extract the tail of a node.
     * @return A function extracting the tail of a node.
     */
    public static Function<Edge,Node> edgeTail() {
        return EdgeTail.instance;
    }

    private static class EdgeTail implements Function<Edge,Node> {
        public static EdgeTail instance = new EdgeTail();

        @Override
        public Node apply(@Nullable Edge input) {
            return input == null ? null : input.getTail();
        }
    }
}
