/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.core;

import org.grouplens.grapht.Injector;
import org.grouplens.grapht.graph.Edge;
import org.grouplens.grapht.graph.Graph;
import org.grouplens.grapht.graph.Node;
import org.grouplens.grapht.spi.*;
import org.grouplens.grapht.util.MemoizingProvider;
import org.grouplens.lenskit.symbols.TypedSymbol;

import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.util.*;

/**
 * A Grapht injector that uses a precomputed graph.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class StaticInjector implements Injector {
    private InjectSPI spi;
    private Graph graph;
    private Node root;
    private Map<Node, Provider<?>> providerCache;
    private final SymbolMapping symbolMapping;

    /**
     * Create a new static injector. The node labelled with
     * {@code null} is the root node.
     *
     * @param spi The inject SPI.
     * @param g   The object graph.
     */
    public StaticInjector(InjectSPI spi, Graph g) {
        this(spi, g, g.getNode(null));
    }

    /**
     * Create a new static injector with a specified root node.
     *
     * @param spi The inject SPI.
     * @param g   The object graph.
     * @param rt  The root node.
     */
    public StaticInjector(InjectSPI spi, Graph g, Node rt) {
        this(spi, g, rt, SymbolMapping.empty());
    }

    /**
     * Create a new static injector with a specified root node and symbol mapping.
     *
     * @param spi The inject SPI.
     * @param g   The object graph.
     * @param rt  The root node.
     * @param map The symbol mapping.
     */
    public StaticInjector(InjectSPI spi, Graph g, Node rt, SymbolMapping map) {
        this.spi = spi;
        graph = g;
        root = rt;
        providerCache = new HashMap<Node, Provider<?>>();
        symbolMapping = map;
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        Desire d = spi.desire(null, type, true);
        Edge e = graph.getOutgoingEdge(root, d);

        if (e != null) {
            return type.cast(instantiate(e.getTail()));
        } else {
            Node node = findSatisfyingNode(spi.matchDefault(), type);
            if (node != null) {
                return type.cast(instantiate(node));
            } else {
                return null;
            }
        }
    }

    public <T> T getInstance(Class<? extends Annotation> qual, Class<T> type) {
        Node node = findSatisfyingNode(spi.match(qual), type);
        if (node != null) {
            return type.cast(instantiate(node));
        } else {
            return null;
        }
    }

    /**
     * Find a node with a satisfaction for a specified type. Does a breadth-first
     * search to find the closest matching one.
     *
     * @param type The type to look for.
     * @return A node whose satisfaction is compatible with {@code type}.
     * @review Decide how to handle qualifiers and contexts
     */
    private Node findSatisfyingNode(QualifierMatcher qmatch, Class<?> type) {
        Queue<Node> work = new LinkedList<Node>();
        Set<Node> seen = new HashSet<Node>();
        work.add(root);
        seen.add(root);
        while (!work.isEmpty()) {
            Node node = work.remove();
            for (Edge e : graph.getOutgoingEdges(node)) {
                // is this the node we are looking for?
                Node nbr = e.getTail();
                CachedSatisfaction lbl = nbr.getLabel();
                assert lbl != null;
                Satisfaction sat = lbl.getSatisfaction();
                if (type.isAssignableFrom(sat.getErasedType())) {
                    // right type, check the qualifier
                    Desire d = e.getDesire();
                    assert d != null;
                    if (qmatch.matches(d.getInjectionPoint().getAttributes().getQualifier())) {
                        return nbr;
                    }
                }

                // these are not the nodes we're looking for
                if (!seen.contains(nbr)) {
                    seen.add(nbr);
                    work.add(nbr);
                }
            }
        }
        // got this far, no node
        return null;
    }

    /**
     * Instantiate a particular node in the graph.
     *
     * @param node The node to instantiate.
     * @return The instantiation of the node.
     */
    public Object instantiate(Node node) {
        Provider<?> p = getProvider(node);

        return p.get();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private synchronized Provider<?> getProvider(Node node) {
        Provider<?> provider = providerCache.get(node);
        if (provider == null) {
            CachedSatisfaction lbl = node.getLabel();
            assert lbl != null;
            provider = lbl.getSatisfaction().makeProvider(new DepSrc(node));
            CachePolicy pol = lbl.getCachePolicy();
            if (pol == CachePolicy.NO_PREFERENCE) {
                pol = lbl.getSatisfaction().getDefaultCachePolicy();
            }
            if (provider instanceof SymbolProvider) {
                // symbol providers are placeholders, must look it up.
                TypedSymbol<?> sym = ((SymbolProvider) provider).getSymbol();
                provider = symbolMapping.get(sym);
                if (provider == null) {
                    throw new RuntimeException("no provider found for " + sym);
                }
            }
            switch (pol) {
            case NEW_INSTANCE:
                break;
            default:
                // TODO allow default policy to be specified
                provider = new MemoizingProvider(provider);
                break;
            }
            providerCache.put(node, provider);
        }
        return provider;
    }

    @Override
    public <T> T getInstance(Annotation qualifier, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    private class DepSrc implements ProviderSource {
        private Node node;

        private DepSrc(Node n) {
            this.node = n;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Provider<?> apply(Desire desire) {
            final Node dep = graph.getOutgoingEdge(node, desire).getTail();
            return new Provider() {
                @Override
                public Object get() {
                    return instantiate(dep);
                }
            };
        }
    }
}
