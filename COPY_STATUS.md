# Copy — status real e fila de conclusão

Atualizado em 24/10/2026. Repositório: joots28/grok-niel; branch de trabalho: copy/foundation-requirements.

Este documento separa implementação em código, validação automática e teste físico. Um build verde não comprova que o modelo local funciona bem no telefone, nem que integrações Android ainda não implementadas funcionam.

## Estado de entrega

- A PR #1 continua aberta, em rascunho e não mesclada.
- Último commit com CI confirmado como verde antes da mudança de histórico: 695a267a, com execução de Actions 40058733181 concluída com sucesso.
- A nova etapa de exportação/exclusão de conversas está no commit 1d16ec3c. A execução 40089328644 ainda aparecia como pendente na última consulta; portanto, essa etapa não deve ser considerada validada pelo CI até a execução terminar com sucesso.
- O artefato da execução verde anterior é um APK de depuração, não uma versão assinada de produção. Ele não inclui as alterações feitas depois do commit 695a267a.
- Nenhum teste em aparelho físico foi realizado nesta sessão. O dispositivo prioritário continua sendo o POCO X5 Pro, 8 GB de RAM e 256 GB.

## Funcionalidades presentes no código

- Aplicativo nativo em Kotlin + Jetpack Compose, com Início, Conversa, Comandos, Controle, Memória e Histórico.
- Conversas e mensagens persistidas em SQLite local; a memória fica separada. O histórico pode ser pesquisado e conversas individuais podem ser excluídas com confirmação.
- Títulos e conteúdos selecionados são cifrados com AES-GCM usando uma chave do Android Keystore. Os bancos SQLite não são criptografados integralmente: IDs, papéis, datas e relações permanecem como metadados.
- Memórias podem ser adicionadas, listadas, editadas e excluídas; há exportação JSON e exclusão total de memórias.
- O modelo de IA local é opcional: o usuário obtém o arquivo .litertlm e o importa manualmente. Não há chave de API ou servidor pago obrigatório para essa inferência local. A compatibilidade final, a latência e o consumo de RAM precisam de teste físico.
- Há entrada de voz por botão usando o reconhecimento local do Android quando disponível e leitura manual de respostas por Text-to-Speech. O reconhecimento local requer Android 12+ e suporte real do serviço no aparelho; leitura em voz alta depende do sintetizador instalado.
- O commit 1d16ec3c acrescenta exportação JSON do histórico completo usando o seletor de documentos do Android, exclusão total com confirmação, testes unitários do serializador e um teste instrumentado de exclusão em cascata. A validação automática dessa alteração permanece pendente na última consulta.

## Ainda não implementado ou não comprovado

1. Teste no POCO X5 Pro: instalação, retomada após fechar o app, inferência real do modelo, latência, memória e bateria.
2. Criptografia integral do banco; no momento, somente campos de texto selecionados são cifrados.
3. Bloqueio do app por PIN/biometria e cofre de privacidade adaptativo completo.
4. Palavra de ativação contínua e serviço de voz em segundo plano.
5. Pesquisa na web, verificação de fontes e roteamento para provedores remotos gratuitos opcionais.
6. Gerenciador real de arquivos, integração de calendário, leitura de notificações, controle de configurações Android e automação de outros apps por acessibilidade.
7. Sincronização/backup opcional, importação validada de exportações e recuperação completa.
8. Extração completa da referência Figma e comparação visual tela a tela. A fidelidade pixel a pixel ainda não está comprovada.
9. Revisão final de acessibilidade, desempenho e compatibilidade em diferentes versões e fabricantes.

## Portões de qualidade

- Build e testes vinculados ao commit exato; falhas devem ser corrigidas antes de marcar a etapa como concluída.
- Testes unitários, instrumentados/emulador e físicos devem ser diferenciados.
- Permissões, erros, limites e estado real das integrações devem estar visíveis na UI.
- Nenhum segredo no código, APK ou logs.
- Sem merge, produção ou lançamento estável sem autorização separada.
