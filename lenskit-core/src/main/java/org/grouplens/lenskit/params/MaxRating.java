package org.grouplens.lenskit.params;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.grouplens.lenskit.params.meta.DefaultDouble;
import org.grouplens.lenskit.params.meta.Parameter;


@Documented
@DefaultDouble(5)
@Parameter(Double.class)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxRating { }