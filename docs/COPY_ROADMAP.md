# Copy — Plano de evolução

Copy é o assistente pessoal Android-first, com funcionamento local e conectado, memória controlável e ferramentas autorizadas. Este plano separa o que já existe do que ainda precisa de implementação ou validação.

## Estado atual — 27 de outubro de 2026

- Repositório: `joots28/grok-niel`, branch `copy-foundation`.
- Android em Kotlin + Jetpack Compose; histórico de conversa SQLite cifrado em repouso; comandos offline limitados, reconhecimento de fala via serviço Android e TextToSpeech.
- Migração da base para versão 5 cifra o conteúdo do histórico anterior e preserva IDs, papéis, datas e ordem. Teste de migração com dados reais em aparelho ainda pendente.
- Cliente remoto opcional compatível com OpenAI Chat Completions, desativado por padrão; chave criptografada com Android Keystore.
- Memórias pessoais persistidas e cifradas, com inclusão/remover/excluir tudo e `secure_delete`. O envio remoto de memórias exige consentimento separado, com limites de 12 itens / 4.000 caracteres.
- Alterar a configuração do provedor, desligar a IA externa, desativar a memória ou entrar no modo local revoga o consentimento de envio de memórias.
- Estilos Conciso/Equilibrado/Detalhado são adicionados ao prompt remoto; comandos locais seguem determinísticos.
- O CI passou no commit `8b36deee` (run [40432399485](https://github.com/joots28/grok-niel/actions/runs/40432399485)).
- O runtime Rust continua sendo um agente de terminal. Não há ponte Kotlin–Rust nem modelo local/pesos no APK.
- A interface ainda não foi comparada visualmente, pixel a pixel, ao Figma específico do Copy.

## Princípios

- Reutilizar o runtime Rust somente depois de mapear contratos, dependências, permissões e licenças.
- Nunca colocar chaves de API, tokens ou credenciais no repositório ou nos logs.
- Exigir autorização explícita para envio externo e operações destrutivas.
- Não presumir que provedores remotos são gratuitos.
- Fazer mudanças pequenas, revisáveis e com testes direcionados.
- Não declarar fidelidade ao Figma antes de comparar com a referência aprovada.

## Fases

### Fase 0 — Diagnóstico

Inspecionar o runtime Rust, toolchain e dependências; distinguir problemas de ambiente de erros de código.

**Estado:** auditoria documental inicial feita; a suíte completa do runtime Rust não foi executada.

### Fase 1 — Fundação Android

Compose, histórico local e CI de build/teste.

**Estado:** implementada na branch `copy-foundation`; CI passou no commit `8b36deee`.

### Fase 2 — Design fiel e contratos

Comparar a UI com o Figma de referência e definir contrato versionado para o runtime móvel.

**Portão:** comparação visual documentada e contrato testado.

### Fase 3 — Núcleo Rust e fluxo vertical

Comparar JNI/UniFFI com processo ACP/headless; implementar uma chamada de ponta a ponta e validar cancelamento, timeout, erros e isolamento.

**Portão:** runtime comprovadamente integrado à UI, testes reproduzíveis e nenhum segredo em logs.

### Fase 4 — Memória e privacidade

- Memória pessoal local cifrada, migrada e apagável: implementada; CI passou.
- Histórico de chat cifrado com migração explícita: implementado; CI passou.
- Consentimento separado para enviar memórias a um provedor: implementado; CI passou.
- Exportação/restauração segura e validação de migração no aparelho: pendentes.

**Portão:** persistência, cifra, migração, exclusão e restauração testadas em dispositivo.

### Fase 5 — IA híbrida, voz e ferramentas

- Cliente remoto opcional implementado; ele não substitui o runtime Rust nem um modelo local.
- Validar um endpoint remoto escolhido pelo usuário, sem credenciais no GitHub.
- Avaliar inferência local; não empacotar pesos antes de escolher modelo, licença e orçamento de armazenamento.
- Integrar ferramentas Android gradualmente, com permissões mínimas.

**Portão:** provedor verificado, erros/limites tratados e comportamento offline testado.

### Fase 6 — Robustez e release

Testes de acessibilidade, bateria, compatibilidade e uso contínuo; instalar no POCO X5 Pro; preparar APK e plano de reversão. Publicação externa requer autorização específica.

**Portão:** checklist de qualidade aprovado.

## Custos

A base local não exige serviço próprio obrigatório. Provedores remotos podem ter custos e limites; compatibilidade de API não garante uso gratuito nem significa que existe um modelo local.

## Próxima ação

Testar o APK no aparelho, validar um endpoint remoto opcional e então definir a fronteira de integração Rust–Android ou um caminho de inferência local.
