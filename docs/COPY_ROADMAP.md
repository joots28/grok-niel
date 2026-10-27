# Copy — Plano de evolução

Copy é o assistente pessoal Android-first, com funcionamento local e conectado, memória controlável e ferramentas autorizadas. Este documento diferencia capacidades existentes de funcionalidades que ainda precisam ser implementadas ou validadas.

## Estado atual — 27 de outubro de 2026

- Repositório: `joots28/grok-niel`; branch: `copy-foundation`.
- A aplicação Android em Kotlin + Jetpack Compose tem histórico de chat SQLite, comandos offline limitados, reconhecimento de fala do Android e TextToSpeech.
- Há uma camada de chat remoto opcional, compatível com endpoints HTTPS OpenAI Chat Completions, desativada por padrão. A chave é cifrada com AES-GCM e protegida pelo Android Keystore.
- A memória pessoal agora é persistida em SQLite, cifrada em repouso com chave separada do Android Keystore, e pode ser adicionada/removida/excluída na UI. Não existe sincronização; as memórias não são enviadas automaticamente ao provedor.
- Os commits de provedor remoto passaram no CI. A validação do build/tests depois das mudanças recentes de memória ainda está pendente.
- O núcleo Rust original segue como agente de terminal. Nenhum runtime Rust foi conectado ao Android, nenhum modelo local/peso foi incorporado ao APK.
- A interface ainda precisa de comparação visual verificável com o Figma específico do Copy.

## Princípios

- Reaproveitar arquitetura Rust apenas após mapear contratos, dependências e permissões.
- Criar identidade e experiência próprias para o Copy; não apresentar o projeto como produto oficial da xAI.
- Preservar `LICENSE`, `THIRD-PARTY-NOTICES` e os avisos de licenças de terceiros.
- Nunca colocar chaves de API, tokens ou credenciais no repositório.
- Exigir autorização explícita para envio externo e para operações destrutivas; manter logs úteis sem registrar segredos.
- Fazer mudanças pequenas, revisáveis e com testes direcionados.
- Não declarar a interface fiel ao Figma antes de compará-la com a referência aprovada.

## Fases

### Fase 0 — Diagnóstico reproduzível
1. Conferir toolchain, dependências de build e instruções.
2. Executar formatação, verificações e testes de crates específicos.
3. Registrar erros de ambiente separadamente de erros de código.
4. Mapear autenticação, seleção de modelo, configuração e limites de permissões.

**Estado:** auditoria documental inicial feita; build/testes completos do runtime Rust não foram executados nesta etapa.

### Fase 1 — Fundação Android
1. Configurar aplicação Kotlin/Compose e tela de conversa.
2. Adicionar persistência local e testes iniciais.
3. Configurar CI para testes e APK de debug.

**Estado:** fundação existe na branch `copy-foundation`; builds Android anteriores passaram. Validar novamente depois da memória cifrada.

### Fase 2 — Design fiel e contratos
1. Identificar a referência exata do Figma do Copy.
2. Comparar e ajustar a UI sem redesenho arbitrário.
3. Definir contrato versionado, erros, timeout e cancelamento para o runtime Android.

**Portão:** comparação visual documentada e contrato testado.

### Fase 3 — Núcleo Rust e fluxo vertical
1. Inspecionar pontos de entrada do runtime sem remover fluxos existentes.
2. Comparar JNI/UniFFI com um processo ACP/headless separado.
3. Implementar uma chamada de ponta a ponta e validar cancelamento, timeout, erros e isolamento.

**Portão:** build e testes reproduzíveis; nenhuma credencial nos logs; runtime integrado comprovadamente na UI.

### Fase 4 — Memória e segurança
1. Persistir preferências explícitas em SQLite, com migrações e ações de adicionar, remover e excluir tudo. **Inicial já implementado.**
2. Cifrar as memórias com Android Keystore e migrar dados antigos sem deixar cópia em texto claro. **Implementação adicionada; CI e teste de aparelho pendentes.**
3. Adicionar exportação, recuperação, política de retenção e preferências persistentes.
4. Só incluir memória nos prompts após definir escopo, consentimento, prévia do envio e exclusão.

**Portão:** persistência, migração, cifra, exportação e exclusão validadas em testes.

### Fase 5 — IA híbrida, voz e ferramentas
1. A camada opcional de chat HTTP é inicial; não substitui a integração Kotlin–Rust nem um modelo local.
2. Validar um endpoint remoto escolhido pelo usuário; não assumir custo zero permanente.
3. Avaliar inferência local separadamente, sem empacotar pesos sem escolha explícita de modelo e orçamento de armazenamento.
4. Integrar voz, ferramentas Android e roteamento gradual, por permissões mínimas.

**Portão:** CI verde, provedor verificado, falhas de rede e limites tratados, comportamento offline demonstrado.

### Fase 6 — Robustez e release controlado
1. Regressão, acessibilidade, bateria, uso de memória, atualização e restauração.
2. Instalar e testar no POCO X5 Pro e validar outras dimensões/tamanhos de tela.
3. Preparar APK, instruções, versão e estratégia de reversão.

**Portão:** checklist de qualidade aprovado. Publicação externa exige autorização específica.

## Custos e dependências

A base local não exige serviço próprio obrigatório. Provedores remotos, sincronização e outros recursos conectados são opcionais, com custos e limites dependentes do serviço escolhido. Um cliente compatível com uma API não torna o provedor gratuito nem significa que existe inferência local.

## Próxima ação

Confirmar o CI das mudanças de memória cifrada; depois, testar um provedor escolhido pelo usuário sem expor chaves e definir a fronteira segura de integração Kotlin–Rust.
