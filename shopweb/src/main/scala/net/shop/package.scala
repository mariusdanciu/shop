package net

import net.shift.common.ApplicativeFunctor

import scala.util.{Failure, Success, Try}
import net.shift.common.ShiftFailure

import scala.concurrent.{ExecutionContext, Future}

package object shop {
  implicit val tryApplicative = new ApplicativeFunctor[Try] {
    def unit[A](a: A): Try[A] = Try(a)
    def fmap[A, B](f: A => B): Try[A] => Try[B] = ta => ta map f
    def <*>[A, B](f: Try[A => B]): Try[A] => Try[B] = ta => ta.flatMap(a => f.map(fa => fa(a)))
  }

  implicit def futureApplicative(implicit ctx: ExecutionContext) = new ApplicativeFunctor[Future] {
    def unit[A](a: A): Future[A] = Future(a)
    def fmap[A, B](f: A => B): Future[A] => Future[B] = ta => ta map f
    def <*>[A, B](f: Future[A => B]): Future[A] => Future[B] = ta => ta.flatMap(a => f.map(fa => fa(a)))
  }

  implicit def option2Try[T](o: Option[T]): Try[T] = o match {
    case Some(v) => Success(v)
    case _       => ShiftFailure.toTry
  }

  implicit def try2Future[T](t: Try[T]): Future[T] = t match {
    case Success(v) => Future.successful(v)
    case Failure(f) => Future.failed(f)
  }
}