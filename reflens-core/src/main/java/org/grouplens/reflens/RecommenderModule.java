/*
 * RefLens, a reference implementation of recommender algorithms.
 * Copyright 2010 Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package org.grouplens.reflens;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;

import javax.annotation.Nullable;

import org.grouplens.reflens.params.BaselinePredictor;
import org.grouplens.reflens.params.ThreadCount;
import org.grouplens.reflens.params.meta.DefaultClass;
import org.grouplens.reflens.params.meta.DefaultValue;
import org.grouplens.reflens.params.meta.PropertyName;
import org.grouplens.reflens.util.ObjectLoader;
import org.grouplens.reflens.util.TypeUtils;
import org.joda.convert.FromStringConverter;
import org.joda.convert.StringConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.util.Providers;

/**
 * TODO Document this class
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
public class RecommenderModule extends AbstractModule {
	protected final Properties properties;
	protected final StringConvert converter;
	protected final Logger logger;
	
	public RecommenderModule() {
		this(System.getProperties(), TypeUtils.CONVERTER);
	}
	
	public RecommenderModule(Properties props) {
		this(props, TypeUtils.CONVERTER);
	}

	public RecommenderModule(Properties props, StringConvert converter) {
		properties = props;
		this.converter = converter;
		logger = LoggerFactory.getLogger(this.getClass());
	}
	
	/* (non-Javadoc)
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		logger.debug("Configuring recommender module");
		configureThreadCount();
		configureBaseline();
	}
	
	protected void configureThreadCount() {
		bindProperty(int.class, ThreadCount.class,
				Runtime.getRuntime().availableProcessors());
	}
	
	@SuppressWarnings("unchecked")
	protected void configureBaseline() {
		PropertyName prop = BaselinePredictor.class.getAnnotation(PropertyName.class);
		if (prop == null) {
			addError("No property found for " + BaselinePredictor.class.getName());
			return;
		}
		
		String rnorm = properties.getProperty(prop.value());
		Class target = null;
		if (rnorm != null) {
			try {
				target = ObjectLoader.getClass(rnorm);
			} catch (ClassNotFoundException e) {
				logger.error("Class {} not found", rnorm);
			}
		}
		if (target != null) {
			if (!RatingPredictorBuilder.class.isAssignableFrom(target)) {
				for (Class c: target.getClasses()) {
					if (!c.getEnclosingClass().equals(target)) continue;
					if (RatingPredictorBuilder.class.isAssignableFrom(c)) {
						target = c;
						break;
					}
				}
			}
		}
		LinkedBindingBuilder<RatingPredictorBuilder> binder = bind(RatingPredictorBuilder.class).annotatedWith(BaselinePredictor.class);
		
		if (target != null) {
			logger.debug("Using baseline {}", target.getName());
			binder.to(target);
		} else {
			binder.toProvider(Providers.of((RatingPredictorBuilder) null));
		}
	}
	
	protected <T> void bindProperty(Class<T> type, Class<? extends Annotation> annotation) {
		bindProperty(type, annotation,
				converter.findConverter(type));
	}
	
	protected <T> void bindProperty(Class<T> type, Class<? extends Annotation> annotation,
			T defaultValue) {
		bindProperty(type, annotation, defaultValue,
				converter.findConverter(type));
	}
	
	protected <T> void bindProperty(Class<T> type, Class<? extends Annotation> annotation,
			FromStringConverter<T> parser) {
		DefaultValue dft = annotation.getAnnotation(DefaultValue.class);
		T dftV = null;
		if (dft != null)
			dftV = parser.convertFromString(type, dft.value());
		bindProperty(type, annotation, dftV, parser);
	}
	
	protected <T> void bindProperty(Class<T> type, Class<? extends Annotation> annotation,
			T defaultValue, FromStringConverter<T> parser) {
		PropertyName name = annotation.getAnnotation(PropertyName.class);
		if (name == null) {
			addError("No property name found for annotation " + annotation.getName());
			return;
		}
		
		String v = properties.getProperty(name.value());
		T value = v == null ? defaultValue : parser.convertFromString(type, v);
		
		if (value == null) {
			bind(type).annotatedWith(annotation).toProvider(Providers.of((T) null));
		} else {
			bind(type).annotatedWith(annotation).toInstance(value);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected <T> void bindClassParameter(TypeLiteral<T> type, Class<? extends Annotation> annotation) {
		DefaultClass clsA = annotation.getAnnotation(DefaultClass.class);
		Class dft = null;
		if (clsA != null)
			dft = clsA.value();
		bindClassParameter(type, annotation, dft);
	}
	
	/**
	 * Bind a dependency using a class read from a property.
	 * 
	 * @param key The Guice dependency key to bind.
	 * @param propName The name of the property containing the class name.
	 * @param dftClass The implementation to bind if the property is not set.
	 * If <tt>null</tt>, then null will be bound (and the dependency must have
	 * the {@link Nullable} annotation).  This parameter has a bare type to make
	 * it easier to use in the face of type erasure.
	 */
	@SuppressWarnings("unchecked")
	protected <T> void bindClassParameter(TypeLiteral<T> type, Class<? extends Annotation> annotation, Class dftClass) {
		PropertyName name = annotation.getAnnotation(PropertyName.class);
		if (name == null) {
			addError("No property name found for annotation " + annotation.getName());
			return;
		}
		
		String className = properties.getProperty(name.value());
		Type target = dftClass;
		if (className != null) {
			try {
				Class tgtClass = ObjectLoader.getClass(className);
				logger.debug("Binding {} to {}", name.value(), target);
				target = TypeUtils.reifyType(type.getType(), tgtClass);
				logger.debug("Reified {} to {}", tgtClass, target);
			} catch (ClassNotFoundException e) {
				addError("Class " + className + " not found");
			}
		}
		
		if (target != null) {
			bind(type).annotatedWith(annotation).to((Key) Key.get(target));
		} else {
			logger.debug("Binding {} to null", type.toString());
			bind(type).annotatedWith(annotation).toProvider(Providers.of((T) null));
		}
	}

}
