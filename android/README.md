# Copy Android

Aplicativo Android nativo em Kotlin + Jetpack Compose. A interface é separada do runtime Rust do Grok Build porque o runtime original foi criado para um agente de terminal; a compatibilidade móvel precisa ser implementada e testada separadamente.

## O que já existe

- Interface de conversa nativa com tema escuro.
- Histórico de chat persistido localmente em SQLite.
- Comandos offline limitados para ajuda, hora e data.
- Entrada de voz via serviço disponível no Android e leitura em voz alta via TextToSpeech.
- Camada de chat remota opcional, compatível com endpoints HTTPS de OpenAI Chat Completions.
- URL base e identificador do modelo configuráveis no app; provedor externo fica desativado por padrão.
- Chave de API criptografada com AES-GCM e chave protegida pelo Android Keystore.
- Mensagens persistidas localmente; chave da API nunca é incluída no histórico ou nos logs.
- Memória pessoal persistente em SQLite, com itens adicionáveis/removíveis e opção de apagar tudo com confirmação.
- Conteúdo da memória cifrado em repouso por AES-GCM com chave separada no Android Keystore. O banco usa uma migração explícita do esquema anterior.
- Testes unitários para validação da configuração do provedor e workflow de CI para testes/APK de debug.

## Configurar IA remota opcional

1. Abra **Configurações**.
2. Informe a URL base HTTPS, o identificador exato do modelo e a chave fornecida pelo provedor.
3. Salve a configuração.
4. Ative **Usar IA externa** somente após conferir privacidade, condições e possíveis custos do serviço.

Quando a IA externa é ativada, a mensagem atual e até 15 mensagens anteriores podem ser enviadas ao endpoint configurado. O provedor precisa implementar o formato `POST /chat/completions` compatível com OpenAI Chat Completions. Disponibilidade, limites e cobrança dependem do provedor escolhido. O app não faz fallback silencioso para outro serviço caso a solicitação falhe.

A chave fica mascarada no campo e o valor em memória da interface é limpo após salvar. O campo vazio permite conservar a chave que já está armazenada. A configuração pode ser removida na própria tela. Nunca coloque chaves no código, no GitHub, em logs ou em capturas de tela.

## Memória pessoal

As memórias são escritas apenas quando o usuário as registra. Permanecem no banco local, cifradas com uma chave mantida pelo Android Keystore, sem sincronização automática. É possível remover um item ou apagar todos com confirmação.

As memórias **não são enviadas por padrão**. Para incluí-las nas requisições remotas, configure e ative um provedor e habilite separadamente **Incluir memórias em pedidos remotos**; o app pede confirmação de que esses dados sairão do aparelho. Você pode desligar essa opção a qualquer momento. Desativar a área de memória esconde os itens e impede seu uso no contexto remoto, mas não apaga o conteúdo salvo. Não há backend de sincronização conectado.

## Limitações importantes

- O runtime Rust original **ainda não está conectado** à interface Android; o cliente HTTP opcional é uma camada separada para inferência remota.
- Não há modelo local nem pesos de modelo incluídos no APK.
- A API de provedor pode ter custo; essa integração não promete uso gratuito.
- O app ainda não executa ferramentas, automações ou comandos de sistema gerados pela IA.
- As memórias não são usadas automaticamente pelo modelo.
- Reconhecimento de fala depende do serviço disponível no Android; a preferência por offline não garante reconhecimento sem rede.
- A interface nativa ainda precisa ser comparada com a referência exata do Figma do Copy.
- Calendário, notificações, arquivos, câmera e outros aplicativos não estão integrados.
- Um build de CI bem-sucedido não substitui teste no POCO X5 Pro e não equivale a uma versão pronta para distribuição.

## Compilar e testar

Requisitos: JDK 17, Android SDK Platform 36 e Gradle 9.6.0.

```bash
gradle -p android testDebugUnitTest assembleDebug
```

O APK de debug é gerado em `android/app/build/outputs/apk/debug/app-debug.apk`. Nenhuma chave de API é necessária para compilar ou usar os comandos locais.
