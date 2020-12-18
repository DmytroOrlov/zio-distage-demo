/*
 * Copyright 2019-2020 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.macros

import scala.reflect.macros.whitebox

import com.github.ghik.silencer.silent

/**
 * Generates method accessors for a service into annotated object.
 */
private[macros] class AccessibleMacro(val c: whitebox.Context) {
  import c.universe._

  sealed trait Info {
    def service: ClassDef

    def serviceTypeParams: List[TypeDef]
  }

  protected case class PlainModuleInfo(
    module: Option[ModuleDef],
    service: ClassDef,
    serviceTypeParams: List[TypeDef]
  ) extends Info

  protected case class ModuleInfo(
    module: ModuleDef,
    service: ClassDef,
    serviceTypeParams: List[TypeDef]
  ) extends Info

  def abort(msg: String): Nothing = c.abort(c.enclosingPosition, msg)

  @silent("pattern var [^\\s]+ in method unapply is never used")
  def apply(annottees: c.Tree*): c.Tree = {

    val any: Tree       = tq"_root_.scala.Any"
    val throwable: Tree = tq"_root_.java.lang.Throwable"

    val moduleInfo = (annottees match {
      case (module: ModuleDef) :: Nil =>
        module.impl.body.collectFirst {
          case service @ ClassDef(_, name, tparams, _) if name.toTermName.toString == "Service" =>
            ModuleInfo(module, service, tparams)
        }
      case (module: ModuleDef) :: (service @ ClassDef(_, _, tparams, _)) :: Nil =>
        Some(PlainModuleInfo(Some(module), service, tparams))
      case (service @ ClassDef(_, _, tparams, _)) :: (module: ModuleDef) :: Nil =>
        Some(PlainModuleInfo(Some(module), service, tparams))
      case (service @ ClassDef(_, _, tparams, _)) :: Nil =>
        Some(PlainModuleInfo(None, service, tparams))
      case _ => None
    }).getOrElse(abort("@accessible macro can only be applied to objects containing `Service` trait."))

    sealed trait Capability
    object Capability {
      case class Effect(r: Tree, e: Tree, a: Tree)                 extends Capability
      case class Managed(r: Tree, e: Tree, a: Tree)                extends Capability
      case class Method(a: Tree)                                   extends Capability
      case class Sink(r: Tree, e: Tree, a: Tree, l: Tree, b: Tree) extends Capability
      case class Stream(r: Tree, e: Tree, a: Tree)                 extends Capability
    }

    case class TypeInfo(capability: Capability) {

      val r: Tree = capability match {
        case Capability.Effect(r, _, _)     => r
        case Capability.Managed(r, _, _)    => r
        case Capability.Sink(r, _, _, _, _) => r
        case Capability.Stream(r, _, _)     => r
        case Capability.Method(_)           => any
      }

      val e: Tree = capability match {
        case Capability.Effect(_, e, _)     => e
        case Capability.Managed(_, e, _)    => e
        case Capability.Sink(_, e, _, _, _) => e
        case Capability.Stream(_, e, _)     => e
        case Capability.Method(_)           => throwable
      }

      val a: Tree = capability match {
        case Capability.Effect(_, _, a)     => a
        case Capability.Managed(_, _, a)    => a
        case Capability.Sink(_, e, a, l, b) => tq"_root_.zio.stream.ZSink[$any, $e, $a, $l, $b]"
        case Capability.Stream(_, e, a)     => tq"_root_.zio.stream.ZStream[$any, $e, $a]"
        case Capability.Method(a)           => a
      }
    }

    def typeInfo(tree: Tree): TypeInfo =
      tree match {
        case tq"$typeName[..$typeParams]" =>
          val typeArgs  = typeParams.map(t => c.typecheck(tq"$t", c.TYPEmode, silent = true).tpe)
          val tpe       = c.typecheck(tq"$typeName[..$typeArgs]", c.TYPEmode).tpe
          val dealiased = tpe.dealias
          val replacements: List[Tree] = (tpe.typeArgs zip typeParams).collect { case (NoType, t) =>
            q"$t"
          }

          val (typeArgTrees, _) = dealiased.typeArgs.foldLeft(List.empty[Tree] -> replacements) {
            case ((acc, x :: xs), NoType) => (acc :+ x)     -> xs
            case ((acc, xs), t)           => (acc :+ q"$t") -> xs
          }

          (dealiased.typeSymbol.fullName, typeArgTrees) match {
            case ("zio.ZIO", r :: e :: a :: Nil)                    => TypeInfo(Capability.Effect(r, e, a))
            case ("zio.ZManaged", r :: e :: a :: Nil)               => TypeInfo(Capability.Managed(r, e, a))
            case ("zio.stream.ZSink", r :: e :: a :: l :: b :: Nil) => TypeInfo(Capability.Sink(r, e, a, l, b))
            case ("zio.stream.ZStream", r :: e :: a :: Nil)         => TypeInfo(Capability.Stream(r, e, a))
            case _                                                  => TypeInfo(Capability.Method(tree))
          }
      }

    def makeAccessor(
      name: TermName,
      info: TypeInfo,
      serviceTypeParams: List[TypeDef],
      typeParams: List[TypeDef],
      paramLists: List[List[ValDef]],
      isVal: Boolean,
      serviceName: TypeName
    ): Tree = {

      val serviceTypeArgs = serviceTypeParams.map(_.name)

      val returnType = info.capability match {
        case Capability.Effect(r, e, a) =>
          if (r != any) tq"_root_.zio.ZIO[_root_.zio.Has[$serviceName[..$serviceTypeArgs]] with $r, $e, $a]"
          else tq"_root_.zio.ZIO[_root_.zio.Has[$serviceName[..$serviceTypeArgs]], $e, $a]"
        case Capability.Managed(r, e, a) =>
          if (r != any) tq"_root_.zio.ZManaged[_root_.zio.Has[$serviceName[..$serviceTypeArgs]] with $r, $e, $a]"
          else tq"_root_.zio.ZManaged[_root_.zio.Has[$serviceName[..$serviceTypeArgs]], $e, $a]"
        case Capability.Stream(r, e, a) =>
          if (r != any) tq"_root_.zio.stream.ZStream[_root_.zio.Has[$serviceName[..$serviceTypeArgs]] with $r, $e, $a]"
          else tq"_root_.zio.stream.ZStream[_root_.zio.Has[$serviceName[..$serviceTypeArgs]], $e, $a]"
        case Capability.Sink(r, e, a, l, b) =>
          if (r != any) tq"_root_.zio.stream.ZSink[_root_.zio.Has[$serviceName[..$serviceTypeArgs]] with $r, $e, $a, $l, $b]"
          else tq"_root_.zio.stream.ZSink[_root_.zio.Has[$serviceName[..$serviceTypeArgs]], $e, $a, $l, $b]"
        case Capability.Method(a) =>
          tq"_root_.zio.ZIO[_root_.zio.Has[$serviceName[..$serviceTypeArgs]], $throwable, $a]"
      }

      val typeArgs = typeParams.map(_.name)

      def isRepeatedParamType(vd: ValDef) = vd.tpt match {
        case AppliedTypeTree(Select(_, nme), _) if nme == definitions.RepeatedParamClass.name => true
        case _                                                                                => false
      }

      val argNames = paramLists.map(_.map { arg =>
        if (isRepeatedParamType(arg)) q"${arg.name}: _*"
        else q"${arg.name}"
      })

      val returnValue = (info.capability, paramLists) match {
        case (_: Capability.Effect, argLists) if argLists.flatten.nonEmpty =>
          q"_root_.zio.ZIO.accessM(_.get[$serviceName[..$serviceTypeArgs]].$name[..$typeArgs](...$argNames))"
        case (_: Capability.Effect, _) =>
          q"_root_.zio.ZIO.accessM(_.get[$serviceName[..$serviceTypeArgs]].$name)"
        case (_: Capability.Managed, argLists) if argLists.flatten.nonEmpty =>
          q"_root_.zio.ZManaged.accessManaged(_.get[$serviceName[..$serviceTypeArgs]].$name[..$typeArgs](...$argNames))"
        case (_: Capability.Managed, _) =>
          q"_root_.zio.ZManaged.accessManaged(_.get[$serviceName[..$serviceTypeArgs]].$name[..$typeArgs])"
        case (_: Capability.Stream, argLists) if argLists.flatten.nonEmpty =>
          q"_root_.zio.stream.ZStream.accessStream(_.get[$serviceName[..$serviceTypeArgs]].$name[..$typeArgs](...$argNames))"
        case (_: Capability.Stream, _) =>
          q"_root_.zio.stream.ZStream.accessStream(_.get[$serviceName[..$serviceTypeArgs]].$name)"
        case (_: Capability.Sink, argLists) if argLists.flatten.nonEmpty =>
          q"_root_.zio.stream.ZSink.accessSink(_.get[$serviceName[..$serviceTypeArgs]].$name[..$typeArgs](...$argNames))"
        case (_: Capability.Sink, _) =>
          q"_root_.zio.stream.ZSink.accessSink(_.get[$serviceName[..$serviceTypeArgs]].$name)"
        case (_, argLists) if argLists.flatten.nonEmpty =>
          val argNames = argLists.map(_.map(_.name))
          q"_root_.zio.ZIO.access(_.get[$serviceName[..$serviceTypeArgs]].$name[..$typeArgs](...$argNames))"
        case (_, _) =>
          q"_root_.zio.ZIO.access(_.get[$serviceName[..$serviceTypeArgs]].$name)"
      }

      if (isVal && serviceTypeParams.isEmpty) q"val $name: $returnType = $returnValue"
      else {
        val allTypeParams =
          serviceTypeParams.map(tp => TypeDef(Modifiers(Flag.PARAM), tp.name, tp.tparams, tp.rhs)) ::: typeParams
        paramLists match {
          case Nil =>
            q"def $name[..$allTypeParams](implicit ev: _root_.izumi.reflect.Tag[$serviceName[..$serviceTypeArgs]]): $returnType = $returnValue"
          case List(Nil) =>
            q"def $name[..$allTypeParams]()(implicit ev: _root_.izumi.reflect.Tag[$serviceName[..$serviceTypeArgs]]): $returnType = $returnValue"
          case _ =>
            q"def $name[..$allTypeParams](...$paramLists)(implicit ev: _root_.izumi.reflect.Tag[$serviceName[..$serviceTypeArgs]]): $returnType = $returnValue"
        }
      }
    }

    val accessors =
      moduleInfo.service.impl.body.collect {
        case DefDef(_, termName, tparams, argLists, tree: Tree, _) if termName != TermName("$init$") =>
          makeAccessor(termName, typeInfo(tree), moduleInfo.serviceTypeParams, tparams, argLists, isVal = false, moduleInfo.service.name)

        case ValDef(_, termName, tree: Tree, _) =>
          makeAccessor(termName, typeInfo(tree), moduleInfo.serviceTypeParams, Nil, Nil, isVal = true, moduleInfo.service.name)
      }

    moduleInfo match {
      case ModuleInfo(q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }", _, _) =>
        q"""
           $mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
             ..$body
             ..$accessors
           }
         """
      case PlainModuleInfo(Some(q"$mods object $tname extends { ..$earlydefns } with ..$parents { $self => ..$body }"), service, _) =>
        q"""
          ${service}
          $mods object $tname extends { ..$earlydefns } with ..$parents { $self =>
            ..$body
            ..$accessors
          }
        """
      case PlainModuleInfo(None, service, _) =>
        q"""
          ${service}
          object ${service.name.toTermName} {
            ..$accessors
          }
        """
      case _ => abort("@accessible macro failure - could not unquote annotated object.")
    }
  }
}
