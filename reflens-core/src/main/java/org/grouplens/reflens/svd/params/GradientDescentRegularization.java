package org.grouplens.reflens.svd.params;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.grouplens.reflens.params.meta.DefaultValue;
import org.grouplens.reflens.params.meta.PropertyName;

import com.google.inject.BindingAnnotation;

/**
 * Regularization term for regularizing the gradient descent process.
 * @author Michael Ekstrand <ekstrand@cs.umn.edu>
 *
 */
@BindingAnnotation
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PropertyName("reflens.svd.GradientDescentRegularization")
@DefaultValue("0.015")
public @interface GradientDescentRegularization {

}
