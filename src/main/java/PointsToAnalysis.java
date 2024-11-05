import java.util.*;
import soot.Body;
import soot.PatchingChain;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.BinopExpr;
import soot.jimple.ClassConstant;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.ThisRef;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.SimpleLiveLocals;

public class PointsToAnalysis {
  PatchingChain<Unit> units;
  UnitGraph uGraph;

  PointsToGraph initGraph;
  Set<String> alwaysLive = new HashSet<>();

  LiveLocals lva;

  HashMap<String, Integer> elimination = new HashMap<>();

  HashSet<String> interfaceMethodsCalls = new HashSet<>();
  HashSet<String> virtualMethodsCalls = new HashSet<>();
  HashSet<String> staticMethodsCalls = new HashSet<>();
  HashSet<String> jniMethodsCalls = new HashSet<>();
  HashSet<String> specialMethodsCalls = new HashSet<>();

  Set<String> newObjectsInMethod = new HashSet<>();
  Set<String> objectCaught = new HashSet<>();
  HashMap<SootMethod,HashSet<String>> escapedObjFromJNI= new HashMap<>();

  final boolean ASSERT_DEBUG = true;
  final boolean ANALYZE_LIB = false;

  PointsToAnalysis(){ //used to make empty ptg for methods that we dont want to analyze
    this.uGraph= null;
    this.units = null;
    this.initGraph = null;
    this.lva = null;
  }

  PointsToAnalysis(Body body) {
    this.uGraph = new ClassicCompleteUnitGraph(body);
    this.units = body.getUnits();
    this.initGraph = new PointsToGraph();
    this.lva = new SimpleLiveLocals(new BriefUnitGraph(body));
    for (Unit u : units) {
      if (u instanceof JIdentityStmt) {
        JIdentityStmt idenStmt = (JIdentityStmt) u;
        Value leftVal = idenStmt.leftBox.getValue();
        Value rightVal = idenStmt.rightBox.getValue();

        String heapObjName = null;
        String wrappedStackVal = null;

        // a = @parameter n
        if (leftVal instanceof JimpleLocal && rightVal instanceof ParameterRef) {
          JimpleLocal stackVal = (JimpleLocal) leftVal;
          ParameterRef paramref = (ParameterRef) idenStmt.rightBox.getValue();
          heapObjName = wrapString(Integer.toString(paramref.getIndex()));
          wrappedStackVal = wrapString(stackVal.getName());
        }

        // a = @thisptr
        else if (leftVal instanceof JimpleLocal && rightVal instanceof ThisRef) {
          JimpleLocal stackVal = (JimpleLocal) leftVal;
          heapObjName = wrapString("@this");
          wrappedStackVal = wrapString(stackVal.getName());
        }

        // a = @exception
        else if (leftVal instanceof JimpleLocal && rightVal instanceof JCaughtExceptionRef) {
          JimpleLocal stackVal = (JimpleLocal) leftVal;
          heapObjName = wrapString("@caughtexception_" + idenStmt.getJavaSourceStartLineNumber());
          wrappedStackVal = wrapString(stackVal.getName());
        }

        else if (ASSERT_DEBUG) {
          System.err.println("Unhandled case reached in 'IdentityStatement' : " + idenStmt.getClass());
          System.exit(1);
        }

        // Update points to graph
        if (wrappedStackVal != null) {
          this.initGraph.handleSimpleNewStatement(wrappedStackVal, heapObjName);
          this.initGraph.objectsToMark.add(heapObjName);
        }
      }
    }
  }

  PointsToAnalysis(Body body, PointsToGraph initGraph, Set<String> alwaysLive) {
    this.uGraph = new ClassicCompleteUnitGraph(body);
    this.units = body.getUnits();
    this.initGraph = initGraph;
    this.alwaysLive = alwaysLive;
    this.lva = new SimpleLiveLocals(new BriefUnitGraph(body));
  }

