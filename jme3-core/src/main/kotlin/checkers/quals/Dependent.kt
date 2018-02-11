package checkers.quals

import java.lang.annotation.Documented
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass

/**
 * Refines the qualified type of the annotated field or variable based on the
 * qualified type of the receiver.  The annotation declares a relationship
 * between multiple type qualifier hierarchies.
 *
 *
 * **Example:**
 * Consider a field, `lock`, that is only initialized if the
 * enclosing object (the receiver), is marked as `ThreadSafe`.
 * Such a field can be declared as:
 *
 * <pre>`
 * private @Nullable @Dependent(result=NonNull.class, when=ThreadSafe.class)
 * Lock lock;
`</pre> *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
annotation//@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
class Dependent(
        /**
         * The class of the refined qualifier to be applied.
         */
        val result: KClass<out Annotation>,
        /**
         * The qualifier class of the receiver that causes the `result`
         * qualifier to be applied.
         */
        val `when`: KClass<out Annotation>)
