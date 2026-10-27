# Copy — status de implementação

Atualizado em 2026-10-27.

## Implementado na branch `copy-foundation`

- Fundação Android nativa em Kotlin + Jetpack Compose, com histórico local em SQLite, comandos locais limitados, reconhecimento de fala via serviço Android e TextToSpeech.
- Camada opcional de chat remoto para endpoints HTTPS compatíveis com OpenAI Chat Completions; desativada por padrão.
- Chave do provedor criptografada com AES-GCM, com chave guardada no Android Keystore; URL e identificador do modelo configuráveis pela tela.
- O cliente remoto aplica timeouts, limita o tamanho da resposta, desativa redirecionamentos e evita registrar a chave.
- A memória pessoal passou de exemplos em sessão para itens gravados em SQLite, com adicionar/remover individualmente e apagar tudo mediante confirmação.
- O conteúdo das memórias é criptografado com AES-GCM e uma chave própria no Android Keystore. Há migração do schema anterior, que gravava texto sem criptografia.
- Memórias ficam apenas neste dispositivo; sincronização não está conectada. Elas ainda não são anexadas automaticamente ao prompt de IA.
- Testes unitários cobrem validação de URL e modelo. O workflow CI já passou para a camada de provedor antes das mudanças de memória; a validação final das mudanças de memória ainda está em execução.

## Validação pendente

- Confirmar o resultado final do workflow Android depois das alterações de memória e verificar que teste e APK de debug passaram juntos.
- Instalar e testar no POCO X5 Pro; o CI não substitui teste físico.
- Configurar e testar um endpoint real escolhido pelo usuário, sem colocar a chave no GitHub.
- Testar migração da base SQLite real, restauração após reiniciar e comportamento quando uma chave do Android Keystore não puder ser usada.
- Comparar a interface nativa com a referência exata do Figma; a correspondência visual ainda não foi confirmada.

## Ainda não implementado

- Integração Kotlin–Rust. O crate `xai-grok-shell` continua sendo um runtime de agente de terminal com ampla árvore de dependências, não uma biblioteca Android integrada.
- Inferência local e pesos de modelo incluídos no APK.
- Roteador híbrido automático entre modelo local e provedores remotos.
- Inclusão contextual das memórias nos prompts, com controle explícito e apresentação clara do que será enviado a um provedor externo.
- Ferramentas Android, automações, sincronização segura e auditoria das ações do assistente.
- Testes completos de acessibilidade, bateria, compatibilidade e uso contínuo em aparelho real.

## Próximas etapas técnicas

1. Confirmar CI de memória e corrigir eventuais falhas antes de ampliar a implementação.
2. Validar um endpoint remoto selecionado pelo usuário, com credenciais apenas no dispositivo.
3. Definir e testar um contrato versionado de runtime antes de conectar a UI ao núcleo Rust.
4. Avaliar separadamente JNI/UniFFI e o modo ACP/headless; não importar a TUI inteira sem validar dependências e tamanho.
5. Integrar o uso de memória ao assistente apenas depois de definir consentimento, exclusão, escopo e quais memórias podem sair do dispositivo.
6. Comparar a UI Android com o Figma aprovado e testar no aparelho real.

Compilação, planejamento ou código criado não significam que o produto completo esteja pronto para uso contínuo ou distribuição.