  //maps the formal parameter to actual parameter
  public static PointsToGraph getProcessedPTG(PatchingChain<Unit> units, PointsToGraph initial,
                                              List<String> callerParams, String receiverObj, Set<String> alwaysLiveObjs) {
    PointsToGraph result = initial.clonePtg(); //get the ptg of caller
    HashMap<String, Set<String>> oldStackMapping = result.stack;
    result.stack = new HashMap<>(); //remove all the local var from the old graph
    alwaysLiveObjs.addAll(initial.computeClosure());
    // Process formals
    for (Unit u : units) {
      if (u instanceof JIdentityStmt) {
        JIdentityStmt idenStmt = (JIdentityStmt) u;
        Value leftVal = idenStmt.leftBox.getValue();
        Value rightVal = idenStmt.rightBox.getValue();

        // a = @parameter n
        if (leftVal instanceof JimpleLocal && rightVal instanceof ParameterRef) {
          JimpleLocal stackVal = (JimpleLocal) leftVal;
          ParameterRef paramref = (ParameterRef) idenStmt.rightBox.getValue();

          String wrappedStackVal = wrapString(stackVal.getName());
          if (callerParams.size() > paramref.getIndex()) {
            String stackNameInInitGraph = callerParams.get(paramref.getIndex());
            try {
              if (stackNameInInitGraph != null && oldStackMapping.get(stackNameInInitGraph) != null) {
                alwaysLiveObjs.addAll(oldStackMapping.get(stackNameInInitGraph));
                result.stack.put(wrappedStackVal, oldStackMapping.get(stackNameInInitGraph));
              }
            } catch (NullPointerException ne) {
              ne.printStackTrace();
            }
          }
        }

        // a = @thisptr
        else if (leftVal instanceof JimpleLocal && rightVal instanceof ThisRef) {
          if (receiverObj == null) {
            JimpleLocal stackVal = (JimpleLocal) leftVal;
            String heapObjName = wrapString("@this");
            String wrappedStackVal = wrapString(stackVal.getName());
            result.handleSimpleNewStatement(wrappedStackVal, heapObjName);
            alwaysLiveObjs.add(heapObjName);
          }
          else {
            JimpleLocal stackVal = (JimpleLocal) leftVal;
            String wrappedStackVal = wrapString(stackVal.getName());
            if (oldStackMapping.containsKey(receiverObj)) {
              result.stack.put(wrappedStackVal, oldStackMapping.get(receiverObj));
              alwaysLiveObjs.addAll(oldStackMapping.get(receiverObj));
            }
          }
        }
      }
    }
    return result;
  }

  public static String wrapString(String s) {
    return "\"" + s + "\"";
  }

  // 1.
  // Statement: return a
  // Action: A stack variable at line return points to whatever the return var
  // points to
  private void handleReturnStmt(JReturnStmt retStmt, PointsToGraph ptg) {
    Value val = retStmt.getOp();
    if (val instanceof JimpleLocal) {
      JimpleLocal stackVal = (JimpleLocal) val;
      String retName = wrapString("@return_" + retStmt.getJavaSourceStartLineNumber());
      String wrappedStackVal = wrapString(stackVal.getName());
      ptg.handleCopyStatement(retName, wrappedStackVal);
    }

    else if (val instanceof Constant) {
      /* NONE */
    }

    else if (ASSERT_DEBUG) {
      System.err.println("Unhandled case reached in 'JReturnStmt' : " + retStmt.getClass());
      System.exit(1);
    }
  }

  // 2.
  // Statement: return
  // Action: NONE
  private void handleReturnVoidStmt(JReturnVoidStmt retVoidStmt, PointsToGraph ptg) {
  }

  // 3.
  // Statement: a = @this, a = @parameter, a = @caughtexception
  // Action: NONE
  private void handleIdentityStmt(JIdentityStmt idenStmt, PointsToGraph ptg) {
  }

