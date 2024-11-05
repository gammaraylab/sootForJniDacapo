import soot.SootMethod;
import soot.jimple.InvokeExpr;
import java.util.ArrayList;
import java.util.List;

public class CallSite {
    SootMethod method;
    InvokeExpr expr;
    List<String> callerParams;
    String receiverObj;
    Position position;

    CallSite(SootMethod method, InvokeExpr expr, List<String> callerParams, String receiverObj, Position position){
        this.method= method;
        this.expr= expr;
        this.callerParams=callerParams;
        this.receiverObj=receiverObj;
        this.position=position;
    }

    boolean equal(SootMethod method, InvokeExpr expr, ArrayList<String> callerParams, String receiverObj, Position position){
        return  this.method.toString().equals(method.toString())
                && expr.toString().equals(expr.toString())
                && this.callerParams.equals(callerParams)
                && this.receiverObj.equals(receiverObj)
                && this.position.equal(position);
    }
}