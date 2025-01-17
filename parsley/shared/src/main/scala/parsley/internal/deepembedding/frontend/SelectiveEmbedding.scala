/* SPDX-FileCopyrightText: © 2022 Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors>
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.internal.deepembedding.frontend

import parsley.internal.deepembedding.backend, backend.StrictParsley

private [parsley] final class Branch[A, B, C](b: LazyParsley[Either[A, B]], p: =>LazyParsley[A => C], q: =>LazyParsley[B => C])
    extends Ternary[Either[A, B], A => C, B => C, C](b, p, q) {
    override def make(b: StrictParsley[Either[A, B]], p: StrictParsley[A => C], q: StrictParsley[B => C]): StrictParsley[C] = new backend.Branch(b, p, q)
}

private [parsley] final class If[A](b: LazyParsley[Boolean], p: =>LazyParsley[A], q: =>LazyParsley[A]) extends Ternary[Boolean, A, A, A](b, p, q) {
    override def make(b: StrictParsley[Boolean], p: StrictParsley[A], q: StrictParsley[A]): StrictParsley[A] = new backend.If(b, p, q)
}

private [parsley] final class Filter[A](p: LazyParsley[A], pred: A => Boolean) extends Unary[A, A](p) {
    override def make(p: StrictParsley[A]): StrictParsley[A] = new backend.Filter(p, pred)
}
private [parsley] final class MapFilter[A, B](p: LazyParsley[A], f: A => Option[B]) extends Unary[A, B](p) {
    override def make(p: StrictParsley[A]): StrictParsley[B] = new backend.MapFilter(p, f)
}
private [parsley] final class FilterOut[A](p: LazyParsley[A], pred: PartialFunction[A, String]) extends Unary[A, A](p) {
    override def make(p: StrictParsley[A]): StrictParsley[A] = new backend.FilterOut(p, pred)
}
private [parsley] final class GuardAgainst[A](p: LazyParsley[A], pred: PartialFunction[A, Seq[String]]) extends Unary[A, A](p) {
    override def make(p: StrictParsley[A]): StrictParsley[A] = new backend.GuardAgainst(p, pred)
}
private [parsley] final class UnexpectedWhen[A](p: LazyParsley[A], pred: PartialFunction[A, (String, Option[String])]) extends Unary[A, A](p) {
    override def make(p: StrictParsley[A]): StrictParsley[A] = new backend.UnexpectedWhen(p, pred)
}
