import java.util.*;

import soot.SootMethod;
import soot.jimple.InvokeExpr;

class Element {
    PointsToGraph initGraph; 
    List<String> callerParams;
    String receiverObj;
    int sourceLine;
}
class CallSiteElement {
    // A callsite
    HashMap<InvokeExpr, Element> callsiteMap = new HashMap<>();
}
public class PTGWL {
    static HashMap<SootMethod, PointsToAnalysis> map = new HashMap<>();
    static HashMap<SootMethod, CallSiteElement> elementMap = new HashMap<>();
    static HashSet<SootMethod> allMethods= new HashSet<>();

    static HashSet<String> jniEscapedObjects= new HashSet<>();
    static HashSet<String> allNewObjects= new HashSet<>();
    static HashSet<String> allLoadedClasses= new HashSet<>();

    static HashSet<String> allStaticCalls = new HashSet<>();
    static HashSet<String> allVirtualCalls = new HashSet<>();
    static HashSet<String> allSpecialCalls = new HashSet<>();
    static HashSet<String> allJniCalls = new HashSet<>();


    public static void addCallsiteToUnitMap(SootMethod m, InvokeExpr u, PointsToGraph ptg, List<String> callerParams, String receiverObj, int sourceLine) {
        if (!elementMap.containsKey(m))
            elementMap.put(m, new CallSiteElement());
        if (!elementMap.get(m).callsiteMap.containsKey(u))
            elementMap.get(m).callsiteMap.put(u, new Element());
        elementMap.get(m).callsiteMap.get(u).initGraph = ptg;
        elementMap.get(m).callsiteMap.get(u).callerParams = callerParams;
        elementMap.get(m).callsiteMap.get(u).receiverObj = receiverObj;
        elementMap.get(m).callsiteMap.get(u).sourceLine = sourceLine;

    }

    public static boolean isCallsiteAlreadyAdded(SootMethod m, InvokeExpr u, int sourceLine) {
        return elementMap.containsKey(m) && elementMap.get(m).callsiteMap.containsKey(u) && elementMap.get(m).callsiteMap.get(u).sourceLine == sourceLine;  // callsite already analyzed or not
    }
    public static void print(Object o){
        System.out.println(o);
    }
    public static void printResults(){
        int totalObjectEscaped=0;
        for(SootMethod method:map.keySet()){   //for each method
            allNewObjects.addAll(map.get(method).allNewObjectsInMethod);   //add all new objects created in each method m
            for(String jni_m: map.get(method).escapedObjFromJNI.keySet()){     //for each jni method jni_m
                jniEscapedObjects.addAll(map.get(method).escapedObjFromJNI.get(jni_m));
            }
            map.get(method).allNewObjectsInMethod.removeAll(map.get(method).objectCaught);  //{new object}-{caught objects}=escaped objects
            totalObjectEscaped+=map.get(method).allNewObjectsInMethod.size();
            allLoadedClasses.addAll(map.get(method).classesLoaded);
            allStaticCalls.addAll(map.get(method).staticMethodsCalls);
            allVirtualCalls.addAll(map.get(method).virtualMethodsCalls);
            allSpecialCalls.addAll(map.get(method).privateMethodsCalls);
            allJniCalls.addAll(map.get(method).jniMethodsCalls);
        }

        System.out.println("******************");
        System.out.println("Classes loaded:         "+allLoadedClasses.size());
        System.out.println("Special calls:          "+allSpecialCalls.size());
        System.out.println("JNI calls:              "+allJniCalls.size());
        allJniCalls.forEach(System.out::println);
        System.out.println("Virtual calls:          "+allVirtualCalls.size());
        System.out.println("Static calls:           "+allStaticCalls.size());
        System.out.println("Total objects allocated:"+allNewObjects.size());
        System.out.println("Escaped objects:        "+(totalObjectEscaped+jniEscapedObjects.size()));
        System.out.println("Escaped via JNI calls:  "+jniEscapedObjects.size());
        if((totalObjectEscaped+jniEscapedObjects.size())!=0)
            System.out.println("Escaped JNI objects out of all escaped objects: "+(jniEscapedObjects.size()*100)/(totalObjectEscaped+jniEscapedObjects.size())+"%");
        System.out.println("Methods analyzed:       "+map.keySet().size());
        System.out.println("******************");
    }

}