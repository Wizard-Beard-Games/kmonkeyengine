package checkers.quals

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass


/**
 * A meta-annotation to specify all the qualifiers that the given qualifier
 * is a subtype of.  This provides a declarative way to specify the type
 * qualifier hierarchy.  (Alternatively, the hierarchy can be defined
 * procedurally by subclassing [checkers.types.QualifierHierarchy] or
 * [checkers.types.TypeHierarchy].)
 *
 *
 *
 * Example:
 * <pre> @SubtypeOf( { Nullable.class } )
 * public @interface NonNull { }
</pre> *
 *
 *
 *
 *
 * If a qualified type is a subtype of the same type without any qualifier,
 * then use `Unqualified.class` in place of a type qualifier
 * class.  For example, to express that `@Encrypted *C*`
 * is a subtype of `*C*` (for every class
 * `*C*`), and likewise for `@Interned`, write:
 *
 * <pre> @SubtypeOf(Unqualified.class)
 * public @interface Encrypted { }
 *
 * &#64;SubtypeOf(Unqualified.class)
 * public @interface Interned { }
</pre> *
 *
 *
 *
 *
 * For the root type qualifier in the qualifier hierarchy (i.e., the
 * qualifier that is a supertype of all other qualifiers in the given
 * hierarchy), use an empty set of values:
 *
 * <pre> @SubtypeOf( { } )
 * public @interface Nullable { }
 *
 * &#64;SubtypeOf( {} )
 * public @interface ReadOnly { }
</pre> *
 *
 *
 *
 * Together, all the @SubtypeOf meta-annotations fully describe the type
 * qualifier hierarchy.
 * No @SubtypeOf meta-annotation is needed on (or can be written on) the
 * Unqualified pseudo-qualifier, whose position in the hierarchy is
 * inferred from the meta-annotations on the explicit qualifiers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class SubtypeOf(
        /** An array of the supertype qualifiers of the annotated qualifier  */
        vararg val value: KClass<out Annotation>)
