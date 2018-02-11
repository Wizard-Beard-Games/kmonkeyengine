package checkers.quals

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Specifies the annotations to be included in a type without having to provide
 * them explicitly.
 *
 * This annotation permits specifying multiple default qualifiers for more
 * than one type system.  It is necessary because Java forbids multiple
 * annotations of the same name at a single location.
 *
 * Example:
 * &nbsp;
 * `<pre>
 * &nbsp; @DefaultQualifiers({
 * &nbsp;     @DefaultQualifier("NonNull"),
 * &nbsp;     @DefaultQualifier(value = "Interned", locations = ALL_EXCEPT_LOCALS),
 * &nbsp;     @DefaultQualifier("Tainted")
 * &nbsp; })
</pre>` *
 *
 * @see DefaultQualifier
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class DefaultQualifiers(
        /** The default qualifier settings  */
        vararg val value: DefaultQualifier = arrayOf())
