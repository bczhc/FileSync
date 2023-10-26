use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::jint;

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_pers_zhc_android_filesync_JNI_test(_env: JNIEnv, _: JClass) -> jint {
    2
}