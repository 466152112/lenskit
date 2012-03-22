/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2012 Regents of the University of Minnesota and contributors
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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Provider;

import org.grouplens.inject.Binding;
import org.grouplens.inject.Context;
import org.grouplens.inject.InjectorConfigurationBuilder;
import org.grouplens.inject.graph.Edge;
import org.grouplens.inject.graph.Graph;
import org.grouplens.inject.graph.Node;
import org.grouplens.inject.solver.DependencySolver;
import org.grouplens.inject.spi.Desire;
import org.grouplens.inject.spi.Satisfaction;
import org.grouplens.inject.util.InstanceProvider;
import org.grouplens.lenskit.GlobalItemRecommender;
import org.grouplens.lenskit.GlobalItemScorer;
import org.grouplens.lenskit.ItemRecommender;
import org.grouplens.lenskit.ItemScorer;
import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.RecommenderEngineFactory;
import org.grouplens.lenskit.data.dao.DAOFactory;
import org.grouplens.lenskit.data.dao.DataAccessObject;

import com.google.common.base.Function;

/**
 * {@link RecommenderEngineFactory} that builds a LenskitRecommenderEngine.
 * 
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 */
public class LenskitRecommenderEngineFactory implements RecommenderEngineFactory, Cloneable, Context {
    private final InjectorConfigurationBuilder config;
    private final DAOFactory factory;
    
    public LenskitRecommenderEngineFactory() {
        this(null);
    }
    
    public LenskitRecommenderEngineFactory(@Nullable DAOFactory factory) {
        this.factory = factory;
        config = new InjectorConfigurationBuilder();
    }
    
    @Override
    public <T> Binding<T> bind(Class<T> type) {
        return config.getRootContext().bind(type);
    }

    @Override
    public void bind(Class<? extends Annotation> param, Object value) {
        config.getRootContext().bind(param, value);
    }

    @Override
    public Context in(Class<?> type) {
        return config.getRootContext().in(type);
    }

    @Override
    public Context in(Class<? extends Annotation> qualifier, Class<?> type) {
        return config.getRootContext().in(qualifier, type);
    }

    @Override
    public Context in(String name, Class<?> type) {
        return config.getRootContext().in(name, type);
    }
    
