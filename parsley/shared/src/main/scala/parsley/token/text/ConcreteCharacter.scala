/* SPDX-FileCopyrightText: © 2022 Parsley Contributors <https://github.com/j-mie6/Parsley/graphs/contributors>
 * SPDX-License-Identifier: BSD-3-Clause
 */
package parsley.token.text

import parsley.Parsley
import parsley.character.char
import parsley.token.descriptions.text.TextDesc
import parsley.token.errors.{ErrorConfig, FilterConfig, LabelConfig, LabelWithExplainConfig}

private [token] final class ConcreteCharacter(desc: TextDesc, escapes: Escape, err: ErrorConfig) extends Character {
    private val quote = char(desc.characterLiteralEnd)
    private lazy val graphic = Character.letter(desc.characterLiteralEnd, desc.escapeSequences.escBegin, allowsAllSpace = false, desc.graphicCharacter)

    private def charLetter(graphicLetter: Parsley[Int]) = {
        escapes.escapeChar <|> err.labelGraphicCharacter(graphicLetter) <|> err.verifiedCharBadCharsUsedInLiteral.checkBadChar
    }
    private def charLiteral[A](letter: Parsley[A], end: LabelConfig) = quote *> letter <* end(quote)

    override lazy val fullUtf16: Parsley[Int] = err.labelCharUtf16(charLiteral(charLetter(graphic.toUnicode), err.labelCharUtf16End))
    // this is a bit inefficient, converting to int and then back to char, but it makes it consistent, and can be optimised anyway
    private lazy val uncheckedBmpLetter = charLetter(graphic.toBmp.map(_.toInt))

    private def constrainedBmp(illegal: Int => Boolean, label: LabelWithExplainConfig, endLabel: LabelConfig, bad: FilterConfig[Int]) = {
        label(charLiteral(bad.collect(uncheckedBmpLetter) { case x if !illegal(x) => x.toChar }, endLabel))
    }

    override lazy val basicMultilingualPlane: Parsley[Char] =
        constrainedBmp(!Character.isBmpCodePoint(_), err.labelCharBasicMultilingualPlane, err.labelCharBasicMultilingualPlaneEnd,
                       err.filterCharNonBasicMultilingualPlane)
    override lazy val ascii: Parsley[Char] =
        constrainedBmp(_ > Character.MaxAscii, err.labelCharAscii, err.labelCharAsciiEnd, err.filterCharNonAscii)
    override lazy val latin1: Parsley[Char] =
        constrainedBmp(_ > Character.MaxLatin1, err.labelCharLatin1, err.labelCharLatin1End, err.filterCharNonLatin1)
}
