package net

import net.shift.common.ApplicativeFunctor
import scala.util.Try

package object shop {
  implicit val tryApplicative = new ApplicativeFunctor[Try] {
    def unit[A](a: A): Try[A] = Try(a)
    def fmap[A, B](f: A => B): Try[A] => Try[B] = ta => ta map f
    def <*>[A, B](f: Try[A => B]): Try[A] => Try[B] = ta => ta.flatMap(a => f.map(fa => fa(a)))
  }
}