  private PointsToGraph handleInvokeExpr(InvokeExpr invokeExpr, PointsToGraph ptg, Unit u) {
    PointsToGraph result = new PointsToGraph();
    PTGWL.totalLoadedClasses.add(invokeExpr.getMethod().getDeclaringClass());
    if (invokeExpr instanceof JDynamicInvokeExpr) {
      /* NONE */
    }
    //interface invoke
    else if (invokeExpr instanceof JInterfaceInvokeExpr) {
      Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(u);
      while (edges.hasNext()) {
        Edge edge = edges.next();
        SootMethod targetMethod = edge.tgt();
        if ((ANALYZE_LIB || !targetMethod.isJavaLibraryMethod()) && targetMethod.hasActiveBody()){
          interfaceMethodsCalls.add(targetMethod.toString()+u.getJavaSourceStartLineNumber());
          ArrayList<String> callerParams = new ArrayList<>();
          for (int i = 0; i < invokeExpr.getArgCount(); i++) {
            Value argValue = invokeExpr.getArg(i);
            if (argValue instanceof JimpleLocal)
              callerParams.add(i, wrapString(((JimpleLocal) argValue).getName()));
            else
              callerParams.add(i, null);
          }
          JInterfaceInvokeExpr interfaceInvokeExpr = (JInterfaceInvokeExpr) invokeExpr;
          String receiverObj = wrapString(((JimpleLocal) interfaceInvokeExpr.getBase()).getName());

          PointsToGraph clonePTG = ptg.clonePtg();
          lva.getLiveLocalsBefore(u).forEach((s) -> {
            clonePTG.objectsToMark.add(PointsToGraph.wrapString(s.getName()));
          });
          clonePTG.objectsToMark.addAll(alwaysLive);
          clonePTG.computeClosure();
          for (String stackVar : clonePTG.stack.keySet())
            clonePTG.objectsToMark.remove(stackVar);

          Set<String> alwaysLiveObjs = new HashSet<>();

          PointsToGraph init = PointsToAnalysis.getProcessedPTG(targetMethod.getActiveBody().getUnits(), clonePTG, callerParams, receiverObj, alwaysLiveObjs);
          PointsToAnalysis pta = PTGWL.map.getOrDefault(targetMethod,new PointsToAnalysis(targetMethod.getActiveBody(), init, alwaysLiveObjs));
          Position ps=new Position(u.getJavaSourceStartLineNumber(),u.getJavaSourceStartColumnNumber());
          CallSite cs=PTGWL.addCallSite(targetMethod, invokeExpr, callerParams, receiverObj, ps);
          try {
            pta.doAnalysis(cs);
          } catch (Exception e) {
            e.printStackTrace();
          }
          PTGWL.map.put(targetMethod, pta);
          result.add(pta.getPTGSummary());
        }
      }
    }
    // private and JNI calls
    else if (invokeExpr instanceof JSpecialInvokeExpr) {
      SootMethod outputMethod = invokeExpr.getMethod();
      if ((ANALYZE_LIB || !outputMethod.isJavaLibraryMethod()) && outputMethod.isNative()) {
        jniMethodsCalls.add(outputMethod.toString()+u.getJavaSourceStartLineNumber());
        ArrayList<String> callerParams = new ArrayList<>();
        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
          Value argValue = invokeExpr.getArg(i);
          if (argValue instanceof JimpleLocal)
            callerParams.add(i, wrapString(((JimpleLocal) argValue).getName()));
          else
            callerParams.add(i, null);
        }
        JSpecialInvokeExpr specialInvokeExpr = (JSpecialInvokeExpr) invokeExpr;
        String receiverObj = wrapString(((JimpleLocal) specialInvokeExpr.getBase()).getName());
        callerParams.add(receiverObj);

        for (String callerParam : callerParams)
          ptg.markRecursively(callerParam);

        HashSet<String> tmp=new HashSet<>();
        ptg.objectsToMark.forEach(s ->{
          if(s!=null && s.charAt(0)!='"')
            tmp.add(s);
        });
        escapedObjFromJNI.put(outputMethod,tmp);
        PTGWL.map.put(outputMethod,new PointsToAnalysis());
        Position ps= new Position(u.getJavaSourceStartLineNumber(), u.getJavaSourceStartColumnNumber());
        PTGWL.addCallSite(outputMethod,invokeExpr,callerParams,receiverObj,ps);
      }
      //constructors, private methods etc
      else {
        Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(u);
        while (edges.hasNext()) {
          Edge edge = edges.next();
          SootMethod targetMethod = edge.tgt();
          if ((ANALYZE_LIB || !targetMethod.isJavaLibraryMethod()) && targetMethod.hasActiveBody()) {
            specialMethodsCalls.add(targetMethod.toString() + u.getJavaSourceStartLineNumber());  // add method call
            // Recursively explore callee methods
            ArrayList<String> callerParams = new ArrayList<>();
            for (int i = 0; i < invokeExpr.getArgCount(); i++) {
              Value argValue = invokeExpr.getArg(i);
              if (argValue instanceof JimpleLocal)
                callerParams.add(i, wrapString(((JimpleLocal) argValue).getName()));
              else
                callerParams.add(i, null);
            }
            JSpecialInvokeExpr specialInvokeExpr = (JSpecialInvokeExpr) invokeExpr;
            String receiverObj = wrapString(((JimpleLocal) specialInvokeExpr.getBase()).getName());

            PointsToGraph clonePTG = ptg.clonePtg();
            lva.getLiveLocalsBefore(u).forEach((s) -> {
              clonePTG.objectsToMark.add(PointsToGraph.wrapString(s.getName()));
            });
            clonePTG.objectsToMark.addAll(alwaysLive);
            clonePTG.computeClosure();
            for (String stackVar : clonePTG.stack.keySet())
              clonePTG.objectsToMark.remove(stackVar);

            Set<String> alwaysLiveObjs = new HashSet<>();
            PointsToGraph init = PointsToAnalysis.getProcessedPTG(targetMethod.getActiveBody().getUnits(), clonePTG, callerParams, receiverObj, alwaysLiveObjs);
            PointsToAnalysis pta = PTGWL.map.getOrDefault(targetMethod,new PointsToAnalysis(targetMethod.getActiveBody(), init, alwaysLiveObjs));
            Position ps=new Position(u.getJavaSourceStartLineNumber(),u.getJavaSourceStartColumnNumber());
            CallSite cs= PTGWL.addCallSite(targetMethod,invokeExpr,callerParams,receiverObj, ps);
            try {
              pta.doAnalysis(cs);
            } catch (Exception e) {
              e.printStackTrace();
            }
            PTGWL.map.put(targetMethod, pta);
            result.add(pta.getPTGSummary());
          }
        }
      }
    }
    //virtual invoke
    else if (invokeExpr instanceof JVirtualInvokeExpr) {
      Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(u);
      while (edges.hasNext()) {
        Edge edge = edges.next();
        SootMethod targetMethod = edge.tgt();
        // explore callee methods
        if ((ANALYZE_LIB || !targetMethod.isJavaLibraryMethod()) && targetMethod.hasActiveBody()) {
          virtualMethodsCalls.add(targetMethod.toString()+u.getJavaSourceStartLineNumber());  // add method call
          ArrayList<String> callerParams = new ArrayList<>();
          for (int i = 0; i < invokeExpr.getArgCount(); i++) {
            Value argValue = invokeExpr.getArg(i);
            if (argValue instanceof JimpleLocal)
              callerParams.add(i, wrapString(((JimpleLocal) argValue).getName()));
            else
              callerParams.add(i, null);
          }
          JVirtualInvokeExpr virtualInvokeExpr = (JVirtualInvokeExpr) invokeExpr;
          String receiverObj = wrapString(((JimpleLocal) virtualInvokeExpr.getBase()).getName());

          PointsToGraph clonePTG = ptg.clonePtg();

          lva.getLiveLocalsBefore(u).forEach((s) -> {
            clonePTG.objectsToMark.add(PointsToGraph.wrapString(s.getName()));
          });
          clonePTG.objectsToMark.addAll(alwaysLive);
          clonePTG.computeClosure();
          for (String stackVar : clonePTG.stack.keySet())
            clonePTG.objectsToMark.remove(stackVar);

          Set<String> alwaysLiveObjs = new HashSet<>();

          PointsToGraph init = PointsToAnalysis.getProcessedPTG(targetMethod.getActiveBody().getUnits(), clonePTG, callerParams, receiverObj, alwaysLiveObjs);
          PointsToAnalysis pta = PTGWL.map.getOrDefault(targetMethod,new PointsToAnalysis(targetMethod.getActiveBody(), init, alwaysLiveObjs));
          Position ps= new Position(u.getJavaSourceStartLineNumber(), u.getJavaSourceStartColumnNumber());
          CallSite cs= PTGWL.addCallSite(targetMethod,invokeExpr,callerParams,receiverObj, ps);
          try {
            pta.doAnalysis(cs);
          } catch (Exception e) {
            e.printStackTrace();
          }
          PTGWL.map.put(targetMethod,pta);
          result.add(pta.getPTGSummary());
        }
      }
    }
    // Static Invoke
    else if (invokeExpr instanceof JStaticInvokeExpr) {
      //get method
      SootMethod targetMethod = invokeExpr.getMethod();
      if((ANALYZE_LIB || !targetMethod.isJavaLibraryMethod()) && targetMethod.hasActiveBody()){
        staticMethodsCalls.add(targetMethod.toString()+u.getJavaSourceStartLineNumber());  // add method call
        //list of parameter passed
        ArrayList<String> callerParams = new ArrayList<>();
        for (int i = 0; i < invokeExpr.getArgCount(); i++) {
          Value argValue = invokeExpr.getArg(i);
          if (argValue instanceof JimpleLocal)
            callerParams.add(i, wrapString(((JimpleLocal) argValue).getName()));
          else
            callerParams.add(i, null);
        }
        //clone the ptg of current method
        PointsToGraph clonePTG = ptg.clonePtg();

        lva.getLiveLocalsBefore(u).forEach((s) -> {
          clonePTG.objectsToMark.add(PointsToGraph.wrapString(s.getName()));
        });
        clonePTG.objectsToMark.addAll(alwaysLive);
        clonePTG.computeClosure();
        for (String stackVar : clonePTG.stack.keySet())
          clonePTG.objectsToMark.remove(stackVar);

        Set<String> alwaysLiveObjs = new HashSet<>();

        PointsToGraph init = PointsToAnalysis.getProcessedPTG(targetMethod.getActiveBody().getUnits(), clonePTG, callerParams, null, alwaysLiveObjs);
        PointsToAnalysis pta = PTGWL.map.getOrDefault(targetMethod,new PointsToAnalysis(targetMethod.getActiveBody(), init, alwaysLiveObjs));
        Position ps= new Position(u.getJavaSourceStartLineNumber(), u.getJavaSourceStartColumnNumber());
        CallSite cs= PTGWL.addCallSite(targetMethod,invokeExpr,callerParams,targetMethod.getDeclaringClass().toString(), ps);
        try {
          pta.doAnalysis(cs);
        } catch (Exception e) {
          e.printStackTrace();
        }
        PTGWL.map.put(targetMethod,pta);
        result = pta.getPTGSummary();
      }
    }

