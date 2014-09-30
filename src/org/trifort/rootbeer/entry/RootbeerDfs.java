/* 
 * Copyright 2012 Phil Pratt-Szeliga and other contributors
 * http://chirrup.org/
 * 
 * See the file LICENSE for copying permission.
 */

package org.trifort.rootbeer.entry;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import soot.SootField;
import soot.Type;
import soot.rbclassload.FieldSignatureUtil;
import soot.rbclassload.MethodSignature;
import soot.rbclassload.RootbeerClassLoader;
import soot.rbclassload.StringNumbers;
import soot.rbclassload.StringToType;

/**
 *
 * @author pcpratts
 */
public class RootbeerDfs {
  
  public void run(String signature) {
    
    Set<MethodSignature> visited = new HashSet<MethodSignature>();
    //System.out.println("doing rootbeer dfs: "+signature);
    LinkedList<MethodSignature> queue = new LinkedList<MethodSignature>();
    queue.add(new MethodSignature(signature));
    queue.add(new MethodSignature("<org.trifort.rootbeer.runtime.Sentinal: void <init>()>"));
    queue.add(new MethodSignature("<org.trifort.rootbeer.runtimegpu.GpuException: void <init>()>"));
    queue.add(new MethodSignature("<org.trifort.rootbeer.runtimegpu.GpuException: org.trifort.rootbeer.runtimegpu.GpuException arrayOutOfBounds(int,int,int)>"));

    CompilerSetup setup = new CompilerSetup();
    for(String method : setup.getDontDfs()){
      visited.add(new MethodSignature(method));
    }
    
    while(queue.isEmpty() == false){
      MethodSignature curr = queue.removeFirst();
      doDfsForRootbeer(curr, queue, visited);
    }
  }

  private void doDfsForRootbeer(MethodSignature signature, 
    LinkedList<MethodSignature> queue, Set<MethodSignature> visited){

    if(visited.contains(signature)){
      return;
    }
    visited.add(signature);
        
    StringToType converter = new StringToType();
    FieldSignatureUtil futil = new FieldSignatureUtil();
    
    //load all virtual methods to be followed
    ClassHierarchy class_hierarchy = RootbeerClassLoader.v().getClassHierarchy();
    List<MethodSignature> virt_methods = class_hierarchy.getVirtualMethods(signature);
    for(MethodSignature virt_method : virt_methods){
      if(virt_method.equals(signature) == false){
    	  queue.add(virt_method);
      }
    }

    //if we should follow into method, don't
    if(RootbeerClassLoader.v().dontFollow(signature)){
      return;
    }
    
    DfsInfo.v().addType(signature.getClassName());
    DfsInfo.v().addType(signature.getReturnType());
    DfsInfo.v().addMethod(signature.toString());
    
    //go into the method
    RTAValueSwitch value_switch = RootbeerClassLoader.v().getValueSwitch(signature);
    for(Integer num : value_switch.getAllTypesInteger()){
      String type_str = StringNumbers.v().getString(num);
      Type type = converter.convert(type_str);
      DfsInfo.v().addType(type);
    }    

    for(MethodSignature method_sig : value_switch.getMethodRefsHierarchy()){
      DfsInfo.v().addMethod(signature.toString());
      queue.add(method_sig);
    }

    for(String field_ref : value_switch.getFieldRefs()){
      futil.parse(field_ref);
      SootField soot_field = futil.getSootField();
      DfsInfo.v().addField(soot_field);
    }

    for(Integer num : value_switch.getInstanceOfsInteger()){
      String type_str = StringNumbers.v().getString(num);
      Type type = converter.convert(type_str);
      DfsInfo.v().addInstanceOf(type);
    }
  }
}
