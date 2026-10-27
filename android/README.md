# Copy Android

Aplicativo Android nativo em Kotlin + Jetpack Compose. A interface é separada do runtime Rust do Grok Build porque o runtime original foi criado para um agente de terminal; a compatibilidade móvel precisa ser implementada e testada separadamente.

## Implementado na fundação

- Interface Android com tema escuro e identidade Copy.
- Histórico de conversa persistido localmente em SQLite.
- Comandos offline limitados para ajuda, hora e data.
- Reconhecimento de fala via serviço disponível no Android e resposta falada via TextToSpeech.
- Limpeza explícita do histórico local.
- Cliente opcional para APIs de chat compatíveis com OpenAI.
- Chave de API cifrada com AES-GCM; chave de cifragem protegida pelo Android Keystore.
- Validação de URL HTTPS, timeouts, limite de resposta, mensagens de erro controladas e redirecionamentos HTTP desativados.
- Testes unitários para validação da configuração do provedor.
- Workflow de CI para executar testes e montar o APK de debug.

## Configuração de IA externa (opcional)

1. Abra **Configurações** no app.
2. Informe a URL base HTTPS, o identificador exato do modelo e a chave fornecida pelo seu provedor.
3. Salve a configuração. O campo da chave é mascarado e fica vazio depois de salvar.
4. Ative **Usar IA externa** somente depois de verificar os termos, a privacidade e os possíveis custos do serviço.

O provedor fica desativado por padrão. Sem ativação explícita, o Copy usa apenas os comandos locais limitados. Quando ativado, o app envia a mensagem atual e até 15 mensagens anteriores ao endpoint configurado. O endpoint precisa implementar o formato `POST /chat/completions` compatível com a API de chat da OpenAI. A disponibilidade, os limites e a cobrança dependem do provedor escolhido.

A configuração pode ser removida pela própria tela. Não coloque chaves no código, no GitHub, em logs ou em capturas de tela. A chave é protegida em repouso pelo Android Keystore, mas o aparelho e o provedor ainda precisam ser tratados como partes sensíveis do fluxo.

## Limitações atuais

- O runtime Rust original **ainda não está integrado** ao app Android; este cliente HTTP opcional é uma fronteira separada para inferência remota.
- Não há modelo local nem pesos de modelo incluídos no APK.
- O app ainda não executa ferramentas, automações ou comandos de sistema gerados pela IA.
- O histórico local é salvo em SQLite, mas a memória semântica/pessoal controlável ainda não foi implementada.
- O reconhecimento de fala depende do serviço disponível no Android; a preferência por offline não garante suporte offline.
- A tela nativa ainda precisa ser comparada com a referência exata do Figma do Copy.
- Calendário, notificações, arquivos, câmera e outros aplicativos ainda não estão integrados.
- Um build de CI bem-sucedido não equivale a teste no POCO X5 Pro ou a uma versão pronta para distribuição.

## Compilar e testar

Requisitos: JDK 17, Android SDK Platform 36 e Gradle 9.6.0.

```bash
gradle -p android testDebugUnitTest assembleDebug
```

O APK de debug é gerado em `android/app/build/outputs/apk/debug/app-debug.apk`. Nenhuma chave de API é necessária para compilar ou executar os comandos locais.
