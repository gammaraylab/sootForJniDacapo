import java.util.*;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;

public class PTGWL {
    static HashMap<Unit,PointsToGraph> outSets= new HashMap<>();
    static HashMap<SootMethod, PointsToAnalysis> map = new HashMap<>();
    static HashSet<String> totalLoadedClasses = new HashSet<>();

    public static void printResults(){
        int jniEscapedObjects= 0;
        int totalNewObjects = 0;
        int totalObjectEscaped=0;
        int totalInterfaceCalls = 0;
        int totalStaticCalls = 0;
        int totalVirtualCalls = 0;
        int totalSpecialCalls = 0;
        int totalJniCalls = 0;

        Set<String> caughtTmp= new HashSet<>();
        for(SootMethod method: map.keySet()){
            totalNewObjects+=map.get(method).newObjectsInMethod.size();   //add all new objects created in each method
            caughtTmp.addAll(map.get(method).newObjectsInMethod);
            for(String jni_m: map.get(method).escapedObjFromJNI.keySet())     //objects escaping via JNI calls
                jniEscapedObjects+=map.get(method).escapedObjFromJNI.get(jni_m).size();

            totalInterfaceCalls+=map.get(method).interfaceMethodsCalls.size();
            totalSpecialCalls+=map.get(method).specialMethodsCalls.size();
            totalJniCalls+=map.get(method).jniMethodsCalls.size();
            totalVirtualCalls+=map.get(method).virtualMethodsCalls.size();
            totalStaticCalls+=map.get(method).staticMethodsCalls.size();
            caughtTmp.removeAll(map.get(method).objectCaught);  //{new object}-{caught objects}=escaped objects
            totalObjectEscaped+=caughtTmp.size();
            caughtTmp.clear();
        }

        System.out.println("******************");
        System.out.println("Classes:          "+ totalLoadedClasses.size());
        System.out.println("Interface:        "+ totalInterfaceCalls);
        System.out.println("Special:          "+ totalSpecialCalls);
        System.out.println("JNI:              "+ totalJniCalls);
        System.out.println("Virtual:          "+ totalVirtualCalls);
        System.out.println("Static:           "+ totalStaticCalls);
        System.out.println("New Obj:          "+ totalNewObjects);
        System.out.println("Escaped:          "+(totalObjectEscaped+jniEscapedObjects));
        System.out.println("Escaped via JNI:  "+jniEscapedObjects);
        if((totalObjectEscaped+jniEscapedObjects)!=0)
            System.out.println("% Escaped JNI objects: "+(jniEscapedObjects*100)/(totalObjectEscaped+jniEscapedObjects)+"%");
        System.out.println("Methods analyzed: "+map.keySet().size());
        System.out.println("-------------------");
    }

    public static void println(Object o){
        System.out.println(o);
    }
}