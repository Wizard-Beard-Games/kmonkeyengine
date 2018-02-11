package checkers.quals

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Applied to a declaration of a package, type, method, variable, etc.,
 * specifies that the given annotation should be the default.  The default is
 * applied to all types within the declaration for which no other
 * annotation is explicitly written.
 * If multiple DefaultQualifier annotations are in scope, the innermost one
 * takes precedence.
 * DefaultQualifier takes precedence over [DefaultQualifierInHierarchy].
 *
 *
 *
 * If you wish to write multiple @DefaultQualifier annotations (for
 * unrelated type systems, or with different `locations` fields) at
 * the same location, use [DefaultQualifiers].
 *
 * @see DefaultLocation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@kotlin.annotation.Target(AnnotationTarget.CONSTRUCTOR,
                          AnnotationTarget.FUNCTION,
                          AnnotationTarget.PROPERTY_GETTER,
                          AnnotationTarget.PROPERTY_SETTER,
                          AnnotationTarget.FIELD,
                          AnnotationTarget.LOCAL_VARIABLE,
                          AnnotationTarget.VALUE_PARAMETER,
                          AnnotationTarget.CLASS,
                          AnnotationTarget.FILE)
annotation class DefaultQualifier(
        /**
         * The name of the default annotation.  It may be a short name like
         * "NonNull", if an appropriate import statement exists.  Otherwise, it
         * should be fully-qualified, like "checkers.nullness.quals.NonNull".
         *
         *
         *
         * To prevent affecting other type systems, always specify an annotation
         * in your own type hierarchy.  (For example, do not set
         * "checkers.quals.Unqualified" as the default.)
         */
        val value: String,
        /** @return the locations to which the annotation should be applied
         */
        val locations: Array<DefaultLocation> = [DefaultLocation.ALL])
