# Copy Android

Aplicativo Android nativo em Kotlin + Jetpack Compose. A interface é separada do runtime Rust do Grok Build, que é um agente de terminal; a compatibilidade móvel ainda precisa ser implementada separadamente.

## O que já existe

- Interface de conversa nativa com tema escuro.
- Histórico local em SQLite. O conteúdo das mensagens é criptografado em repouso com AES-GCM e uma chave guardada pelo Android Keystore.
- Migração do histórico antigo em texto para o esquema criptografado atual (banco versão 5); o fluxo precisa ser validado em aparelho real com dados de versões anteriores.
- Comandos locais limitados para ajuda, hora e data.
- Entrada de voz pelo serviço disponível no Android e saída falada via TextToSpeech.
- Cliente remoto opcional compatível com endpoints HTTPS de OpenAI Chat Completions. O provedor é desativado por padrão.
- URL base, identificador do modelo e chave configuráveis no app. A chave é criptografada com AES-GCM e protegida pelo Android Keystore.
- Memória pessoal persistente e criptografada em SQLite, com inclusão, remoção individual e exclusão total mediante confirmação.
- Memórias não são enviadas por padrão. A inclusão em pedidos remotos exige uma confirmação separada, tem limite de 12 itens / 4.000 caracteres e pode ser revogada.
- Estilos Conciso, Equilibrado e Detalhado são aplicados às respostas remotas; no modo local os comandos continuam predefinidos.
- CI de Android passa no commit `8b36deee`: testes unitários e criação do APK de debug concluídos.

## Configurar IA remota opcional

1. Abra **Configurações**.
2. Informe a URL base HTTPS, o identificador exato do modelo e a chave fornecida pelo provedor.
3. Salve a configuração. Isso desativa a IA remota até você ativá-la novamente e revoga a autorização anterior de envio de memórias.
4. Ative **Usar IA externa** após conferir a privacidade, os termos e os possíveis custos do serviço.
5. Para enviar também memórias, habilite separadamente **Incluir memórias em pedidos remotos** e confirme a autorização na tela.

Quando a IA externa está ativada, a mensagem atual e até 15 mensagens anteriores podem ser enviadas ao endpoint configurado. O endpoint precisa implementar `POST /chat/completions` compatível com OpenAI Chat Completions. Disponibilidade, limites e cobrança dependem do provedor. O app não faz fallback silencioso para outro serviço se a chamada falhar.

A chave fica mascarada na interface. Deixar o campo vazio conserva a chave já salva; salvar uma configuração desativa o provedor e revoga a autorização para enviar memórias. Não coloque chaves no código, no GitHub, nos logs ou em capturas de tela.

## Memória pessoal e privacidade

As memórias são registradas pelo usuário e ficam cifradas no banco local, sem sincronização automática. Para incluí-las em requisições remotas, é necessário ativar a IA externa e confirmar separadamente que os dados sairão do aparelho. Desativar a memória, ativar o modo local, desligar a IA externa, alterar a configuração do provedor ou remover o provedor revoga a autorização de envio de memórias. É possível apagar memórias individualmente ou todas com confirmação.

O histórico de conversa também é cifrado em repouso com uma chave separada do Android Keystore. `allowBackup=false` impede que o banco cifrado seja restaurado sem as chaves do dispositivo por um backup comum. Ainda não há exportação/restauração segura do histórico e da memória.

## Limitações importantes

- O runtime Rust original ainda **não está integrado** à interface Android; o cliente HTTP opcional é uma camada separada de inferência remota.
- Não há modelo local nem pesos de modelo incluídos no APK.
- Sem um provedor configurado e ativado, o app só faz comandos locais limitados — não é um LLM offline.
- Um provedor remoto pode cobrar pelo uso; a compatibilidade de API não garante gratuidade.
- As memórias só entram no contexto remoto quando o usuário autoriza. O mecanismo ainda não inclui um sistema de ferramentas/automações.
- A interface Android ainda precisa ser comparada com a referência exata do Figma do Copy; não foi validada pixel a pixel.
- Calendário, notificações, arquivos, câmera e outros apps não estão integrados.
- O build verde do CI não substitui teste no POCO X5 Pro e não significa que o produto inteiro esteja pronto para distribuição.

## Compilar e testar

Requisitos: JDK 17, Android SDK Platform 36 e Gradle 9.6.0.

```bash
gradle -p android testDebugUnitTest assembleDebug
```

O APK de debug é gerado em `android/app/build/outputs/apk/debug/app-debug.apk`. Nenhuma chave de API é necessária para compilar ou usar os comandos locais.
