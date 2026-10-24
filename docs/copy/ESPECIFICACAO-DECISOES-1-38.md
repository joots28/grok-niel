# Copy — Especificação consolidada de decisões (Questionários 1–38)

> **Status:** planejamento consolidado; decisões registradas até o Questionário 38 em 24/10/2026. Este documento não significa que todos os recursos já estejam implementados.
>
> **Repositório:** `joots28/grok-niel`  
> **Branch de trabalho:** `copy/foundation-requirements`  
> **Regra de segurança do fluxo:** não mesclar PR, publicar em produção ou lançar versão estável sem autorização separada.

## Objetivo

Construir o Copy, assistente pessoal Android com identidade **Luo**, inspirado na experiência de um Mini-Jarvis: conversação, voz, memória, pesquisa, ferramentas e automação de tarefas, priorizando qualidade, privacidade, autonomia controlada e funcionamento local.

## Restrições inegociáveis

- **Sem custos obrigatórios:** não exigir APIs pagas, assinatura, créditos pagos, hospedagem paga ou serviços premium para o funcionamento básico.
- **Local-first e híbrido:** manter capacidades locais; serviços online gratuitos são opcionais e podem ter limites, indisponibilidade ou mudar suas condições.
- **Android nativo:** Kotlin e APIs Android, respeitando permissões, políticas, limitações de segundo plano e compatibilidade real.
- **Sem root obrigatório.**
- **Privacidade por padrão:** dados pessoais, arquivos, notificações e memórias ficam locais por padrão; qualquer envio de conteúdo privado para serviços externos exige autorização específica.
- **Sem falsa conclusão:** o Luo deve verificar o resultado de ações e declarar limitações, falhas e incertezas. Não afirmar que executou uma ação, pesquisou na web ou testou um recurso sem evidência.
- **Figma como referência obrigatória:** preservar a especificação visual aprovada; adaptar apenas o necessário ao Android. Não redesenhar sem autorização e validar a fidelidade comparando a implementação real com o Figma.
- **Evolução por etapas:** entregar incrementos utilizáveis com critérios de aceitação e testes, sem abandonar a ambição final.
- **Sem publicação automática:** código e builds de teste podem ser preparados na branch de trabalho; merge, produção e lançamento estável exigem autorização separada.
- **Testes honestos:** distinguir testes unitários, instrumentados/emulador e testes no aparelho físico. O sucesso de build não comprova funcionamento real de voz, inferência, automação ou integrações.

## Decisões aprovadas

### 1–10 — Fundamentos, inteligência, segurança e interface

1. **Arquitetura híbrida local + online opcional.** O modo local permanece utilizável; nenhum provedor pago é obrigatório.
2. **Ambição da primeira versão: Mini-Jarvis.** Conversa, voz, memória e comandos Android são o objetivo de produto, entregues progressivamente e validados por etapas.
3. **Automação Android ampla, dentro das permissões.** Usar APIs nativas e acessibilidade quando permitido; confirmar ações sensíveis.
4. **Voz com palavra de ativação opcional.** Priorizar detecção local, botão e conversa por voz; respeitar permissões, indicador de microfone, bateria e restrições de segundo plano.
5. **Memória adaptativa avançada.** Contexto e padrões, sugestões de memória, revisão/edição/exclusão e autorização configurável para persistência.
6. **Roteamento inteligente de modelos.** Escolher entre recursos locais e serviços online gratuitos conforme tarefa, qualidade, privacidade e disponibilidade; sem dependência de API paga.
7. **Agente de tarefas confiável.** Planejar, pesquisar, verificar resultados, tratar falhas e tentar alternativas gratuitas; relatar o que ocorreu de fato.
8. **Segurança em camadas.** PIN/biometria quando aplicável, permissões por recurso, credenciais protegidas, registro local, revogação de acesso e defesa contra instruções maliciosas vindas de páginas/mensagens.
9. **Figma é a especificação visual obrigatória.** Preservar cores, tipografia, espaçamento, componentes, navegação e animações; adaptar apenas o necessário para Android e validar por comparação.
10. **Máximo funcionamento offline.** Conversa local, memória, histórico, arquivos autorizados, comandos e rotinas compatíveis; indicar claramente funções que exigem conexão.

### 11–20 — Modelos, sincronização, autonomia, qualidade e entrega

