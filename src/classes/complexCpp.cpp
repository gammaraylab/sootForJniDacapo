#include <jni.h>
#include "Complex.h"
/*
 * Class:     Complex
 * Method:    add
 * Signature: (LComplexNumber;LComplexNumber;)LComplexNumber;
 */
JNIEXPORT jobject JNICALL Java_Complex_add
  (JNIEnv *env, jobject thisobj, jobject complexNumber_p, jobject complexNumber_q){
    jclass numberClass=env->GetObjectClass( complexNumber_p); //get the class of the parameters

    jfieldID fieldID_r=env->GetFieldID(numberClass,"r","D");  //find the field ID of the field in a particular class
    if(fieldID_r==NULL){
      printf("Cannot find Field \"r\"\n");
      return NULL;
    }

    jfieldID fieldID_i=env->GetFieldID(numberClass,"i","D");
    if(fieldID_i==NULL){
      printf("Cannot find field \"i\"\n");
      return NULL;
    }
    
    //get the field by its field id from the object p
    jdouble r1=env->GetDoubleField(complexNumber_p,fieldID_r);
    jdouble i1=env->GetDoubleField(complexNumber_p,fieldID_i);
    
    //get the field by its field id from the object q
    jdouble r2=env->GetDoubleField(complexNumber_q,fieldID_r);
    jdouble i2=env->GetDoubleField(complexNumber_q,fieldID_i);

    // create two local var to store the results
    jdouble temp_r= r1+r2;
    jdouble temp_i= i1+i2;

    //find the method ID of constructor of NumberClass 
    jmethodID numberClass_init_id=env->GetMethodID(numberClass,"<init>","(DD)V");
    //create an object of NumberClass type
    jobject temp_obj=env->NewObject(numberClass,numberClass_init_id,temp_r,temp_i);

    return temp_obj;
  }

/*
 * Class:     Complex
 * Method:    multiply
 * Signature: (LComplexNumber;LComplexNumber;)LComplexNumber;
 */
JNIEXPORT jobject JNICALL Java_Complex_multiply
  (JNIEnv *env, jobject thisobj, jobject complexNumber_p, jobject complexNumber_q){
   jclass numberClass=env->GetObjectClass( complexNumber_p); //to get the class
    jfieldID fieldID_r=env->GetFieldID(numberClass,"r","D");
    if(fieldID_r==NULL){
      printf("Cannot find Field \"r\"\n");
      return NULL;
    }

    jfieldID fieldID_i=env->GetFieldID(numberClass,"i","D");
    if(fieldID_i==NULL){
      printf("Cannot find field \"i\"\n");
      return NULL;
    }
    //get the field by its field id from the object p
    jdouble r1=env->GetDoubleField(complexNumber_p,fieldID_r);
    jdouble i1=env->GetDoubleField(complexNumber_p,fieldID_i);
    
    //get the field by its field id from the object q
    jdouble r2=env->GetDoubleField(complexNumber_q,fieldID_r);
    jdouble i2=env->GetDoubleField(complexNumber_q,fieldID_i);

    // create two local var to store the results
    jdouble temp_r= r1*r2-i1*i2;
    jdouble temp_i= r1*i2+r2*i1;

    //find the method ID of constructor of NumberClass 
    jmethodID numberClass_init_id=env->GetMethodID(numberClass,"<init>","(DD)V");
    //create an object of NumberClass type
    jobject temp_obj=env->NewObject(numberClass,numberClass_init_id,temp_r,temp_i);

    return temp_obj;
  }