    else if (ASSERT_DEBUG) {
      System.err.println("Unhandled case reached in 'InvokeStatement'");
      System.exit(1);
    }
    return result;
  }

  // 4.
  // Statement: InvokeStatement
  // Action: At invoke statements
  private void handleInvokeStmt(JInvokeStmt invokeStmt, PointsToGraph ptg) {
    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
    PointsToGraph resultPTG = handleInvokeExpr(invokeExpr, ptg, invokeStmt);
    resultPTG.stack.clear();
    ptg.add(resultPTG);
  }

  // 5.
  // Statement: goto X
  // Action: NONE
  private void handleGotoStmt(JGotoStmt gotoStmt, PointsToGraph ptg) {
  }

  // 6.
  // Statement: if cond == true goto X
  // Action: NONE
  private void handleIfStmt(JIfStmt ifStmt, PointsToGraph ptg) {
  }

  // 7.
  // Statement: EnterMonitorStmt
  // Action: NONE
  private void handleEnterMonitor(JEnterMonitorStmt enterMonitorStmt, PointsToGraph ptg) {
  }

  // 8.
  // Statement: JExitMonitorStmt
  // Action: NONE
  private void handleExitMonitor(JExitMonitorStmt exitMonitorStmt, PointsToGraph ptg) {
  }

  // 9.
  // Statement: JNopStmt
  // Action: NONE
  private void handleNopStmt(JNopStmt jNopStmt, PointsToGraph ptg) {
  }

  // 10.
  // Statement: throw X
  // Action: Object being thrown escapes
  private void handleThrowStmt(JThrowStmt jThrowStmt, PointsToGraph ptg) {

    Value val = jThrowStmt.getOp();
    if (val instanceof JimpleLocal) {
      JimpleLocal stackVal = (JimpleLocal) val;
      String retName = wrapString("@throw_" + jThrowStmt.getJavaSourceStartLineNumber());
      String wrappedStackVal = wrapString(stackVal.getName());
      ptg.handleAssignmentToGlobal(retName, wrappedStackVal);
    }

    else if (val instanceof Constant) {
      /* NONE */
    }

    else if (ASSERT_DEBUG) {
      System.err.println("Unhandled case reached in 'JReturnStmt'");
      System.exit(1);
    }
  }

  // 11.
  // Statement: JBreakpointStmt
  // Action: NONE
  private void handleBreakpointStmt(JBreakpointStmt jBreakpointStmt, PointsToGraph ptg) {
    //ignore
  }

  // 12.
  // Statement: JAssignStmt
  // Action: Handle all assignment cases
  private void handleAssignmentStmt(JAssignStmt stmnt, PointsToGraph ptg) {
    PointsToGraph resultPTG;
    final String STAR_FIELD = "\"*\"";
    // a = new A()
    if (stmnt.leftBox.getValue() instanceof JimpleLocal && stmnt.rightBox.getValue() instanceof JNewExpr) {
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String heapObjName = "O" + stmnt.getJavaSourceStartLineNumber();
      String wrappedStackVal = wrapString(stackVal.getName());

      newObjectsInMethod.add(heapObjName);    //new object created
      ptg.handleSimpleNewStatement(wrappedStackVal, heapObjName);
    }
    // a.f = b
    else if (stmnt.leftBox.getValue() instanceof JInstanceFieldRef
            && stmnt.rightBox.getValue() instanceof JimpleLocal) {
      JInstanceFieldRef fieldref = (JInstanceFieldRef) stmnt.leftBox.getValue();
      JimpleLocal stackVal = (JimpleLocal) stmnt.rightBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      String wrapped_a = wrapString(fieldref.getBase().toString());
      String wrapped_f = wrapString(fieldref.getField().getName());
      ptg.handleStoreStatement(wrapped_a, wrapped_f, wrappedStackVal);
    }
    // a = b.f
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JInstanceFieldRef) {
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      JInstanceFieldRef fieldref = (JInstanceFieldRef) stmnt.rightBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      String wrapped_b = wrapString(fieldref.getBase().toString());
      String wrapped_f = wrapString(fieldref.getField().getName());

      ptg.handleLoadStatement(wrappedStackVal, wrapped_b, wrapped_f);
    }
    // a = b
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JimpleLocal) {
      JimpleLocal stackVal1 = (JimpleLocal) stmnt.leftBox.getValue();
      JimpleLocal stackVal2 = (JimpleLocal) stmnt.rightBox.getValue();
      String wrappedStackVal1 = wrapString(stackVal1.getName());
      String wrappedStackVal2 = wrapString(stackVal2.getName());
      ptg.handleCopyStatement(wrappedStackVal1, wrappedStackVal2);
    }
    // global = a
    else if (stmnt.leftBox.getValue() instanceof StaticFieldRef
            && stmnt.rightBox.getValue() instanceof JimpleLocal) {
      StaticFieldRef staticFieldref = (StaticFieldRef) stmnt.leftBox.getValue();
      JimpleLocal stackVal = (JimpleLocal) stmnt.rightBox.getValue();

      String wrappedGlobal = wrapString(staticFieldref.getField().getName());
      String wrappedStackVal = wrapString(stackVal.getName());

      ptg.handleAssignmentToGlobal(wrappedGlobal, wrappedStackVal);
    }
    // a = global
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof StaticFieldRef) {
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      StaticFieldRef staticFieldref = (StaticFieldRef) stmnt.rightBox.getValue();

      String wrappedStackVal = wrapString(stackVal.getName());
      String wrappedGlobal = wrapString(staticFieldref.getField().getName());

      ptg.handleAssignmentFromGlobal(wrappedStackVal, wrappedGlobal);
    }
    // a = length of b
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JLengthExpr) {
      // Ignore
    }
    // a = null
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof NullConstant) {
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      ptg.handleSimpleNULLStatement(wrappedStackVal);
    }
    // a.f = null
    else if (stmnt.leftBox.getValue() instanceof JInstanceFieldRef
            && stmnt.rightBox.getValue() instanceof NullConstant) {
      JInstanceFieldRef fieldref = (JInstanceFieldRef) stmnt.leftBox.getValue();
      String wrapped_a = wrapString(fieldref.getBase().toString());
      String wrapped_f = wrapString(fieldref.getField().getName());

      ptg.handleNULLStoreStatement(wrapped_a, wrapped_f);
    }
    // a = b.foo()
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JInterfaceInvokeExpr) {
      InvokeExpr invokeExpr = ((JInterfaceInvokeExpr) stmnt.rightBox.getValue());
      resultPTG = handleInvokeExpr(invokeExpr, ptg, stmnt); //get the results
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      HashMap<String, Set<String>> oldStack = resultPTG.stack;
      resultPTG.stack = new HashMap<>();
      resultPTG.stack.put(wrappedStackVal, oldStack.get("return"));
      ptg.add(resultPTG);
    }
    // a = <virtualInvoke>
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JVirtualInvokeExpr) {
      InvokeExpr invokeExpr = ((JVirtualInvokeExpr) stmnt.rightBox.getValue());
      resultPTG = handleInvokeExpr(invokeExpr, ptg, stmnt);
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      HashMap<String, Set<String>> oldStack = resultPTG.stack;
      resultPTG.stack = new HashMap<>();
      resultPTG.stack.put(wrappedStackVal, oldStack.get("return"));
      ptg.add(resultPTG);
    }

    // a = new Array[]
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JNewArrayExpr) {
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      String heapObjName = "O" + stmnt.getJavaSourceStartLineNumber();
      String arrayStore = "A" + stmnt.getJavaSourceStartLineNumber();
      ptg.handleArrayNewStatement(wrappedStackVal, heapObjName, arrayStore);
      newObjectsInMethod.add(arrayStore);   //add newArray obj
    }

    // [any] = constant
    else if (stmnt.rightBox.getValue() instanceof Constant) {
      /* ignore */
    }

    // [any] = a binop b
    else if (stmnt.rightBox.getValue() instanceof BinopExpr) {
      /* ignore */
    }

    // a = arr[10]
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JArrayRef) {
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      JArrayRef arrayRef = (JArrayRef) stmnt.rightBox.getValue();

//      final String STAR_FIELD = "\"*\"";
      String wrappedStackVal = wrapString(stackVal.getName());
      String wrappedArrayBase = wrapString(arrayRef.getBase().toString());

      ptg.handleLoadStatement(wrappedStackVal, wrappedArrayBase, STAR_FIELD);
    }
    // arr[10] = b
    else if (stmnt.leftBox.getValue() instanceof JArrayRef
            && stmnt.rightBox.getValue() instanceof JimpleLocal) {
      JArrayRef arrayRef = (JArrayRef) stmnt.leftBox.getValue();
      JimpleLocal stackVal = (JimpleLocal) stmnt.rightBox.getValue();

//      final String STAR_FIELD = "\"*\"";
      String wrappedStackVal = wrapString(stackVal.getName());
      String wrappedArrayBase = wrapString(arrayRef.getBase().toString());

      ptg.handleStoreStatement(wrappedArrayBase, STAR_FIELD, wrappedStackVal);
    }
    // arr[10] = class "Ltestcase/Test4;"
    else if (stmnt.leftBox.getValue() instanceof JArrayRef
            && stmnt.rightBox.getValue() instanceof ClassConstant) {
      JArrayRef arrayRef = (JArrayRef) stmnt.leftBox.getValue();
      ClassConstant classConst = (ClassConstant) stmnt.rightBox.getValue();

      String wrappedArrayBase = wrapString(arrayRef.getBase().toString());
      String classConstStr = wrapString(classConst.getValue());
      String classConstStrObj = wrapString("@" + classConst.getValue());

      ptg.stackStrongUpdate(classConstStr, classConstStrObj);
//      final String STAR_FIELD = "\"*\"";
      ptg.handleStoreStatement(wrappedArrayBase, STAR_FIELD, classConstStr);
    }
    // r0.f = 10
    else if (stmnt.leftBox.getValue() instanceof JInstanceFieldRef
            && stmnt.rightBox.getValue() instanceof IntConstant) {
      // ignore
    }
    // a = <static invoke>
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JStaticInvokeExpr) {
      InvokeExpr invokeExpr = ((JStaticInvokeExpr) stmnt.rightBox.getValue());
      resultPTG = handleInvokeExpr(invokeExpr, ptg, stmnt);
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      HashMap<String, Set<String>> oldStack = resultPTG.stack;
      resultPTG.stack = new HashMap<>();
      resultPTG.stack.put(wrappedStackVal, oldStack.get("return"));
      ptg.add(resultPTG);
    }
    // a = (A) b
    else if (stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JCastExpr) {
      JimpleLocal s = (JimpleLocal) stmnt.leftBox.getValue();
      String w1 = wrapString(s.getName());
      JCastExpr castExpr = (JCastExpr) stmnt.rightBox.getValue();
      String w2 = wrapString(castExpr.getOp().toString());
      ptg.handleCopyStatement(w1, w2);
    }
    //a=<specialinvoke>
    else if(stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JSpecialInvokeExpr) {
      InvokeExpr invokeExpr = ((JSpecialInvokeExpr) stmnt.rightBox.getValue());
      resultPTG = handleInvokeExpr(invokeExpr, ptg, stmnt); //get the results
      JimpleLocal stackVal = (JimpleLocal) stmnt.leftBox.getValue();
      String wrappedStackVal = wrapString(stackVal.getName());
      HashMap<String, Set<String>> oldStack = resultPTG.stack;
      resultPTG.stack = new HashMap<>();
      resultPTG.stack.put(wrappedStackVal, oldStack.get("return"));
      ptg.add(resultPTG);
    }
    else if(stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JInstanceOfExpr) {
      //ignore
    }
    else if(stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JNegExpr) {
      //ignore
    }
    else if(stmnt.leftBox.getValue() instanceof JimpleLocal
            && stmnt.rightBox.getValue() instanceof JNewMultiArrayExpr) {
      //ignore
    }
    else if (ASSERT_DEBUG) {
      System.err.println("Unhandled statement reached 'JAssignStmt'");
      System.err.println(stmnt);
    }
  } //4 handleInvokeStmnt

  // 13.
  // Statement: JLookupSwitchStmt
  // Action: NONE
  private void handleLookupSwitchStmt(JLookupSwitchStmt jLookupSwitchStmt, PointsToGraph ptg) {
  }

  // 14.
  // Statement: JTableSwitchStmt
  // Action: NONE
  private void handleTableSwitchStmt(JTableSwitchStmt jTableSwitchStmt, PointsToGraph ptg) {
  }

  // 15.
  // Statement: JRetStmt
  // Action: NONE
  private void handleRetStmt(JRetStmt jRetStmt, PointsToGraph ptg) {
    //ignore
  }

  // ***********************************************************************************************

  private void flowFunction(Unit u, PointsToGraph ptg) {
    // 1. ReturnStmt<JReturnStmt>
    if (u instanceof JReturnStmt)
      handleReturnStmt((JReturnStmt) u, ptg);
      // 2. ReturnVoid<JReturnVoidStmt>
    else if (u instanceof JReturnVoidStmt)
      handleReturnVoidStmt((JReturnVoidStmt) u, ptg);
      // 3. IdentityStmt<JIdentityStmt>
    else if (u instanceof JIdentityStmt)
      handleIdentityStmt((JIdentityStmt) u, ptg);
      // 4. InvokeStmt<JInvokeStmt> === invoke InvokeExpr
    else if (u instanceof JInvokeStmt)
      handleInvokeStmt((JInvokeStmt) u, ptg);
      // 5. gotoStmt<JGotoStmt>
    else if (u instanceof JGotoStmt)
      handleGotoStmt((JGotoStmt) u, ptg);
      // 6. ifStmt<JIfStmt>
    else if (u instanceof JIfStmt)
      handleIfStmt((JIfStmt) u, ptg);
      // 7. MonitorEnterStmt<JEnterMonitorStmt>
    else if (u instanceof JEnterMonitorStmt)
      handleEnterMonitor((JEnterMonitorStmt) u, ptg);
      // 8. MonitorExitStmt<JExitMonitorStmt>
    else if (u instanceof JExitMonitorStmt)
      handleExitMonitor((JExitMonitorStmt) u, ptg);
      // 9. nopStmt<JNopStmt>
    else if (u instanceof JNopStmt)
      handleNopStmt((JNopStmt) u, ptg);
      // 10. ThrowStmt<JThrowStmt>
    else if (u instanceof JThrowStmt)
      handleThrowStmt((JThrowStmt) u, ptg);
      // 11. BreakpointStmt<JBreakpointStmt>
    else if (u instanceof JBreakpointStmt)
      handleBreakpointStmt((JBreakpointStmt) u, ptg);
      // 12. AssignmentStatement<JAssignStmt>
    else if (u instanceof JAssignStmt)
      handleAssignmentStmt((JAssignStmt) u, ptg);
      // 13. LookupSwitch<JLookupSwitchStmt>
    else if (u instanceof JLookupSwitchStmt)
      handleLookupSwitchStmt((JLookupSwitchStmt) u, ptg);
      // 14. TableSwitch<JTableSwitchStmt>
    else if (u instanceof JTableSwitchStmt)
      handleTableSwitchStmt((JTableSwitchStmt) u, ptg);
      // 15. JRetStmt -- deprecated
      // Wiki(https://en.wikipedia.org/wiki/List_of_Java_bytecode_instructions#endnote_Deprecated)
    else if (u instanceof JRetStmt)
      handleRetStmt((JRetStmt) u, ptg);

    else if (ASSERT_DEBUG) {
      System.err.println("Unhandled statement reached '" + u.getClass() + "'");
      assert (false);
    }

    if (u instanceof JReturnStmt) {
      ptg.stack.keySet().forEach((s) -> {
        if (s.contains("@return")) {
          ptg.objectsToMark.add(s);
        }
      });
    }
    lva.getLiveLocalsAfter(u).forEach((s) -> {
      ptg.objectsToMark.add(PointsToGraph.wrapString(s.getName()));
    });
    ptg.objectsToMark.addAll(alwaysLive);
    ptg.computeClosure();
    Set<String> usedObjs = ptg.computeClosure();
    Set<String> GC = ptg.getAllHeapObjs();
    GC.removeAll(usedObjs);
    objectCaught.addAll(GC);
    GC.forEach((h) -> {
      ptg.eliminateHeapObj(h);
      elimination.put(h, u.getJavaSourceStartLineNumber());
      ptg.collectedObjects.add(h);
    });
  }

  // ***********************************************************************************************
  private PointsToGraph PTGSummary = null;

  private PointsToGraph getPTGSummary() {
    return PTGSummary;
  }

  public void doAnalysis(CallSite callSite) throws Exception {
    PTGWL.printResults();
    if(PTGWL.prevCount==PTGWL.callSiteMap.size()){
      PTGWL.depth--;
//      if(PTGWL.depth<PTGWL.THRESHOLD/2)
//        println(callSite.method.getSignature()+":"+callSite.position.lineNo);
    }
    else
      PTGWL.depth =PTGWL.THRESHOLD;
    if(PTGWL.depth <0){
      PTGWL.printResults();
      println("callsitemap:"+PTGWL.prevCount);
      return;
//      System.exit(2);
    }
    PTGWL.prevCount=PTGWL.callSiteMap.size();

    List<Unit> worklist = new ArrayList<>();
    // First iteration over the CFG, worklist initialization
    for (Unit currUnit : units) {
      PointsToGraph currentOutSet = new PointsToGraph();

      // Starting point of the function will not have any predecessors, we will take a
      // meet of all the incoming PTGs
      if (uGraph.getPredsOf(currUnit).isEmpty())
        currentOutSet.add(initGraph);
      else // take union of all the outset of predecessors
        for (Unit incoming : uGraph.getPredsOf(currUnit)) {
          currentOutSet.add(PTGWL.callSiteMap.get(callSite).getFlow(incoming));
        }

      //analyze the current unit and update the current out flow set
      if(PTGWL.callSiteMap.get(callSite).getFlow(currUnit).isEmpty() /*|| !PTGWL.callSiteMap.get(callSite).getFlow(currUnit).equals(currentOutSet) */) {
        PTGWL.callSiteMap.get(callSite).setFlow(currUnit, currentOutSet);
        flowFunction(currUnit, currentOutSet);
      }

      // Add successors to worklist if both the flow set are not same
      if (!PTGWL.callSiteMap.get(callSite).getFlow(currUnit).equals(currentOutSet)) {
        PTGWL.callSiteMap.get(callSite).setFlow(currUnit, currentOutSet);
        worklist.addAll(uGraph.getSuccsOf(currUnit));
      }
    }
    while (!worklist.isEmpty()) {
      // Pop one unit from the worklist
      PointsToGraph currentOutSet = new PointsToGraph();
      Unit currUnit = worklist.iterator().next();
      worklist.remove(currUnit);

      // Starting point of the function will not have any predecessors, we will take a
      // meet of all the incoming PTGs
      if (uGraph.getPredsOf(currUnit).isEmpty())
        currentOutSet.add(initGraph);
      else // take union of all the outset of predecessors
        for (Unit incoming : uGraph.getPredsOf(currUnit)) {
          currentOutSet.add(PTGWL.callSiteMap.get(callSite).getFlow(incoming));
        }

      //analyze the current unit and update the current out flow set
      if(PTGWL.callSiteMap.get(callSite).getFlow(currUnit).isEmpty()) {
        PTGWL.callSiteMap.get(callSite).setFlow(currUnit, currentOutSet);
        flowFunction(currUnit, currentOutSet);
      }

      // Add successors to worklist if both the flow set are not same
      if (!PTGWL.callSiteMap.get(callSite).getFlow(currUnit).equals(currentOutSet)) {
        PTGWL.callSiteMap.get(callSite).setFlow(currUnit, currentOutSet);
        worklist.addAll(uGraph.getSuccsOf(currUnit));
      }
    }

    // Merge all return statements
    PointsToGraph summary = new PointsToGraph();
    for (Unit u : units) {
      if (u instanceof JReturnVoidStmt || u instanceof JRetStmt || u instanceof JReturnStmt) {
        summary.add(PTGWL.callSiteMap.get(callSite).getFlow(u));
      }
    }

    // clear local stack mappings
    Set<String> finalStackReturns = new HashSet<>();
    HashMap<String, Set<String>> oldStack = summary.stack;
    summary.stack = new HashMap<>();
    for (String stackVar : oldStack.keySet()) {
      if (stackVar.contains("@return"))
        finalStackReturns.addAll(oldStack.get(stackVar));
    }
    if (!finalStackReturns.isEmpty())
      summary.stack.put("return", finalStackReturns);

    // store union of PTGs are return statements
    PTGSummary = summary;
  }

  void println(Object o){
    System.out.println(o);
  }
}

