/*
 * LensKit, a reference implementation of recommender algorithms.
 * Copyright 2010-2011 Regents of the University of Minnesota
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
package org.grouplens.lenskit.pico;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.grouplens.lenskit.util.PrimitiveUtils;
import org.picocontainer.BindKey;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.ComponentFactory;
import org.picocontainer.ComponentMonitor;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.LifecycleStrategy;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;

public class JustInTimePicoContainer extends DefaultPicoContainer {
    private static final long serialVersionUID = 1L;
    
    private transient Map<Object, ComponentAdapter<?>> jitAdapters;

    public JustInTimePicoContainer(final ComponentFactory componentFactory,
                                   final LifecycleStrategy lifecycleStrategy,
                                   final PicoContainer parent, 
                                   final ComponentMonitor componentMonitor) {
        super(componentFactory, lifecycleStrategy, parent, componentMonitor);
    }

    public JustInTimePicoContainer(final ComponentMonitor monitor, 
                                   final LifecycleStrategy lifecycleStrategy, 
                                   final PicoContainer parent) {
        super(monitor, lifecycleStrategy, parent);
    }
    
    public JustInTimePicoContainer(final ComponentFactory componentFactory,
                                   final LifecycleStrategy lifecycleStrategy,
                                   final PicoContainer parent) {
        super(componentFactory, lifecycleStrategy, parent);
    }
    
    public JustInTimePicoContainer(final ComponentFactory componentFactory, 
                                   final PicoContainer parent) {
        super(componentFactory, parent);
    }
    
    public JustInTimePicoContainer(final ComponentMonitor monitor, 
                                   final PicoContainer parent) {
        super(monitor, parent);
    }

    public JustInTimePicoContainer(final LifecycleStrategy lifecycleStrategy, 
                                   final PicoContainer parent) {
        super(lifecycleStrategy, parent);
    }

    public JustInTimePicoContainer(final ComponentFactory componentFactory) {
        super(componentFactory);
    }

    public JustInTimePicoContainer(final ComponentMonitor monitor) {
        super(monitor);
    }

    public JustInTimePicoContainer(final PicoContainer parent) {
        super(parent);
    }

    public JustInTimePicoContainer() {
        super();
    }
    
    private boolean attemptJustInTime(Object key) {
        // Don't do any jit-binding if we already have something (and don't report failures)
        if (getComponentAdapter(key) != null || 
            (jitAdapters != null && jitAdapters.containsKey(key)))
            return true;
        
        Class<?> impl = null;
        if (key instanceof Class)
            impl = (Class<?>) key;
        else if (key instanceof BindKey)
            impl = ((BindKey<?>) key).getType();

        if (impl == null || Modifier.isAbstract(impl.getModifiers()) || Modifier.isInterface(impl.getModifiers())
            || PrimitiveUtils.isBoxedTypePrimitive(impl))
            return false;
        System.out.println(">>>> JIT Binding for " + key + " -> " + impl);
        try {
            ComponentAdapter<?> adapter = componentFactory.createComponentAdapter(componentMonitor, lifecycleStrategy, new Properties(), 
                                                                                  key, impl, (Parameter[]) null);
            if (jitAdapters == null)
                jitAdapters = new HashMap<Object, ComponentAdapter<?>>();
            jitAdapters.put(key, adapter);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
    
    @Override
    public Object getComponent(Object componentKeyOrType) {
        Object result = super.getComponent(componentKeyOrType);
        if (result == null) {
            if (attemptJustInTime(componentKeyOrType))
                return jitAdapters.get(componentKeyOrType).getComponentInstance(this, null);
        }
        return result;
    }

    @Override
    public Object getComponent(Object componentKeyOrType, Type into) {
        Object result = super.getComponent(componentKeyOrType, into);
        if (result == null) {
            if (attemptJustInTime(componentKeyOrType))
                return jitAdapters.get(componentKeyOrType).getComponentInstance(this, into);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> componentType) {
        T result = super.getComponent(componentType);
        if (result == null) {
            if (attemptJustInTime(componentType))
                return (T) jitAdapters.get(componentType).getComponentInstance(this, null);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> componentType, Class<? extends Annotation> binding) {
        if (binding == null)
            return getComponent(componentType);
        else
            return (T) getComponent(new BindKey<T>(componentType, binding));
    }
    
    @Override
    public List<Object> getComponents() {
        List<Object> all = super.getComponents();
        List<Object> withJIT = new ArrayList<Object>(all);
        
        for (ComponentAdapter<?> adapter: jitAdapters.values())
            withJIT.add(adapter.getComponentInstance(this, null));
        return withJIT;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getComponents(Class<T> type) {
        List<T> all = super.getComponents(type);
        List<T> withJIT = new ArrayList<T>(all);
        
        for (ComponentAdapter<?> adapter: jitAdapters.values()) {
            if (type.isAssignableFrom(adapter.getComponentImplementation()))
                withJIT.add((T) adapter.getComponentInstance(this, null));
        }
        return withJIT;
    }
}
