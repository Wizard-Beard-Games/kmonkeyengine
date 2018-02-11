package checkers.quals

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy


/**
 * A meta-annotation indicating that the annotated annotation is a type
 * qualifier.
 *
 * Examples of such qualifiers: `@ReadOnly`, `@NonNull`
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class TypeQualifier
