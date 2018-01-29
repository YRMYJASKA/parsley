import parsley.Parsley
import parsley.Instruction
import scala.annotation.tailrec
import scala.language.implicitConversions
import scala.collection.mutable.Buffer

package object parsley
{
    type Stack = List[Any]
    type ProgramCounter = Int
    type InstructionBuffer = Array[Instruction]
    class Frame(val ret: ProgramCounter, val instrs: InstructionBuffer)
    type CallStack = List[Frame]
    type Depth = Int
    type HandlerStack = List[(Depth, ProgramCounter, Int)]
    type Input = List[Char]
    type InputStack = List[Input]
    
    sealed trait Status
    case object Good extends Status
    case object Recover extends Status
    case object Failed extends Status

    class Context(var stack: Stack,
                  var instrs: InstructionBuffer,
                  var calls: CallStack,
                  var input: Input,
                  var inputs: InputStack,
                  var inputsz: Int,
                  var stacksz: Int,
                  var checkStack: List[Int],
                  val subs: Map[String, InstructionBuffer],
                  var status: Status,
                  var handlers: HandlerStack,
                  var depth: Int,
                  var pc: ProgramCounter)
   {
       override def toString(): String = 
       {
           s"[\n  stack=[${stack.mkString(", ")}]\n  instrs=${instrs.mkString("; ")}\n  inputs=${input.mkString(", ")}\n  status=$status\n  pc=$pc\n  depth=$depth\n  rets=${calls.map(_.ret).mkString(", ")}\n  handlers=$handlers\n]"
       }

       def fail() =
       {
           if (handlers.isEmpty) { status = Failed; this }
           else
           {
               val (depth_, handler, stacksz_) = handlers.head
               val diff = depth - depth_ - 1
               val calls_ = if (diff > 0) calls.drop(diff) else calls
               status = Recover
               if (diff >= 0)
               {
                   instrs = calls_.head.instrs
                   calls = calls_.tail
               }
               pc = handler
               handlers = handlers.tail
               stack = stack.drop(stacksz - stacksz_)
               stacksz = stacksz_
               depth = depth_
           }
       }
   }

    def runParser[A](p: Parsley[A], input: String): Result[A] = runParser[A](p, input.toList, input.size)
    def runParser[A](p: Parsley[A], input: List[Char], sz: Int): Result[A] = runParser_[A](new Context(Nil, p.instrs.toArray, Nil, input, Nil, sz, 0, Nil, p.subs.map{ case (k, v) => k -> v.toArray}, Good, Nil, 0, 0))
    def runParser[A](instrs: InstructionBuffer, subs: Map[String, InstructionBuffer], input: List[Char], sz: Int) = runParser_[A](new Context(Nil, instrs, Nil, input, Nil, sz, 0, Nil, subs, Good, Nil, 0, 0))
    
    sealed trait Result[A]
    case class Success[A](x: A) extends Result[A]
    case class Failure[A](msg: String) extends Result[A]

    @tailrec
    def runParser_[A](ctx: Context): Result[A] =
    {
        //println(ctx)
        if (ctx.status == Failed) return Failure("Unknown error")
        val pc = ctx.pc
        val instrs = ctx.instrs
        if (pc < instrs.length)
        {
            instrs(pc)(ctx)
            runParser_[A](ctx)
        }
        else if (ctx.calls.isEmpty) Success(ctx.stack.head.asInstanceOf[A])
        else
        {
            val frame = ctx.calls.head
            ctx.instrs = frame.instrs
            ctx.calls = ctx.calls.tail
            ctx.pc = frame.ret
            ctx.depth -= 1
            runParser_[A](ctx)
        }
    }
    
    @inline implicit def stringLift(str: String): Parsley[String] = parsley.Parsley.string(str)
    @inline implicit def charLift(c: Char): Parsley[Char] = parsley.Parsley.char(c)
}
