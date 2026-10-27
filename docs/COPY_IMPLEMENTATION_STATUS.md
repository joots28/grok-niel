# Copy — status de implementação

Atualizado em 2026-10-27.

## Implementado na branch `copy-foundation`

- A fundação Android está em `android/`, com Kotlin e Jetpack Compose.
- O histórico de conversa é persistido em SQLite; os comandos locais atendem apenas ajuda, hora e data.
- Há entrada de fala pelo serviço disponível no Android e leitura de resposta por TextToSpeech.
- Foi adicionado um cliente opcional de chat para endpoints HTTPS compatíveis com OpenAI.
- A chave do provedor é cifrada com AES-GCM usando uma chave protegida pelo Android Keystore; ela não é armazenada em texto puro nas preferências.
- A IA externa fica desabilitada por padrão. Com a ativação explícita, a mensagem atual e até 15 mensagens anteriores podem ser enviadas ao endpoint configurado.
- O cliente aplica timeout, limita o corpo da resposta, desativa redirecionamentos e não registra a chave em logs.
- Foram adicionados testes de validação de URL/modelo e um workflow de CI para teste e criação do APK de debug.
- A interface React/TSX exportada do Figma está preservada em um backup local PDF/ZIP; a confirmação do PDF dentro do repositório depende do workflow de montagem.

## Validação pendente desta alteração

- A implementação do provedor acabou de ser enviada à branch. Os resultados do workflow de CI desses novos commits ainda devem ser consultados antes de declarar os testes aprovados.
- Não foi feita compilação Android local nesta sessão nem instalação física no POCO X5 Pro.
- O endpoint remoto real não foi testado porque não há uma configuração e uma chave fornecidas pelo usuário.
- Testar chamadas com autenticação inválida, limite de uso, timeouts e indisponibilidade do provedor.
- Testar a limpeza do segredo, restauração de estado e persistência após reiniciar o aplicativo.

## Ainda não implementado

- Integração Kotlin–Rust: o crate `xai-grok-shell` é um runtime de agente de terminal com dependências amplas; ele não pode ser considerado um motor Android integrado apenas por existir no repositório.
- Inferência local e pesos de modelo no aparelho.
- Roteador inteligente entre modelos local e remoto.
- Memória pessoal persistente e controlável separada do histórico.
- Cofre para outras credenciais, sincronização segura, ferramentas Android, automações e auditoria de ações.
- Correspondência visual verificada com o Figma específico do Copy.
- Testes completos de acessibilidade, bateria, compatibilidade e uso contínuo em aparelho real.

## Próximas etapas técnicas

1. Confirmar build e testes do provedor opcional pelo CI e corrigir falhas reais.
2. Validar a chamada com um endpoint compatível escolhido pelo usuário, sem incluir segredos no repositório.
3. Definir o contrato versionado da interface de runtime antes de acoplar o Android ao núcleo Rust.
4. Avaliar separadamente JNI/UniFFI e um processo ACP/headless; não importar o TUI inteiro sem validar dependências e tamanho.
5. Implementar memória pessoal com migrações, opt-in, edição, exportação e exclusão.
6. Comparar a interface Android com a referência Figma e testar no dispositivo real.

A compilação, a existência de código ou uma resposta de provedor não significam que o produto inteiro esteja pronto para uso contínuo ou distribuição.
