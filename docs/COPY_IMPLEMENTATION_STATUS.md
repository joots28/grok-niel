# Copy — status de implementação

Atualizado em 2026-10-27. Branch de trabalho: `copy-foundation`.

## Implementado

- Aplicativo Android em Kotlin + Jetpack Compose, histórico local em SQLite, comandos offline limitados, entrada de voz via serviço Android e TextToSpeech.
- Cliente remoto opcional para endpoints HTTPS compatíveis com OpenAI Chat Completions; desativado por padrão.
- URL, modelo e chave do provedor configuráveis na tela. A chave é criptografada com AES-GCM e protegida pelo Android Keystore; redirecionamentos são desativados, há timeouts e limites de payload.
- Histórico de conversa cifrado em repouso com uma chave própria do Android Keystore. O banco foi elevado para versão 5 com migração explícita que preserva IDs, papéis, horários e ordem ao cifrar o conteúdo anterior.
- Memória pessoal cifrada em repouso com chave separada do Android Keystore, migração do schema anterior, inclusão, remoção individual, exclusão total confirmada e SQLite `secure_delete`.
- Memórias não são enviadas por padrão. A inclusão no contexto remoto exige consentimento separado e fica limitada a 12 itens / 4.000 caracteres.
- O estilo de resposta selecionado é aplicado aos prompts do provedor remoto. Os comandos locais permanecem determinísticos.
- CI passou no commit `8b36deee`, run [40432399485](https://github.com/joots28/grok-niel/actions/runs/40432399485): testes unitários e APK de debug construído.

## Validação pendente

- Instalação e teste físico no POCO X5 Pro.
- Teste de endpoint remoto real configurado pelo usuário; nenhum provedor nem chave foram fornecidos nesta sessão.
- Exercitar em aparelho real a migração de bancos com conversas/memórias antigas e o comportamento caso a chave do Android Keystore fique indisponível.
- Comparação visual com o Figma do Copy; fidelidade pixel a pixel não confirmada.
- Testes completos de acessibilidade, bateria, compatibilidade, uso contínuo e exportação/restauração.

## Ainda não implementado

- Integração Kotlin–Rust. O crate `xai-grok-shell` é parte de um runtime de agente de terminal com árvore ampla de dependências.
- Inferência local e pesos de modelo incluídos no APK.
- Roteador automático entre modelo local e provedores remotos.
- Ferramentas Android, automações, sincronização segura e auditoria de ações.

## Próximas etapas

1. Validar a instalação e a migração em aparelho real.
2. Configurar e testar um endpoint remoto escolhido pelo usuário, sem colocar segredos no GitHub.
3. Mapear o runtime Rust e definir um contrato móvel; comparar JNI/UniFFI com ACP/headless antes de integrar.
4. Avaliar inferência local sem pressupor custo zero nem empacotar pesos sem escolher modelo, licença e orçamento de armazenamento.
5. Validar a interface contra o Figma aprovado.

Build verde não significa que o produto completo esteja pronto para uso contínuo ou distribuição.