    @Override
    public LenskitRecommenderEngineFactory clone() {
        try {
            return (LenskitRecommenderEngineFactory) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public LenskitRecommenderEngine create() {
        if (factory == null)
            throw new IllegalStateException("create() called with no DAOFactory");
        DataAccessObject dao = factory.snapshot();
        try {
            return create(dao);
        } finally {
            dao.close();
        }
    }
    
    public LenskitRecommenderEngine create(DataAccessObject dao) {
        // FIXME: we really ought to clone the config before we modify it here
        config.getRootContext().bind(DataAccessObject.class).to(dao);
        
        DependencySolver solver = new DependencySolver(config.build(), 100);
        
        // Resolve all required types to complete a Recommender
        solver.resolve(config.getSPI().desire(null, RatingPredictor.class));
        solver.resolve(config.getSPI().desire(null, ItemScorer.class));
        solver.resolve(config.getSPI().desire(null, GlobalItemScorer.class));
        solver.resolve(config.getSPI().desire(null, ItemRecommender.class));
        solver.resolve(config.getSPI().desire(null, GlobalItemRecommender.class));

        // At this point the graph contains the dependency state to build a
        // recommender with the current DAO. Any extra bind rules don't matter
        // because they could not have created any Nodes.
        Graph<Satisfaction, Desire> buildGraph = solver.getGraph();
        
        // Instantiate all nodes, and remove transient edges
        Queue<Node<Satisfaction>> removeQueue = new LinkedList<Node<Satisfaction>>();
        Map<Node<Satisfaction>, Object> instances = instantiate(buildGraph, removeQueue);
        
        // Remove all subgraphs that have been detached by the transient edge removal
        pruneGraph(buildGraph, removeQueue);
        
        Iterator<Entry<Node<Satisfaction>, Object>> i = instances.entrySet().iterator();
        while(i.hasNext()) {
            Node<Satisfaction> n = i.next().getKey();
            if (n.getLabel() != null) {
                // Remove this instance if it is a DAO, or depends on a DAO,
                // or if no other node depends on it
                if (DataAccessObject.class.isAssignableFrom(n.getLabel().getErasedType())) {
                    // This is the DAO instance node specific to the build phase,
                    // we replace it with a special satisfaction so it can be replaced
                    // per-session by the LenskitRecommenderEngine
                    Node<Satisfaction> newDAONode = new Node<Satisfaction>(new DAOSatisfaction());
                    buildGraph.replaceNode(n, newDAONode);
                } else if (buildGraph.getIncomingEdges(n).isEmpty()
                           || requiresDAO(n, buildGraph)) {
                    // This instance either requires a session DAO, or is no
                    // longer part of the graph
                    i.remove();
                }
            }
        }
        
        return new LenskitRecommenderEngine(factory, buildGraph, 
                                            instances, config.getSPI());
    }
    
    private boolean requiresDAO(Node<Satisfaction> n, Graph<Satisfaction, Desire> graph) {
        for (Edge<Satisfaction, Desire> e: graph.getOutgoingEdges(n)) {
            Node<Satisfaction> tail = e.getTail();
            if (DataAccessObject.class.isAssignableFrom(tail.getLabel().getErasedType())) {
                // The node, n, has a direct dependency on a DAO
                return true;
            } else {
                // Check if it has an indirect dependency on a DAO
                return requiresDAO(tail, graph);
            }
        }
        
        // The node does not have any dependencies on a DAO
        return false;
    }
    
    private void pruneGraph(Graph<Satisfaction, Desire> graph, Queue<Node<Satisfaction>> removeQueue) {
        while(!removeQueue.isEmpty()) {
            Node<Satisfaction> candidate = removeQueue.poll();
            if (graph.getIncomingEdges(candidate).isEmpty()) {
                // No other node depends on this node, so we can remove it,
                // we must also flag its dependencies as removal candidates
                for (Edge<Satisfaction, Desire> e: graph.getOutgoingEdges(candidate)) {
                    removeQueue.add(e.getTail());
                }
                graph.removeNode(candidate);
            }
        }
    }
    
    private Map<Node<Satisfaction>, Object> instantiate(Graph<Satisfaction, Desire> graph, Queue<Node<Satisfaction>> removeQueue) {
        Map<Node<Satisfaction>, Object> instanceMap = new HashMap<Node<Satisfaction>, Object>();
        Set<Node<Satisfaction>> leaves = new HashSet<Node<Satisfaction>>();
        for (Node<Satisfaction> n: graph.getNodes()) {
            if (graph.getOutgoingEdges(n).isEmpty()) {
                leaves.add(n);
            }
        }
        
        instantiate(leaves, instanceMap, graph, removeQueue);
        return instanceMap;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void instantiate(Set<Node<Satisfaction>> nodes, final Map<Node<Satisfaction>, Object> instanceMap, 
                             Graph<Satisfaction, Desire> graph, Queue<Node<Satisfaction>> removeQueue) {
        // End condition is when nodes has a single node with a null satisfaction
        if (nodes.size() == 1) {
            Node<Satisfaction> root = nodes.iterator().next();
            if (root.getLabel() == null) {
                // This is the root node, so stop recursing
                return;
            }
        }
        
        // Instantiate all nodes at this level. Since recursion starts at the
        // leaves, we know that all dependencies have already been instantiated
        Set<Node<Satisfaction>> nextLevel = new HashSet<Node<Satisfaction>>();
        for (Node<Satisfaction> n: nodes) {
            if (!instanceMap.containsKey(n)) {
                // need to instantiate this node in the graph
                final Set<Edge<Satisfaction, Desire>> outgoing = graph.getOutgoingEdges(n);
                Provider<?> provider = n.getLabel().makeProvider(new Function<Desire, Provider<?>>() {
                    @Override
                    public Provider<?> apply(Desire desire) {
                        for (Edge<Satisfaction, Desire> e: outgoing) {
                            if (e.getLabel().equals(desire)) {
                                // Return the cached instance based on the tail node
                                Object instance = instanceMap.get(e.getTail());
                                return new InstanceProvider(instance);
                            }
                        }
                        
                        // Should not happen
                        throw new RuntimeException("Could not find instantiated dependency");
                    }
                });
                
                // Store created instance into the map
                instanceMap.put(n, provider.get());
                
                // Remove all transient outgoing edges from the graph
                for (Edge<Satisfaction, Desire> e: outgoing) {
                    if (e.getLabel().isTransient()) {
                        graph.removeEdge(e);
                        
                        // Push the tail node of the transient edge into the queue,
                        // there's a chance that it can be removed if it has no more
                        // incoming edges
                        removeQueue.add(e.getTail());
                    }
                }
                
                // Determine nodes that depend on this instance and set them
                // up for instantiation on the next recursion
                for (Edge<Satisfaction, Desire> e: graph.getIncomingEdges(n)) {
                    nextLevel.add(e.getHead());
                }
            }
        }
        
        // Move up the dependency hierarchy
        instantiate(nextLevel, instanceMap, graph, removeQueue);
    }
}
