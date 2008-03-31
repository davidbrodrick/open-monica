#include <jni.h>
#include "atnf_atoms_mon_util_MonitorNativeLinux.h"
#include <sys/time.h>
#include <sys/resource.h>
#include <unistd.h>

JNIEXPORT jlong JNICALL Java_atnf_atoms_mon_util_MonitorNativeLinux_getCPUTime(JNIEnv *env, jclass myclass)
{
   struct rusage resource_usage;
   struct timeval user_time;
   struct timeval system_time;
   
   getrusage(RUSAGE_SELF, &resource_usage);
   
   user_time = resource_usage.ru_utime;
   system_time = resource_usage.ru_stime;
   
   return (jlong)(user_time.tv_usec + system_time.tv_usec);
}

JNIEXPORT jlong JNICALL Java_atnf_atoms_mon_util_MonitorNativeLinux_getCPUUserTime(JNIEnv *env, jclass myclass)
{
   struct rusage resource_usage;
   struct timeval user_time;
   
   getrusage(RUSAGE_SELF, &resource_usage);
   
   user_time = resource_usage.ru_utime;
   
   return (jlong)(user_time.tv_usec);
}

JNIEXPORT jlong JNICALL Java_atnf_atoms_mon_util_MonitorNativeLinux_getCPUSystemTime(JNIEnv *env, jclass myclass)
{
   struct rusage resource_usage;
   struct timeval user_time;
   struct timeval system_time;
   
   getrusage(RUSAGE_SELF, &resource_usage);
   
   system_time = resource_usage.ru_stime;
   
   return (jlong)(system_time.tv_usec);
}
