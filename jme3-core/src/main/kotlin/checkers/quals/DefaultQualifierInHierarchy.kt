package checkers.quals

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Indicates that the annotated qualifier is the default qualifier in the
 * qualifier hierarchy:  it applies if the programmer writes no explicit
 * qualifier.
 *
 *
 *
 * The [DefaultQualifier] annotation, which targets Java code elements,
 * takes precedence over `DefaultQualifierInHierarchy`.
 *
 *
 *
 * Each type qualifier hierarchy may have at most one qualifier marked as
 * `DefaultQualifierInHierarchy`.
 *
 * @see checkers.quals.DefaultQualifier
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class DefaultQualifierInHierarchy
