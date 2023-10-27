#![feature(try_blocks)]
#![feature(const_char_from_u32_unchecked)]

use std::io;
use std::io::{Cursor, Read, Write};
use std::net::{SocketAddr, TcpStream};
use std::ptr::null_mut;

use adb_sync::transfer::tcp::{Message, STREAM_MAGIC};
use adb_sync::transfer::{write_send_list_to_stream, Stream};
use adb_sync::{bincode_deserialize_compress, bincode_serialize_compress, index_dir};
use anyhow::anyhow;
use byteorder::{ReadBytesExt, WriteBytesExt, LE};
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jobject, jstring};
use jni::JNIEnv;

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
                let js = env.new_string(msg)?;
                env.call_method(
                    &callback,
                    "call",
                    "(Ljava/lang/String;)V",
                    &[JValue::Object(&js.into())],
                )?;
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

fn send<F>(dir: &str, socket_addr: SocketAddr, mut log_callback: F) -> anyhow::Result<()>
where
    F: FnMut(&str),
{
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
    log_callback("Indexing...");

    let entries = index_dir(dir, false)?;
    let mut buf = Cursor::new(Vec::new());
    bincode_serialize_compress(&mut buf, entries)?;

    log_callback("Sending list file...");
    socket.write_u32::<LE>(buf.get_ref().len() as u32)?;
    io::copy(&mut buf.into_inner().as_slice(), &mut socket)?;
    check_response!();

    let send_list_length = socket.read_u32::<LE>()?;
    let mut buf = Cursor::new(Vec::new());
    io::copy(
        &mut Read::by_ref(&mut socket).take(send_list_length as u64),
        &mut buf,
    )?;
    let send_list: Vec<Vec<u8>> = bincode_deserialize_compress(&mut buf.into_inner().as_slice())?;
    socket.write_u8(Message::Finish as u8)?;

    log_callback("Sending stream...");
    let mut stream = Stream::new(&mut socket);
    write_send_list_to_stream(&mut stream, dir, &send_list, |p| {
        log_callback(format!("{}", p.display()).as_str());
    })?;
    drop(stream);

    socket.write_u8(Message::Eof as u8)?;
    check_response!();

    drop(socket);

    log_callback("Done!");

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
