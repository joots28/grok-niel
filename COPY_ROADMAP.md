# Copy — Plano de evolução

Copy é o assistente pessoal construído a partir da base de código aberta importada em `joots28/grok-niel`. Este documento separa capacidades existentes na base de funcionalidades que ainda precisam ser implementadas ou validadas.

## Princípios

- Reaproveitar a arquitetura Rust existente antes de reescrever componentes.
- Criar identidade, configuração e experiência próprias para o Copy; não apresentar o projeto como produto oficial da xAI.
- Preservar `LICENSE`, `THIRD-PARTY-NOTICES` e os avisos de licenças de dependências.
- Nunca colocar chaves de API, tokens ou credenciais no repositório.
- Exigir autorização explícita para operações externas ou destrutivas; manter logs úteis sem registrar segredos.
- Fazer mudanças pequenas, revisar os arquivos envolvidos e executar verificações direcionadas antes de integrar.

## Estado inicial verificado

- Repositório: `joots28/grok-niel`.
- Base: workspace Rust com edição 2024 e múltiplos crates.
- Binário de entrada atual: `xai-grok-pager-bin`; o README da base descreve um agente de programação de terminal (TUI/CLI).
- Runtime do agente: crate `xai-grok-shell`.
- Configuração: crate `xai-grok-config`.
- A compilação e os testes ainda precisam ser executados neste ambiente; a leitura do código no GitHub não comprova que o projeto compila.

## Fases propostas

### Fase 1 — Diagnóstico reproduzível
1. Conferir toolchain Rust, dependências de build e instruções do repositório.
2. Executar `cargo fmt --all -- --check` e verificações/testes de crates específicos.
3. Registrar erros de ambiente separadamente de erros de código.
4. Mapear autenticação, seleção de modelo, configuração e limites de permissões.

### Fase 2 — Identidade e configuração do Copy
1. Introduzir configuração própria sem quebrar a compatibilidade com os arquivos atuais.
2. Definir nome, mensagens de inicialização e documentação do Copy.
3. Documentar provedores/modelos suportados, custos e requisitos de credenciais.
4. Manter qualquer migração reversível e sem substituir o histórico importado.

### Fase 3 — Camada de modelos
1. Isolar a seleção de provedor/modelo da lógica de ferramentas quando a arquitetura atual permitir.
2. Suportar endpoints compatíveis apenas após verificar a implementação existente e a compatibilidade real.
3. Validar mensagens de erro, timeouts, limites e ausência de credenciais.
4. Não prometer uso gratuito: isso depende do provedor e de seus limites/preços.

### Fase 4 — Assistente pessoal
1. Melhorar memória opt-in com controles para consultar, exportar e apagar dados.
2. Adicionar planejamento de tarefas e resumos de progresso.
3. Permitir ferramentas por permissões explícitas, com confirmação para comandos sensíveis.
4. Adicionar testes para falhas, cancelamento e proteção de segredos.

### Fase 5 — Interface e integrações
1. Avaliar API e interface web/mobile sem acoplar a interface ao runtime.
2. Definir autenticação, isolamento de sessões e armazenamento antes de hospedar publicamente.
3. Validar limites operacionais e custos antes de qualquer deploy.

## Critérios de conclusão de cada etapa

- Diff pequeno e revisável.
- Testes executados com resultados registrados; testes não executados devem ser declarados.
- Nenhum segredo adicionado aos commits.
- Nenhuma publicação em produção sem autorização separada.
- Licenças e avisos de terceiros preservados.

## Próxima ação

Concluir o diagnóstico da configuração e do runtime do agente antes de alterar o caminho de execução principal. Não tratar este roadmap como prova de que as fases foram implementadas.
