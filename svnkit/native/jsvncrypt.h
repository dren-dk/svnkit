/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher */

#ifndef _Included_org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher
#define _Included_org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher
 * Method:    encryptData
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher_encryptData
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher
 * Method:    decryptData
 * Signature: ([B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_tmatesoft_svn_core_internal_wc_SVNWinCryptPasswordCipher_decryptData
  (JNIEnv *, jobject, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif
