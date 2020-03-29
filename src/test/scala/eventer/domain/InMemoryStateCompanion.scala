package eventer.domain

import zio.{Ref, UIO}

// use only one parameter list to allow for type inference
abstract class InMemoryStateCompanion[StateT, StateWrapperT](f: Ref[StateT] => StateWrapperT, z: StateT)
    extends (Ref[StateT] => StateWrapperT) {
  def make(state: StateT): UIO[StateWrapperT] = Ref.make(state).map(f)
  val empty: UIO[StateWrapperT] = make(z)
}

abstract class InMemorySetStateCompanion[A, StateWrapperT](f: Ref[Set[A]] => StateWrapperT)
    extends InMemoryStateCompanion[Set[A], StateWrapperT](f, Set.empty[A])
abstract class InMemorySeqStateCompanion[A, StateWrapperT](f: Ref[Seq[A]] => StateWrapperT)
    extends InMemoryStateCompanion[Seq[A], StateWrapperT](f, Seq.empty[A])
abstract class InMemoryMapStateCompanion[A, B, StateWrapperT](f: Ref[Map[A, B]] => StateWrapperT)
    extends InMemoryStateCompanion[Map[A, B], StateWrapperT](f, Map.empty[A, B])
