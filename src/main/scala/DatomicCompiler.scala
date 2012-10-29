package reactivedatomic

import scala.reflect.macros.Context
import language.experimental.macros
import scala.tools.reflect.Eval
import scala.reflect.internal.util.{Position, OffsetPosition}

trait DatomicCompiler {


  def inception(c: Context) = {
    import c.universe.{Literal, Constant, Apply, Ident, reify, newTermName, Expr, Tree, Position}

    new {

      def incept(d: DatomicData): c.Tree = d match {
        case DString(v) => Apply(Ident(newTermName("DString")), List(Literal(Constant(v))))
        case DInt(v) => Apply(Ident(newTermName("DInt")), List(Literal(Constant(v))))
        case DLong(v) => Apply(Ident(newTermName("DLong")), List(Literal(Constant(v))))
        case DFloat(v) => Apply(Ident(newTermName("DFloat")), List(Literal(Constant(v))))
        case DDouble(v) => Apply(Ident(newTermName("DDouble")), List(Literal(Constant(v))))
        case DBoolean(v) => Apply(Ident(newTermName("DBoolean")), List(Literal(Constant(v))))
        //case DRef(v) => termSerialize(v)
        //case DBigDec(v) => v.toString
        //case DInstant(v) => v.toString
        //case DUuid(v) => v.toString
        //case DUri(v) => v.toString
        
      }

      def incept(ds: DataSource): c.Tree = ds match {
        case ImplicitDS => Ident(newTermName("ImplicitDS"))
        case ExternalDS(n) => Apply( Ident(newTermName("ExternalDS")), List(Literal(Constant(n))) )         
      }

      def incept(t: Term): c.Tree = t match {
        case Var(name) => Apply( Ident(newTermName("Var")), List(Literal(Constant(name))) )
        case Keyword(name, None) => Apply( 
          Ident(newTermName("Keyword")), 
          List(
            Literal(Constant(name))
          )
        )
        case Keyword(name, Some(Namespace(ns))) => Apply( 
          Ident(newTermName("Keyword")), 
          List(
            Literal(Constant(name)), 
            Apply(Ident(newTermName("Some")), List(
              Apply(Ident(newTermName("Namespace")), List(Literal(Constant(ns)))) 
            ))
          )
        )
        case Empty => reify("_").tree
        case Const(d: DatomicData) => Apply( Ident(newTermName("Const")), List(incept(d)) )
        case ds: DataSource => incept(ds)
      }

      def incept(df: DFunction) = Apply( Ident(newTermName("DFunction")), List(Literal(Constant(df.name))) )
      def incept(df: DPredicate) = Apply( Ident(newTermName("DPredicate")), List(Literal(Constant(df.name))) )

      def incept(b: Binding): c.Tree = b match {
        case ScalarBinding(name) => Apply( Ident(newTermName("ScalarBinding")), List(incept(name)) )
        case TupleBinding(names) => 
          Apply( 
            Ident(newTermName("TupleBinding")), 
            List(
              Apply(
                Ident(newTermName("Seq")),
                names.map(incept(_)).toList
              )
            )
          )
        case CollectionBinding(name) => Apply( Ident(newTermName("CollectionBinding")), List(incept(name)) )
        case RelationBinding(names) => 
          Apply( 
            Ident(newTermName("RelationBinding")), 
            List(
              Apply(
                Ident(newTermName("Seq")),
                names.map(incept(_)).toList
              )
            )
          )
      }

      def incept(e: Expression): c.Tree = e match {
        case PredicateExpression(df, args) => 
          Apply( 
            Ident(newTermName("PredicateExpression")), 
            List(
              incept(df),
              Apply(Ident(newTermName("Seq")), args.map(incept(_)).toList)
            )
          )
        case FunctionExpression(df, args, binding) =>
          Apply( 
            Ident(newTermName("FunctionExpression")), 
            List(
              incept(df),
              Apply(Ident(newTermName("Seq")), args.map(incept(_)).toList),
              incept(binding)
            )
          )
      }

      def incept(r: Rule): c.Tree = r match {
        case DataRule(ds, entity, attr, value) =>
          Apply( Ident(newTermName("DataRule")), 
            List(
              (if(ds == ImplicitDS) Ident(newTermName("ImplicitDS")) else Apply( Ident(newTermName("ExternalDS")), List(Literal(Constant(ds.name)))) ), 
              incept(entity), 
              incept(attr), 
              incept(value)
            ) 
          )
        case f: ExpressionRule => 
          Apply( 
            Ident(newTermName("ExpressionRule")), 
            List(incept(f.expr))
          )
          
      }

      def incept(o: Output): c.Tree = o match {
        case OutVariable(v) => Apply(Ident(newTermName("OutVariable")), List(incept(v)))
      }

      def incept(w: Where): c.Tree = 
        Apply( Ident(newTermName("Where")), List( Apply(Ident(newTermName("Seq")), w.rules.map(incept(_)).toList )) )


      def incept(i: Input): c.Tree = i match {
        case InDataSource(ds) => Apply(Ident(newTermName("InDataSource")), List(incept(ds)))
        case InVariable(v) => Apply(Ident(newTermName("InVariable")), List(incept(v)))
      }

      def incept(in: In): c.Tree = 
        Apply( Ident(newTermName("In")), List( Apply(Ident(newTermName("Seq")), in.inputs.map(incept(_)).toList )) )

      
      def incept(f: Find): c.Tree = 
        Apply( Ident(newTermName("Find")), List( Apply(Ident(newTermName("Seq")), f.outputs.map(incept(_)).toList )) )  

      def incept(q: PureQuery): c.universe.Tree = {
        Apply(
          Ident(newTermName("PureQuery")), 
          List(incept(q.find)) ++ 
          q.in.map{ in => List(Apply(Ident(newTermName("Some")), List(incept(in)))) }.getOrElse(List(Ident(newTermName("None")))) ++ 
          List(incept(q.where))
        )
      }

      def incept[A <: Args, B <: Args](q: TypedQuery[A, B]): c.universe.Tree = {
        Apply(
          Ident(newTermName("TypedQuery")), 
          List(
            incept(q.query)
          )
        )
      }
    }
  } 

  
}