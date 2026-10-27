use jni::objects::{JObject, JString};
use jni::sys::jstring;
use jni::JNIEnv;

/// Small, deterministic local response engine shared with Android through JNI.
/// It is not a language model and does not contact any network service.
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

pub fn reply(input: &str, current_time: &str, current_date: &str) -> String {
    match classify(input) {
        "empty" => "Escreva uma mensagem para começar.".to_owned(),
        "greeting" =>
            "Olá. Sou a interface local do Copy. O núcleo inteligente ainda está em integração.".to_owned(),
        "time" => format!("Agora são {current_time}."),
        "date" => format!("Hoje é {current_date}."),
        "help" =>
            "Comandos locais disponíveis: perguntar a hora, perguntar a data e pedir ajuda. Perguntas gerais funcionarão quando o provedor remoto estiver ativo.".to_owned(),
        _ =>
            "Recebi sua mensagem e ela foi salva somente neste dispositivo. O núcleo de IA ainda não está conectado, então não vou inventar uma resposta nem enviar seu texto a um serviço externo.".to_owned(),
    }
}

fn read_java_string(env: &mut JNIEnv, value: &JString) -> String {
    env.get_string(value)
        .map(|value| value.into())
        .unwrap_or_default()
}

/// JNI entry point called by com.otiniel.copy.CopyRustBridge.
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_otiniel_copy_CopyRustBridge_nativeReply(
    mut env: JNIEnv,
    _receiver: JObject,
    input: JString,
    current_time: JString,
    current_date: JString,
) -> jstring {
    let input_text = read_java_string(&mut env, &input);
    let time_text = read_java_string(&mut env, &current_time);
    let date_text = read_java_string(&mut env, &current_date);
    let response = reply(&input_text, &time_text, &date_text);

    match env.new_string(response) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::{classify, reply};

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

    #[test]
    fn creates_time_and_date_responses_from_device_local_values() {
        assert_eq!(reply("Que horas são?", "14:05", "27/10/2026"), "Agora são 14:05.");
        assert_eq!(reply("Qual a data?", "14:05", "27/10/2026"), "Hoje é 27/10/2026.");
    }

    #[test]
    fn unknown_prompt_is_not_misrepresented_as_an_ai_answer() {
        let response = reply("Explique computação quântica", "14:05", "27/10/2026");
        assert!(response.contains("núcleo de IA ainda não está conectado"));
        assert!(response.contains("nem enviar seu texto a um serviço externo"));
    }
}
