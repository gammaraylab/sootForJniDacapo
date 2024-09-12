import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PointsToGraph {

  private final HashMap<String, HashMap<String, Set<String>>> heap; // Heap mapping
  HashMap<String, Set<String>> stack; // Stack mapping

  final String GLOBAL_SYM = "\"@global\"";
  final String NULL_SYM = "\"@null\"";

  Set<String> collectedObjects = new HashSet<>();

  Set<String> objectsToMark = new HashSet<>(); // Nodes to mark in the output

  public void eliminateHeapObj(String toRem) {
    for (String stackVar : stack.keySet())  //remove from stack
      stack.get(stackVar).remove(toRem);
    heap.remove(toRem);//remove from heap

    for (String heapObj : heap.keySet()) {
      for (String f : heap.get(heapObj).keySet())
        heap.get(heapObj).get(f).remove(toRem);
    }
  }

  void markRecursively(String marked) {
    if (objectsToMark.contains(marked))
      return;
    objectsToMark.add(marked);
    if (heap.containsKey(marked)) {
      for (String field : heap.get(marked).keySet()) {
        heap.get(marked).get(field).forEach((String o) -> markRecursively(o));
      }
    } else if (stack.containsKey(marked)) {
      stack.get(marked).forEach((String o) -> markRecursively(o));
    } else {
      assert (false);
    }
  }

  public Set<String> computeClosure() {
    Set<String> objectsToMarkCopy = new HashSet<>(objectsToMark);
    objectsToMark.clear();
    objectsToMarkCopy.forEach((String o) -> markRecursively(o));
    return objectsToMarkCopy;
  }

  public static String wrapString(String s) {
    return "\"" + s + "\"";
  }

  public PointsToGraph clone() {
    PointsToGraph clone = new PointsToGraph();
    for (String heapObj : heap.keySet()) {
      clone.heap.put(heapObj, new HashMap<>());
      for (String field : heap.get(heapObj).keySet()) {
        clone.heap.get(heapObj).put(field, new HashSet<>());
        clone.heap.get(heapObj).get(field).addAll(heap.get(heapObj).get(field));
      }
    }
    for (String stackVar : stack.keySet()) {
      clone.stack.put(stackVar, new HashSet<>());
      clone.stack.get(stackVar).addAll(stack.get(stackVar));
    }

    // clone.objectsToMark.addAll(objectsToMark);

    return clone;
  }

  public boolean equals(PointsToGraph other) {
    if (!stack.keySet().equals(other.stack.keySet()))
      return false;
    if (!heap.keySet().equals(other.heap.keySet()))
      return false;

    // Compare stack
    for (String stackVar : stack.keySet()) {
      if (!stack.get(stackVar).equals(other.stack.get(stackVar)))
        return false;
    }

    // Compare heap
    for (String heapObj : heap.keySet()) {
      HashMap<String, Set<String>> fieldMap1 = heap.get(heapObj);
      HashMap<String, Set<String>> fieldMap2 = other.heap.get(heapObj);
      if (!fieldMap1.keySet().equals(fieldMap2.keySet()))
        return false;
      for (String field : fieldMap1.keySet()) {
        if (!fieldMap1.get(field).equals(fieldMap2.get(field)))
          return false;
      }
    }

    return true;
  }

  public void add(PointsToGraph other) {
    try{
      for (String heapObj : other.heap.keySet()) {
        if (!heap.containsKey(heapObj))
          heap.put(heapObj, new HashMap<>());
        for (String field : other.heap.get(heapObj).keySet()) {
          if (!heap.get(heapObj).containsKey(field))
            heap.get(heapObj).put(field, new HashSet<>());
          heap.get(heapObj).get(field).addAll(other.heap.get(heapObj).get(field));
        }
      }
      for (String stackVar : other.stack.keySet()) {
        if (!stack.containsKey(stackVar))
          stack.put(stackVar, new HashSet<>());
        if (other.stack.get(stackVar) != null) //if the re
          stack.get(stackVar).addAll(other.stack.get(stackVar));
      }

      // objectsToMark.addAll(other.objectsToMark);
    }catch (NullPointerException ne){
//      ne.printStackTrace();
    }
  }

  public PointsToGraph() {
    heap = new HashMap<>();
    stack = new HashMap<>();
  }

  // Helpers *************************************************
  public void ensureGlobalVar(String globalVar) {
    if (!stack.containsKey(globalVar)) {
      ensureStackVar(globalVar);
      ensureHeapObj(GLOBAL_SYM);
      stackStrongUpdate(globalVar, GLOBAL_SYM);
    }
  }

  public void ensureStackVar(String localVar) {
    if (!stack.containsKey(localVar))
      stack.put(localVar, new HashSet<>());
  }

  public void ensureHeapObj(String heapObj) {
    if (!heap.containsKey(heapObj))
      heap.put(heapObj, new HashMap<>());
  }

  final String STAR_FIELD = "\"*\"";

  public void ensureField(String heapObj, String field) {
    ensureHeapObj(heapObj);
    if (!heap.get(heapObj).containsKey(field))
      heap.get(heapObj).put(field, new HashSet<>());
  }

  // a -> OBJ
  public void stackStrongUpdate(String stackVar, String heapObj) {
    ensureStackVar(stackVar);
    ensureHeapObj(heapObj);
    stack.get(stackVar).clear();
    stack.get(stackVar).add(heapObj);
  }


  // ********************************************************

  // a = new A[]
  public void handleArrayNewStatement(String stackVar, String heapObj, String heapStore) {
    stackStrongUpdate(stackVar, heapObj);
    ensureField(heapObj, STAR_FIELD);
    heap.get(heapObj).get(STAR_FIELD).add(heapStore);
  }

  // a = new A(); Simple New
  public void handleSimpleNewStatement(String stackVar, String heapObj) {
    stackStrongUpdate(stackVar, heapObj);
  }

  // a = null
  public void handleSimpleNULLStatement(String stackVar) {
    stackStrongUpdate(stackVar, NULL_SYM);
  }

  // a = b; Copy statement (strong)
  public void handleCopyStatement(String stackVar1, String stackVar2) {
    ensureStackVar(stackVar1);
    ensureStackVar(stackVar2);
    stack.get(stackVar1).clear();
    stack.get(stackVar1).addAll(stack.get(stackVar2));
  }

  // global = a
  public void handleAssignmentToGlobal(String globalVar, String localVar) {
    ensureGlobalVar(globalVar);
    ensureStackVar(localVar);
    stack.get(globalVar).clear();
    stack.get(globalVar).addAll(stack.get(localVar));
  }

  // a = global
  public void handleAssignmentFromGlobal(String stackVar1, String stackVar2) {
    ensureStackVar(stackVar1);
    ensureGlobalVar(stackVar2);
    stack.get(stackVar1).clear();
    stack.get(stackVar1).addAll(stack.get(stackVar2));
  }

  // a.f = b; Store Statement
  public void handleStoreStatement(String stackVar1, String field, String stackVar2) {
    ensureStackVar(stackVar1);
    ensureStackVar(stackVar2);
    for (String heapObj : stack.get(stackVar1)) {
      ensureField(heapObj, field);
      heap.get(heapObj).get(field).addAll(stack.get(stackVar2));
      if (heap.get(heapObj).containsKey(STAR_FIELD)) {
        heap.get(heapObj).get(STAR_FIELD).addAll(stack.get(stackVar2));
      }
    }
  }

  // a = b.f; Load statement
  public void handleLoadStatement(String stackVar1, String stackVar2, String field) {

    ensureStackVar(stackVar1);
    ensureStackVar(stackVar2);

    Set<String> b_f = new HashSet<>();

    for (String heapObj : stack.get(stackVar2)) {
      ensureHeapObj(heapObj);

      // * field or f field, if no f then null also

      if (heap.get(heapObj).containsKey(STAR_FIELD)) {
        b_f.addAll(heap.get(heapObj).get(STAR_FIELD));
      }

      if (heap.get(heapObj).containsKey(field)) {
        b_f.addAll(heap.get(heapObj).get(field));
      } else {
        ensureHeapObj(NULL_SYM);
        ensureField(heapObj, field);
        heap.get(heapObj).get(field).add(NULL_SYM);
        b_f.addAll(heap.get(heapObj).get(field));
      }

    }

    stack.get(stackVar1).clear();
    stack.get(stackVar1).addAll(b_f);
  }

  // a.f = null; Null Store Statement
  public void handleNULLStoreStatement(String stackVar1, String field) {
    ensureStackVar(stackVar1);
    ensureHeapObj(NULL_SYM);

    for (String heapObj : stack.get(stackVar1)) {
      ensureHeapObj(heapObj);
      ensureField(heapObj, field);
      heap.get(heapObj).get(field).add(NULL_SYM);
    }
  }

  Set<String> getAllHeapObjs() {
    Set<String> result = new HashSet<>();
    for (String heapObj : heap.keySet()) {
      result.add(heapObj);
      for (String field : heap.get(heapObj).keySet()) {
        for (String otherHeapObj : heap.get(heapObj).get(field)) {
          result.add(otherHeapObj);
        }
      }
    }
    return result;
  }

}