11. **Gerenciador inteligente de modelos.** Recomendar modelos segundo compatibilidade, RAM, armazenamento, desempenho e bateria; importar, testar, selecionar ou remover somente com autorização. Aparelho-alvo informado: POCO X5 Pro, 8 GB RAM e 256 GB; testar no dispositivo real.
12. **Backup/sincronização híbridos e opcionais.** Local por padrão, backup protegido e seleção de categorias; nenhum serviço pago obrigatório. Sincronização desativada até ser configurada.
13. **Autonomia configurável por categoria.** Executar sequências autorizadas, registrar e verificar; pedir confirmação para exclusão, ações financeiras, envio/compartilhamento e outras ações de impacto.
14. **Pesquisa web avançada.** Comparar fontes, datas e contradições; usar alternativas gratuitas quando disponíveis; nunca inventar fontes nem alegar pesquisa sem acesso.
15. **Painel completo do agente.** Conversa integrada a tarefas, ferramentas, estado do modelo, automações, fontes, logs e diagnósticos, respeitando a referência do Figma.
16. **Atualizações seguras.** Exibir alterações, verificar compatibilidade e oferecer recuperação; distinguir atualização do aplicativo de atualização de modelos. Sem publicação silenciosa de versão estável.
17. **Testes rigorosos e evidências.** Build, testes unitários, UI, persistência, permissões, regressão, segurança, desempenho e consumo; funções críticas exigem teste no aparelho real antes de serem declaradas prontas.
18. **Entrega progressiva com qualidade.** Primeiro incremento útil, seguido de voz, palavra de ativação, automações e pesquisa; cada etapa terá critérios de aceitação e testes.
19. **Distribuição verificável.** Histórico no GitHub, builds APK de depuração, releases com notas, checksums, instruções e recuperação. Não publicar versão estável sem autorização.
20. **Desempenho adequado ao aparelho.** Medir latência, RAM e bateria; recomendar modelos maiores apenas quando viáveis; online gratuito é alternativa opcional para tarefas exigentes.

### 21–30 — Privacidade, recuperação, integrações e melhoria contínua

21. **Privacidade contextual por categoria.** Consultas genéricas podem usar serviços online se permitido; conversas pessoais, memórias, arquivos e notificações ficam locais por padrão. Compartilhamento sensível requer confirmação; controles devem ser visíveis e revogáveis.
22. **Recuperação inteligente e segura.** Classificar erros, tentar alternativas seguras, preservar dados e registrar diagnósticos. Pedir autorização ou verificar o estado antes de mudanças destrutivas, reset ou ações externas que possam duplicar operações.
23. **Integrações amplas em etapas.** Recursos centrais do celular, produtividade, apps compatíveis, compartilhamento e acessibilidade/UI automation; sempre com permissões e testes.
24. **Disponibilidade em segundo plano configurável.** Priorizar palavra de ativação e rotinas autorizadas, com controles para pausar e reduzir bateria; testar as restrições de Android/MIUI/HyperOS no aparelho.
25. **Interpretação contextual de comandos.** Usar contexto e preferências, esclarecer ambiguidades importantes e permitir ações reversíveis dentro das permissões; operações sensíveis ou irreversíveis exigem confirmação.
26. **Personalidade consistente do Luo.** Proativo, estratégico, técnico, claro e com humor moderado; sem fingir ser humano, esconder limites ou exagerar capacidades; coerente em texto e voz.
27. **Arquitetura modular do agente.** Núcleo de planejamento e seleção de ferramentas com interfaces padronizadas; ferramentas declaram capacidades/permissões; núcleo autoriza, registra e verifica resultados.
28. **Gerenciador inteligente de tarefas.** Dividir tarefas em etapas, dependências, prioridades e progresso; permitir pausar/cancelar/retomar; verificar estado antes de repetir ações externas para evitar duplicação.
29. **Melhoria contínua controlada.** Analisar erros, feedback e resultados, identificar padrões e propor melhorias a instruções, preferências e procedimentos. Estratégias temporárias podem ajustar-se dentro da sessão; alterações persistentes exigem aprovação, testes e rollback. Isso não significa treinar automaticamente o modelo.
30. **Cofre de privacidade adaptativo.** Permissões por categoria, autenticação adicional para ações sensíveis, credenciais armazenadas com proteção do sistema, ocultação de dados privados em notificações/logs, bloqueio configurável e revogação rápida. Envio de dados privados para serviços externos exige autorização específica.

### 31–38 — Voz, automação, arquivos e recursos pessoais

