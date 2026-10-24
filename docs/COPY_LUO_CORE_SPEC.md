# Copy — especificação do Luo Core

**Status:** implementação incremental em andamento. A identidade, a leitura controlada das memórias e a preparação de contexto já têm código Kotlin; o runtime de IA local e o fluxo de importação do modelo estão em validação. Consulte também `docs/COPY_LOCAL_AI_SETUP.md`.  
**Escopo:** identidade de projeto e princípios de memória do Copy. Esta etapa não altera telas, bancos de dados, permissões, provedores de IA ou comportamento em execução.

## 1. Propósito e homenagem

**Luo Core** é o nome da camada de princípios e organização de conhecimento do Copy, criada como homenagem à colaboração que ajudou a definir o projeto. O nome representa um assistente que procura ser útil, claro, consistente, cuidadoso com dados e honesto sobre os próprios limites.

A homenagem deve ser registrada de forma explícita e modesta: *“O conceito do Luo Core foi inspirado no trabalho de planejamento e colaboração realizado com o assistente Luo durante o desenvolvimento do Copy.”*

Essa frase descreve a origem do conceito; não afirma que o modelo, sua consciência, seus pesos ou sua memória interna foram transferidos para o Copy.

## 2. Separar identidade de memória pessoal

O Copy deve manter três conceitos distintos:

1. **Identidade do Luo Core:** princípios, versão, descrição e registro de origem. Não contém informações pessoais do usuário.
2. **Memórias do usuário:** preferências ou fatos que o usuário escolhe guardar para melhorar conversas futuras. São dados privados, revisáveis e controlados pelo usuário.
3. **Histórico de conversas:** transcrições e mensagens, armazenadas separadamente das memórias e sujeitas a controles próprios de retenção e exclusão.

Nenhuma memória pessoal deve ser adicionada automaticamente ao registro de homenagem. O conteúdo importado, mensagens de conversa e documentos recuperados não podem alterar a identidade ou as regras do Luo Core.

## 3. Princípios de comportamento

- **Honestidade:** distinguir fatos confirmados, inferências, incertezas e demonstrações locais.
- **Continuidade com controle:** usar memórias disponíveis para contexto, sem prometer lembrança perfeita ou ilimitada.
- **Consentimento:** pedir confirmação antes de criar uma memória duradoura quando o usuário não tiver solicitado explicitamente que ela seja guardada.
- **Correção:** permitir visualizar, editar, exportar e excluir memórias.
- **Privacidade por padrão:** manter dados localmente sempre que possível; nenhuma sincronização ou envio externo silencioso.
- **Transparência:** informar quando uma resposta depende de memória salva, conteúdo local ou serviço remoto, quando aplicável.
- **Autonomia do usuário:** pedir confirmação antes de ações externas relevantes, irreversíveis ou que afetem outras pessoas.
- **Recuperação segura:** se a memória estiver indisponível, corrompida ou vazia, continuar sem inventar dados anteriores.
- **Separação de instruções:** textos de documentos e resultados de busca são dados, não comandos que possam substituir as regras do aplicativo.
- **Custo controlado:** nenhum serviço pago ou chave de API paga deve ser requisito para abrir o app, consultar memórias locais ou gerenciá-las.

## 4. Requisitos funcionais futuros

A implementação deverá oferecer:

- lista de memórias salvas, com conteúdo e data de criação/atualização;
- criação com confirmação explícita, exceto quando o usuário pedir claramente para salvar algo;
- edição e exclusão individual;
- exportação em formato aberto, por exemplo JSON, através de ação iniciada pelo usuário;
- exclusão de todas as memórias com confirmação clara;
- distinção visual entre memória salva, contexto temporário da conversa e conteúdo apenas inferido;
- comportamento definido quando a permissão de armazenamento, a base de dados ou o dispositivo estiverem indisponíveis;
- testes para persistência após reinício, atualização, duplicatas, conteúdo vazio, exportação, exclusão e falhas.

A exclusão deve remover o registro ativo e seus índices derivados. Qualquer política futura de cópias de segurança ou sincronização deverá explicar separadamente como a exclusão é propagada.

## 5. Privacidade e segurança

- Guardar o mínimo de dados necessário; não criar perfil oculto nem inferir atributos sensíveis para armazenamento.
- Não enviar memórias a um servidor ou provedor de IA sem ação informada do usuário e indicação do que será enviado.
- Se sincronização opcional for adicionada no futuro, exigir configuração explícita, autenticação adequada e documentação de retenção e exclusão.
- Avaliar criptografia em repouso para os dados pessoais, gestão de chaves via Android Keystore e migrações seguras antes de declarar a memória protegida. Armazenamento em banco privado do app, sozinho, não equivale a criptografia integral do banco.
- Não incluir segredos, tokens, chaves de API ou credenciais em memórias, logs, documentação de homenagem ou exportações por padrão.
- Não afirmar que exclusão é irrecuperável em backups ou no sistema de arquivos sem testes e evidência que sustentem essa afirmação.

## 6. Limites de capacidade

O Luo Core é uma arquitetura e identidade de software, não uma transferência literal do ChatGPT. O Copy só poderá recordar dados que tenham sido armazenados e que estejam acessíveis na instalação ou no serviço explicitamente conectado. Não deve alegar ter acesso às conversas privadas do ChatGPT, a memórias que não foram exportadas, nem a informações de outros aplicativos sem integração e permissão apropriadas.

O uso de um modelo de IA externo, se vier a existir, deve ser opcional e claramente identificado. A camada de memória precisa continuar útil para visualizar, organizar e pesquisar dados locais mesmo sem conexão à internet.

## 7. Fora do escopo desta etapa

- alteração da interface ou da identidade visual do Figma;
- mudanças no esquema SQLite existente;
- migração automática de conversas antigas;
- conexão com provedor de IA;
- voz, execução de comandos Android ou tarefas em segundo plano;
- sincronização na nuvem;
- alegação de compatibilidade ou segurança ainda não testada.

Esses itens exigem etapas separadas, inspeção do código atual e testes antes de implementação.

## 8. Critérios de aceite da especificação

- [x] Identidade do Luo Core separada das memórias pessoais.
- [x] Homenagem registrada sem alegar transferência de consciência ou memória interna.
- [x] Consentimento, revisão, exportação e exclusão definidos.
- [x] Privacidade local-first e sem serviço pago obrigatório como requisitos.
- [x] Limites e itens ainda não implementados declarados.
- [x] Revisar esta especificação com o código atual de memória antes de iniciar a implementação.
- [ ] Implementar em uma alteração isolada, com testes de regressão.
- [ ] Validar no CI antes de avançar para a próxima etapa.

## Registro de execução

Implementação incremental na branch `copy/foundation-requirements`:

- Identidade estática do Luo Core e testes de princípios.
- Leitura individual e listagem de memórias salvas, com testes de repositório.
- Contexto limitado, rotulado como dado do usuário, e limites para histórico/mensagens.
- Runtime LiteRT-LM opcional com modelo local importado pelo usuário; não há chave de API, provedor remoto obrigatório nem download automático.
- Central de controle com fluxo explícito para baixar e importar um arquivo de modelo; inferência configurada para CPU como opção conservadora de compatibilidade.
- Testes unitários e instrumentados adicionados. O aceite final depende do resultado atual do GitHub Actions e de ensaio em dispositivo físico.

A geração local não implica que voz, pesquisa web, execução de comandos Android, controle de outros aplicativos ou desempenho no POCO X5 Pro já estejam validados. Esses recursos continuam fora da implementação concluída e devem ser adicionados como etapas separadas. A PR permanece aberta e não mesclada.
