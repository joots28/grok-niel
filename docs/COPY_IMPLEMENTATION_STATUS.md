# Copy — status de implementação

Atualizado em 2026-10-27.

## Implementado na branch `copy-foundation`

- Aplicação Android nativa em Kotlin + Jetpack Compose, histórico local em SQLite, comandos offline limitados, reconhecimento de fala via serviço Android e TextToSpeech.
- Camada opcional de chat remoto para endpoints HTTPS compatíveis com OpenAI Chat Completions; desativada por padrão.
- A URL, o identificador do modelo e a chave do provedor são configuráveis no app. A chave é criptografada com AES-GCM e protegida pelo Android Keystore.
- O cliente remoto aplica timeouts, limita mensagens e respostas, desativa redirecionamentos e evita registrar a chave em logs.
- Memória pessoal persistente em SQLite, com itens adicionáveis/removíveis e opção de apagar tudo mediante confirmação.
- Conteúdo das memórias criptografado em repouso com uma chave separada no Android Keystore; a base foi migrada do esquema anterior e o SQLite está configurado para secure delete.
- Nenhuma sincronização é feita. As memórias ficam no aparelho por padrão e só são incluídas no contexto remoto após ativação do provedor e confirmação de consentimento separada. O usuário pode desligar essa permissão.
- Os pedidos incluem no máximo 12 memórias e 4.000 caracteres de contexto. Memórias não acionam ferramentas ou ações no dispositivo.
- Testes unitários de validação de URL/modelo e montagem do APK de debug passaram no workflow Android do commit `69684bdf` (27/10/2026).

## Validação ainda pendente

- Instalar e testar no POCO X5 Pro; build verde não substitui teste físico.
- Configurar e testar um endpoint real escolhido pelo usuário, sem enviar a chave ao GitHub.
- Testar migração SQLite com dados existentes e comportamento em aparelho quando uma chave do Android Keystore não puder ser usada.
- Comparar a interface Android com a referência exata do Figma; a correspondência visual ainda não foi confirmada.
- Testes completos de acessibilidade, bateria, compatibilidade, exportação/restauração de memórias e uso contínuo.

## Ainda não implementado

- Integração Kotlin–Rust. O crate `xai-grok-shell` continua sendo um runtime de agente de terminal com uma árvore ampla de dependências, não uma biblioteca Android integrada.
- Inferência local e pesos de modelo incluídos no APK.
- Roteador híbrido automático entre modelo local e provedores remotos.
- Ferramentas Android, automações, sincronização segura, auditoria de ações e execução de comandos gerados pelo modelo.

## Próximas etapas técnicas

1. Testar o APK em aparelho real e testar um provedor configurado pelo usuário.
2. Definir um contrato versionado de runtime antes de conectar a interface ao núcleo Rust.
3. Avaliar JNI/UniFFI e ACP/headless separadamente; não importar a TUI inteira sem validar dependências e tamanho.
4. Desenvolver uma opção de inferência local sem pressupor custo zero nem empacotar pesos sem escolha explícita de modelo.
5. Só ampliar o envio de memória para fora do aparelho com consentimento explícito, prévia de escopo e exclusão fácil.
6. Comparar a UI Android com o Figma aprovado.

Compilação, planejamento ou código criado não significam que o produto completo esteja pronto para uso contínuo ou distribuição.