31. **Voz adaptativa com modos de disponibilidade.** Botão, conversa por voz e palavra de ativação opcional; modos de economia, disponibilidade ampliada e pausa total. Priorizar detecção local quando viável; indicar estado do microfone e limitações do Android/Xiaomi.
32. **Automação ampla com segurança.** Combinar comandos nativos, integrações oficiais e acessibilidade/UI automation quando permitido. Confirmar envio de mensagens, publicações, compras, exclusões e outras ações sensíveis. Verificar resultado e relatar bloqueios.
33. **Gerenciador inteligente de arquivos.** Pesquisar, classificar, resumir e organizar arquivos em locais autorizados. Permissões por pasta/categoria; controles próprios para alterações e compartilhamentos; confirmação para exclusões. Local por padrão.
34. **Planejador pessoal inteligente.** Organizar tarefas por prioridade, prazo e dependências; sugerir horários, identificar conflitos e acompanhar progresso. Integração opcional com calendários/apps compatíveis; lembretes locais offline quando possível; confirmar alterações importantes.
35. **Central inteligente de notificações.** Classificar notificações autorizadas, resumir, identificar prazos e sugerir ações. Permissões por aplicativo/categoria, pausa e controle de armazenamento; proteger conteúdo sensível nos logs; confirmar ações externas.
36. **Inteligência multimodal adaptativa.** Analisar imagens, fotos, capturas de tela, documentos digitalizados e áudio compatível; extrair texto, resumir e usar conteúdo visual em tarefas. Câmera somente quando solicitada; processamento local priorizado; autorização antes de enviar conteúdo privado a serviço externo. Não presumir que um modelo de texto analisa imagens/áudio sem componentes adicionais.
37. **Controle contextual máximo permitido do Android.** Usar comandos compatíveis para volume, brilho, conectividade e modos de uso. Orientar o usuário quando exigir intervenção ou permissão especial; sem root obrigatório e sem prometer controle de funções bloqueadas pelo sistema.
38. **Agente de pesquisa e verificação.** Decompor pesquisas complexas, consultar fontes independentes, verificar datas, comparar afirmações, detectar contradições e apresentar referências rastreáveis. Tentar alternativas gratuitas em caso de falha; separar fatos verificados de inferências e indicar quando não for possível confirmar. Offline não será apresentado como pesquisa atualizada.

## Diretrizes técnicas e de implementação

- Aplicativo Android nativo em Kotlin/Jetpack Compose, respeitando a arquitetura modular já existente.
- Manter armazenamento local e proteção por Android Keystore onde aplicável; não declarar o banco inteiro criptografado se apenas determinados campos forem protegidos.
- Modelos locais devem ser opcionais, explicitamente importados/baixados e validados; informar tamanho, origem, licença, compatibilidade e custo de dados móveis quando conhecidos.
- Nunca baixar modelos, enviar dados privados, conceder permissões, trocar modelos ou executar ações de impacto sem a autorização aplicável.
- Qualquer inferência sobre compatibilidade precisa ser confirmada com testes reais; não presumir que compilar equivale a funcionar.
- Manter mudanças pequenas, isoladas e revisáveis. Inspecionar os arquivos antes de alterá-los e executar testes adequados depois de cada etapa.
- Não sobrescrever o README atual sem análise e decisão específica: ele descreve o upstream Grok Build. Este documento separado registra o planejamento do Copy sem apagar a documentação existente.

## Estado conhecido e limites de validação

A base existente tem um app Android nativo e uma branch de trabalho com PR. Foi relatado anteriormente que o CI compilou o app e executou testes unitários/instrumentados em emulador; isso não substitui teste físico.

Ainda precisam de validação real, entre outros pontos: inferência local com um modelo compatível, saída de streaming, voz e palavra de ativação, execução em segundo plano no POCO, automação por acessibilidade, pesquisa web, integrações de agenda/notificações, recursos multimodais e fidelidade visual ao Figma. Não declarar essas funções concluídas sem evidências.

## Próxima sequência de trabalho — sem novos questionários

1. Registrar esta especificação na branch de trabalho.
2. Inspecionar a implementação existente e comparar cada decisão com código, testes e limitações atuais.
3. Produzir uma matriz de rastreabilidade: **decisão → estado atual → evidência → lacuna → próxima alteração → teste de aceitação**.
4. Implementar uma alteração pequena por vez, sem merge ou publicação em produção.
5. Em cada atualização, informar arquivos alterados, testes realmente executados, limitações restantes e uma forma de testar no Android/celular quando houver artefato disponível.

