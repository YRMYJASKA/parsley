/* SPDX-FileCopyrightText: © 2022 Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors>
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.expr

import parsley.Parsley

/** This helper object builds values of `Ops[A, B]`, for fully heterogeneous precedence parsing.
  *
  * @since 2.2.0
  * @group Builders
  */
object GOps {
    /** This function builds an `Ops` object representing many operators found at the same precedence level, with a given fixity.
      *
      * The operators found on the level constructed by this function are heterogeneous: the type of the level below may
      * vary from the types of the values produced at this level. To make this work, a `wrap` function must be provided
      * that can transform the values from the layer below into the types generated by this layer.
      *
      * Using path-dependent typing, the given fixity describes the shape of the operators expected. For more information see
      * [[https://github.com/j-mie6/Parsley/wiki/Building-Expression-Parsers the Parsley wiki]].
      *
      * @tparam A the base type consumed by the operators.
      * @tparam B the type produced/consumed by the operators.
      * @param fixity the fixity of the operators described.
      * @param ops The operators themselves, provided variadically.
      * @param wrap the function which should be used to wrap up a value of type `A` when required
      *             (this will be at right of a left-assoc chain, left of a right-assoc chain, or
      *             the root of a prefix/postfix chain).
      * @see [[Fixity `Fixity`]]
      * @note currently a bug in scaladoc incorrect displays this functions type, it should be: `fixity.Op[A, B]`, NOT `Op[A, B]`.
      * @since 2.2.0
      */
    def apply[A, B](fixity: Fixity)(ops: Parsley[fixity.Op[A, B]]*)(implicit wrap: A => B): Ops[A, B] = fixity match {
        case InfixL  => Lefts[A, B](ops.asInstanceOf[Seq[Parsley[InfixL.Op[A, B]]]]: _*)
        case InfixR  => Rights[A, B](ops.asInstanceOf[Seq[Parsley[InfixR.Op[A, B]]]]: _*)
        case Prefix  => Prefixes[A, B](ops.asInstanceOf[Seq[Parsley[Prefix.Op[A, B]]]]: _*)
        case Postfix => Postfixes[A, B](ops.asInstanceOf[Seq[Parsley[Postfix.Op[A, B]]]]: _*)
        case InfixN  => NonAssocs[A, B](ops.asInstanceOf[Seq[Parsley[InfixN.Op[A, B]]]]: _*)
    }
}

/** This helper object builds values of `Ops[A, B]` where `A <: B`, for subtyped heterogeneous precedence parsing.
  *
  * @since 3.0.0
  * @group Builders
  */
object SOps {
    /** This function builds an `Ops` object representing many operators found at the same precedence level, with a given fixity.
      *
      * The operators found on the level constructed by this function are heterogeneous: the type of the level below may
      * vary from the types of the values produced at this level. It is constrained, however, such that values of the
      * layer below must be upcastable into types generated by this layer: one layer must be a subtype of the other.
      *
      * Using path-dependent typing, the given fixity describes the shape of the operators expected. For more information see
      * [[https://github.com/j-mie6/Parsley/wiki/Building-Expression-Parsers the Parsley wiki]].
      *
      * @tparam B the type produced/consumed by the operators, must be a supertype of `A`.
      * @tparam A the base type consumed by the operators.
      * @param fixity the fixity of the operators described.
      * @param ops the operators themselves, provided variadically.
      * @since 3.0.0
      * @see [[Fixity `Fixity`]]
      * @note currently a bug in scaladoc incorrect displays this functions type, it should be: `fixity.Op[A, B]`, NOT `Op[A, B]`.
      * @note the order of types in this method is reversed compared with [[GOps.apply]], this is due to a Scala typing issue.
      */
    def apply[B, A <: B](fixity: Fixity)(ops: Parsley[fixity.Op[A, B]]*): Ops[A, B] = GOps(fixity)(ops: _*)
}
