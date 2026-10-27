# Copy — status de implementação

Atualizado em 2026-10-27.

## Implementado na branch copy-foundation

- Aplicativo Android em Kotlin + Jetpack Compose; histórico de conversas salvo em SQLite; comandos offline limitados, reconhecimento de fala pelo serviço Android e TextToSpeech.
- Cliente remoto opcional compatível com endpoints HTTPS OpenAI Chat Completions. Fica desativado por padrão. URL, modelo e chave são configuráveis no app.
- Chave da API criptografada em AES-GCM com chave protegida pelo Android Keystore; redirecionamentos HTTP desativados, timeouts e limites de payload implementados.
- Memória pessoal salva no SQLite, criptografada em repouso com chave própria do Android Keystore, com migração do schema anterior, inclusão, remoção individual e exclusão total confirmada. SQLite usa secure_delete.
- Memórias não são enviadas por padrão. Após configurar e ativar um provedor, a inclusão da memória em pedidos remotos exige confirmação explícita, pode ser desligada e tem limite de 12 itens / 4.000 caracteres.
- O CI de Android passou no commit 69684bdf: testes unitários e build de APK debug concluídos em 27/10/2026.

## Validação pendente

- Instalação e teste físico no POCO X5 Pro.
- Teste de um endpoint real configurado pelo usuário; nenhum provedor ou chave foram fornecidos nesta sessão.
- Teste em aparelho da migração SQLite, restauração e comportamento se a chave do Keystore ficar indisponível.
- Comparação visual com o Figma do Copy; fidelidade pixel a pixel ainda não confirmada.
- Testes amplos de acessibilidade, bateria, compatibilidade e uso contínuo.

## Não implementado

- Integração Kotlin–Rust. O crate xai-grok-shell permanece um runtime para agente de terminal com dependências amplas.
- Inferência local ou pesos de modelo incluídos no APK.
- Roteador automático entre modelo local e provedor remoto.
- Ferramentas Android, automações e sincronização segura.

## Próximas etapas

1. Testar o APK no aparelho e validar um provedor opcional, sem pôr segredos no GitHub.
2. Definir contrato versionado do runtime e comparar JNI/UniFFI com ACP/headless antes de integrar o núcleo Rust.
3. Investigar inferência local sem pressupor custo zero ou empacotar pesos sem escolher modelo e medir o armazenamento.
4. Continuar a validação do design e dos controles de privacidade.

Build verde não significa que o produto completo esteja pronto para distribuição.
