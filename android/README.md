# Copy Android — fundação inicial

Aplicativo Android nativo em Kotlin + Jetpack Compose, separado do runtime Rust para evitar acoplar a interface ao agente de terminal.

## O que existe nesta etapa

- Interface de conversa escura e responsiva.
- Histórico persistido localmente em SQLite.
- Comandos offline limitados para ajuda, hora e data.
- Entrada de voz via RecognizerIntent, com pedido de permissão para microfone.
- Leitura em voz alta via TextToSpeech, quando existe um mecanismo de voz configurado.
- Ação explícita para apagar a conversa local.
- Testes unitários dos comandos locais.
- Workflow de CI para executar testes e montar o APK de debug.

## Limites importantes

Esta é a fundação, não a versão final do Copy.

- O motor de IA Rust ainda não está conectado.
- Ainda não existe integração Kotlin–Rust, serviço local, roteador de modelos ou inferência local.
- Perguntas gerais ainda não são respondidas por um modelo de IA; o app informa isso explicitamente.
- Reconhecimento de voz depende do serviço disponível no Android. A preferência por offline não garante suporte offline.
- A tela é provisória e ainda precisa ser comparada com a referência exata do Figma do Copy.
- Não há acesso ao calendário, notificações, arquivos, câmera ou outros aplicativos nesta etapa.
- O APK de debug não é uma versão de produção.

## Abrir e compilar

Abra a pasta android/ no Android Studio e sincronize o projeto com Gradle. O ambiente de build precisa do JDK 17 e Android SDK Platform 37.

Com Gradle 9.6.0 e o SDK configurado:

    gradle -p android testDebugUnitTest assembleDebug

O APK de debug será gerado em android/app/build/outputs/apk/debug/app-debug.apk.

Nenhuma chave de API ou credencial é necessária para compilar esta fundação. Não inclua segredos nos arquivos de build ou no GitHub.
