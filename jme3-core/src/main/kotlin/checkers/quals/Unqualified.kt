package checkers.quals


/**
 * A special annotation intended solely for representing an unqualified type in
 * the qualifier hierarchy, as an argument to [SubtypeOf.value],
 * in the type qualifiers declarations.
 *
 *
 *
 * Programmers cannot write this in source code.
 */
@TypeQualifier
@SubtypeOf
@Target // empty target prevents programmers from writing this in a program
annotation class Unqualified
