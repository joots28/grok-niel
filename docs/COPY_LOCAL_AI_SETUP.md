# Copy — ativar respostas com IA local

## Custo e privacidade

O Copy usa o runtime aberto LiteRT-LM e pode gerar texto no próprio Android. Não é necessária uma chave de API, assinatura de IA nem servidor pago. A inferência não envia as mensagens para um provedor remoto.

O primeiro download do modelo é opcional e tem cerca de 329 MB. Ele usa a conexão de internet escolhida pelo usuário; podem existir custos de dados impostos pelo plano móvel. Depois de baixado e importado, o modelo funciona sem conexão. O aplicativo não baixa modelos automaticamente e não declara a permissão \`INTERNET\`.

## Modelo inicial sugerido

- **Modelo:** Qwen3-0.6B, conversão LiteRT-LM de aproximadamente 329 MB.
- **Licença publicada no repositório do modelo:** Apache-2.0.
- **Repositório e licença:** https://huggingface.co/litert-community/Qwen3-0.6B
- **Arquivo sugerido:** \`Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm\`

A qualidade de um modelo de 0,6 bilhão de parâmetros é mais limitada que a de serviços maiores. Velocidade, memória ocupada e estabilidade precisam ser medidas no aparelho real; ainda não há certificação de desempenho para o POCO X5 Pro.

## Instalação no Android

1. Na Central de controle do Copy, toque em **Baixar modelo gratuito**.
2. No navegador, baixe o arquivo \`.litertlm\` sugerido e guarde-o em Downloads.
3. Volte ao Copy e toque em **Importar arquivo .litertlm**.
4. Selecione o arquivo baixado. O app confere a assinatura inicial e copia o modelo para seu armazenamento privado.
5. Abra uma conversa e envie uma mensagem para iniciar a inferência local.

A primeira inicialização pode demorar e o modelo usa memória RAM adicional. Se o Android encerrar o app ou o modelo falhar ao carregar, feche outros aplicativos e tente novamente. Não reduza as proteções do dispositivo.

## Limites da primeira versão

- Somente geração de texto local; voz, execução de comandos Android e pesquisa web não estão conectadas.
- O app salva histórico e memórias separadamente. Somente memórias explicitamente salvas são incluídas no contexto.
- A leitura de assinaturas não garante que todo arquivo seja um modelo válido; a inicialização do runtime é a verificação final.
- O arquivo do modelo é removido quando o usuário o remove no app; desinstalar o Copy também remove seu armazenamento privado.
- A compatibilidade precisa ser validada em CI e em um dispositivo físico antes de declarar o recurso pronto.
