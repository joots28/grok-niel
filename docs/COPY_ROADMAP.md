# Copy — Plano de evolução

Copy é o assistente pessoal Android-first, com funcionamento local e conectado, memória controlável e ferramentas autorizadas. Este plano separa o que já existe do que ainda precisa de integração e validação.

## Estado atual — 27 de outubro de 2026

- Repositório: `joots28/grok-niel`, branch `copy-foundation`.
- Android em Kotlin + Jetpack Compose: conversa persistente em SQLite, comandos offline limitados, reconhecimento de fala via serviço Android e TextToSpeech.
- Cliente remoto opcional para APIs HTTPS compatíveis com OpenAI Chat Completions; desativado por padrão. A chave é protegida pelo Android Keystore.
- Memórias pessoais salvas em SQLite e criptografadas; incluem migração do esquema, remoção individual, excluir tudo com confirmação e secure delete.
- Memórias ficam no aparelho por padrão. Enviá-las a um provedor requer ativar o provedor e uma confirmação de consentimento separada; o usuário pode desligar o envio. Limite contextual de 12 memórias / 4.000 caracteres.
- O CI de Android passou no commit `69684bdf`, com testes unitários e APK debug.
- O núcleo Rust continua sendo um agente de terminal: não há ponte Kotlin–Rust nem modelo local/pesos incluídos no APK.
- A UI ainda não foi comparada visualmente, pixel a pixel, ao Figma específico do Copy.

## Princípios

- Reutilizar o runtime Rust somente após mapear contratos, dependências, permissões e licenças.
- Nunca colocar chaves de API, tokens ou credenciais no repositório ou logs.
- Exigir autorização explícita para envio externo e operações destrutivas.
- Não presumir que provedores remotos são gratuitos.
- Fazer mudanças pequenas, revisáveis e com testes direcionados.
- Não declarar fidelidade ao Figma sem comparação com a referência aprovada.

## Fases

### Fase 0 — Diagnóstico
Inspecionar o runtime Rust, toolchain e dependências; distinguir problemas de ambiente de erros de código.

**Estado:** auditoria documental inicial feita; a suíte completa do runtime Rust não foi executada.

### Fase 1 — Fundação Android
Compose, histórico local e CI de build/teste.

**Estado:** implementada na branch `copy-foundation`; CI de Android passou no commit `69684bdf`.

### Fase 2 — Design fiel e contratos
Comparar a UI com o Figma de referência e definir contrato versionado para o runtime móvel.

**Portão:** comparação visual documentada e contrato testado.

### Fase 3 — Núcleo Rust e fluxo vertical
Comparar JNI/UniFFI com processo ACP/headless; fazer uma chamada de ponta a ponta, com cancelamento, timeout, erros e isolamento.

**Portão:** runtime conectado e comprovado na UI, testes reproduzíveis e nenhum segredo em logs.

### Fase 4 — Memória e privacidade
- Memória local persistente, cifrada, migrada e apagável: implementada; CI passou.
- Consentimento separado para envio remoto: implementado; CI passou.
- Exportação/restauração, política de retenção configurável e testes de migração no aparelho: pendentes.

**Portão:** persistência, cifra, migração, exclusão e restauração validadas em dispositivo.

### Fase 5 — IA híbrida, voz e ferramentas
- Cliente remoto opcional está implementado, mas não substitui o runtime Rust ou inferência local.
- Validar um endpoint escolhido pelo usuário, sem segredo no GitHub.
- Pesquisar e testar inferência local; não empacotar pesos sem escolher modelo, licença e orçamento de armazenamento.
- Integrar ferramentas Android gradualmente, com permissões mínimas.

**Portão:** provedor verificado, erros e limites tratados, uso offline testado.

### Fase 6 — Robustez e release
Testes de acessibilidade, bateria, compatibilidade e uso contínuo; instalar no POCO X5 Pro; preparar APK e plano de reversão. Publicação externa requer autorização.

## Custos

A base local não precisa de serviço próprio obrigatório. Recursos remotos podem ter custos e limites. Compatibilidade de API não garante uso gratuito nem significa que existe um modelo local.

## Próxima ação

Testar o APK em aparelho real e um endpoint remoto opcional; em seguida, definir a fronteira da integração Rust–Android ou a implementação de inferência local.
