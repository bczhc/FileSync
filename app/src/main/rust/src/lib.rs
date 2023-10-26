#![feature(try_blocks)]
#![feature(const_char_from_u32_unchecked)]

use std::fs;
use std::ptr::null_mut;

use jni::objects::{JClass, JObject, JObjectArray, JString, JValueGen};
use jni::sys::{jobject, jstring};
use jni::JNIEnv;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_android_filesync_JNI_send(
    mut env: JNIEnv,
    _: JClass,
    network_dest: JString,
    dirs_jobj: JObjectArray,
    callback: JObject,
) {
    let result: anyhow::Result<()> = try {
        let network_dest = env.get_string(&network_dest)?.to_str()?.to_string();
        let dirs_length = env.get_array_length(&dirs_jobj)?;
        let mut dirs = Vec::new();
        for i in 0..dirs_length {
            let s: JString = env.get_object_array_element(&dirs_jobj, i)?.into();
            dirs.push(env.get_string(&s)?.to_str()?.to_string());
        }

        for x in dirs {
            for x in fs::read_dir(&x)? {
                let jstr = env.new_string(format!("{:?}", x))?;
                env.call_method(
                    &callback,
                    "call",
                    "(Ljava/lang/String;)V",
                    &[JValueGen::Object(&jstr.into())],
                )?;
            }
        }
    };
    if let Err(e) = result {
        env.throw(format!("Err: {}", e)).unwrap();
    }
}

const WORD_JOINER: char = unsafe { char::from_u32_unchecked(0x2060) };

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_android_filesync_JNI_joinWordJoiner(
    mut env: JNIEnv,
    _: JClass,
    s: JString,
) -> jstring {
    let result: anyhow::Result<jstring> = try {
        let js = env.get_string(&s)?;
        let mut new_string = String::new();
        for c in js.to_str()?.chars() {
            new_string.push(c);
            new_string.push(WORD_JOINER);
        }
        env.new_string(new_string)?.into_raw()
    };
    match result {
        Ok(s) => s,
        Err(e) => {
            env.throw(format!("Err: {}", e)).unwrap();
            jobject::from(null_mut())
        }
    }
}
