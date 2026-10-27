use jni::objects::{JObject, JString};
use jni::sys::jstring;
use jni::JNIEnv;

/// Stable, deliberately small contract between the Android UI and the Rust core.
/// These command classifications are local and deterministic; they are not an LLM.
pub fn classify(input: &str) -> &'static str {
    let normalized = input.trim().to_lowercase();

    if normalized.is_empty() {
        "empty"
    } else if matches!(
        normalized.as_str(),
        "oi" | "olá" | "ola" | "bom dia" | "boa tarde" | "boa noite"
    ) {
        "greeting"
    } else if normalized.contains("hora") {
        "time"
    } else if normalized.contains("data") || normalized.contains("que dia") {
        "date"
    } else if normalized == "ajuda" || normalized == "comandos" {
        "help"
    } else {
        "unknown"
    }
}

/// JNI entry point called by com.otiniel.copy.CopyRustBridge.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_otiniel_copy_CopyRustBridge_nativeClassify(
    mut env: JNIEnv,
    _receiver: JObject,
    input: JString,
) -> jstring {
    let input_text: String = match env.get_string(&input) {
        Ok(value) => value.into(),
        Err(_) => " ".to_owned(),
    };
    let result = classify(&input_text);

    match env.new_string(result) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::classify;

    #[test]
    fn classifies_local_commands() {
        assert_eq!(classify(""), "empty");
        assert_eq!(classify("   "), "empty");
        assert_eq!(classify("Olá"), "greeting");
        assert_eq!(classify("Que horas são?"), "time");
        assert_eq!(classify("Qual é a data?"), "date");
        assert_eq!(classify("Que dia é hoje?"), "date");
        assert_eq!(classify("ajuda"), "help");
        assert_eq!(classify("Explique computação quântica"), "unknown");
    }
}
