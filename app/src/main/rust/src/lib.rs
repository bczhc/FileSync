#![feature(try_blocks)]
#![feature(const_char_from_u32_unchecked)]

use std::io::{Cursor, Read, Write};
use std::net::{SocketAddr, TcpStream};
use std::path::Path;
use std::ptr::null_mut;
use std::sync::Mutex;
use std::{io, panic};

use crate::helper::jni_log;
use adb_sync::transfer::tcp::{Message, SendConfigs, STREAM_MAGIC};
use adb_sync::transfer::{write_send_list_to_stream, Stream};
use adb_sync::{bincode_deserialize_compress, bincode_serialize_compress, index_dir};
use anyhow::anyhow;
use bincode::Encode;
use byteorder::{ReadBytesExt, WriteBytesExt, LE};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jobject, jstring};
use jni::{JNIEnv, JavaVM};
use once_cell::sync::Lazy;

mod helper;

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_android_filesync_JNI_send(
    mut env: JNIEnv,
    _: JClass,
    network_dest: JString,
    dir: JString,
    callback: JObject,
) {
    let result: anyhow::Result<()> = try {
        let network_dest = env.get_string(&network_dest)?.to_str()?.to_string();
        let dir = env.get_string(&dir)?.to_str()?.to_string();

        let socket_addr = network_dest.parse::<SocketAddr>()?;

        send(&dir, socket_addr, |msg| {
            let result: anyhow::Result<()> = try {
                match msg {
                    CallbackType::Message(msg) => {
                        let js = env.new_string(msg)?;
                        env.call_method(
                            &callback,
                            "message",
                            "(Ljava/lang/String;)V",
                            &[JValue::Object(&js.into())],
                        )?;
                    }
                    CallbackType::Progress(path, n, total) => {
                        let js = env.new_string(path)?;
                        env.call_method(
                            &callback,
                            "progress",
                            "(Ljava/lang/String;II)V",
                            &[
                                JValue::Object(&js.into()),
                                JValue::Int(n.try_into()?),
                                JValue::Int(total.try_into()?),
                            ],
                        )?;
                    }
                }
            };
            if let Err(e) = result {
                // let's just assert callback calls won't fail
                panic!("Callback error: {}", e);
            }
        })?;
    };
    if let Err(e) = result {
        env.throw(format!("Err: {}", e)).unwrap();
    }
}

enum CallbackType<'a> {
    Message(&'a str),
    Progress(&'a str, usize, usize),
}

fn send<F>(dir: &str, socket_addr: SocketAddr, mut log_callback: F) -> anyhow::Result<()>
where
    F: FnMut(CallbackType),
{
    macro_rules! log_msg {
        ($msg:expr) => {
            log_callback(CallbackType::Message($msg))
        };
    }

    let mut socket = TcpStream::connect(socket_addr)?;
    socket.write_all(STREAM_MAGIC)?;

    macro_rules! check_response {
        () => {
            let message = socket.read_u8()?;
            if message != Message::Finish as u8 {
                Err(anyhow!("Unexpected incoming response: {}", message))?;
            }
        };
    }
    check_response!();
    log_msg!("Indexing...");

    let entries = index_dir(dir, false)?;

    log_msg!("Sending list file...");
    send_serializable(&mut socket, &entries)?;
    check_response!();

    let dir_basename = Path::new(dir).file_name().map(|x| {
        // TODO: non-UTF8 names
        String::from(x.to_str().unwrap())
    });
    let send_configs = SendConfigs {
        src_basename: dir_basename,
    };
    send_serializable(&mut socket, &send_configs)?;
    check_response!();

    let send_list_length = socket.read_u32::<LE>()?;
    let mut buf = Cursor::new(Vec::new());
    io::copy(
        &mut Read::by_ref(&mut socket).take(send_list_length as u64),
        &mut buf,
    )?;
    let send_list: Vec<Vec<u8>> = bincode_deserialize_compress(&mut buf.into_inner().as_slice())?;
    socket.write_u8(Message::Finish as u8)?;

    log_msg!("Sending stream...");
    let mut stream = Stream::new(&mut socket);
    write_send_list_to_stream(&mut stream, dir, &send_list, |p, (n, total)| {
        log_callback(CallbackType::Progress(
            format!("{}", p.display()).as_str(),
            n,
            total,
        ))
    })?;
    drop(stream);

    socket.write_u8(Message::Eof as u8)?;
    check_response!();

    drop(socket);

    log_msg!("Done!");

    Ok(())
}

fn send_serializable<W, E>(mut writer: W, obj: &E) -> anyhow::Result<()>
where
    W: Write,
    E: Encode,
{
    let mut buf = Cursor::new(Vec::new());
    bincode_serialize_compress(&mut buf, obj)?;
    writer.write_u32::<LE>(buf.get_ref().len() as u32)?;
    io::copy(&mut buf.into_inner().as_slice(), &mut writer)?;
    Ok(())
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

static JAVA_VM: Lazy<Mutex<Option<JavaVM>>> = Lazy::new(|| Mutex::new(None));

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_pers_zhc_android_filesync_JNI_initJni(env: JNIEnv, _: JClass) {
    let jvm = env.get_java_vm().unwrap();
    JAVA_VM.lock().unwrap().replace(jvm);
    panic::set_hook(Box::new(|x| {
        let info = format!("{}", x);
        let guard = JAVA_VM.lock().unwrap();
        let jvm = guard.as_ref().unwrap();
        let mut env = jvm.attach_current_thread().unwrap();
        let err_text = format!("Rust panic!!\n{}", info);
        jni_log(&err_text).unwrap();
        let _ = env.throw(err_text);
    }));
}
