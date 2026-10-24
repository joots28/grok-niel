# Copy para Android — versão de desenvolvimento

Aplicativo nativo em Kotlin + Jetpack Compose. O ícone de inicialização é próprio do Copy, desenhado em vetor para Android Adaptive Icons.

## O que esta versão já oferece

- Navegação entre Início, Conversa, Comandos, Controle, Memória e Histórico.
- Conversas, mensagens e memórias armazenadas localmente em SQLite.
- Histórico de conversas, pesquisa e exclusão com confirmação.
- Memórias com operações de criar, listar, editar e excluir; exportação de memórias em JSON pelo seletor de arquivos do Android.
- Gerenciador para importar e remover manualmente um modelo de IA local compatível com LiteRT-LM.
- Caminho de inferência local conectado à conversa quando um modelo compatível está importado. As mensagens não são enviadas a uma API remota nesta arquitetura local-first.
- Contexto de memória e histórico limitado enviado ao modelo local para manter continuidade de conversa.

O conteúdo de mensagens e títulos é protegido por AES-GCM, mas o banco SQLite como um todo e todos os metadados não são criptografados integralmente. Não trate esta versão como um cofre de segurança auditado.

## O que ainda não está pronto

- Voz real, reconhecimento e síntese de fala, palavra de ativação ou escuta em segundo plano.
- Pesquisa na internet integrada, comparação de fontes e citações automáticas.
- Controle real do Wi-Fi, Bluetooth, notificações, calendário, outros aplicativos ou interface via acessibilidade.
- Análise multimodal de imagens/áudio, sincronização em nuvem ou rotinas em segundo plano.
- Comparação visual pixel a pixel com o arquivo Figma.
- Validação de inferência com o modelo real em um aparelho físico. Compilar o app e passar em testes de unidade/instrumentação não comprovam que o modelo vai carregar e responder corretamente no seu POCO.

Os controles de demonstração não devem ser interpretados como ações reais no Android. O Copy deve informar quando a IA local não está instalada ou não consegue gerar uma resposta.

## Como obter o APK de desenvolvimento

1. Abra a [validação Android do Copy no GitHub Actions](https://github.com/joots28/grok-niel/actions/workflows/copy-android.yml).
2. Abra a execução mais recente com resultado **success** na branch de trabalho da PR #1.
3. Na seção **Artifacts**, baixe `copy-android-debug-apk` e extraia o ZIP.
4. Instale `app-debug.apk` no Android. Se o sistema pedir, autorize temporariamente a instalação de aplicativos para o gerenciador de arquivos ou navegador que você usou.
5. Abra o Copy. Sem um modelo compatível importado, a conversa salva mensagens localmente, mas não gera respostas de IA.

Este é um **APK de depuração**, útil para avaliação. Não é uma versão estável assinada com uma chave de distribuição própria. Para distribuição estável será necessário configurar assinatura de release fora do repositório, testar em aparelho físico e publicar uma versão identificada.

## Ativar a IA local

1. Na Central de controle do Copy, toque em **Baixar modelo gratuito**. O botão abre a página de download no navegador; nenhum modelo é baixado automaticamente.
2. Baixe um arquivo LiteRT-LM compatível com o importador mostrado no app. O download usa sua conexão de internet e pode consumir dados móveis.
3. Volte ao Copy e toque em **Importar arquivo .litertlm**.
4. Selecione o arquivo baixado e aguarde a validação/cópia para o armazenamento privado do aplicativo.
5. Abra uma conversa e envie uma mensagem de teste.

A checagem de importação verifica tamanho e assinatura inicial do arquivo; a compatibilidade completa só é comprovada quando o runtime consegue carregar o modelo. Um modelo pequeno pode responder mais lentamente ou com qualidade limitada. Não há cobrança obrigatória por API nesta arquitetura, mas a compatibilidade e o desempenho ainda precisam ser validados no celular real.

## Compilação

- JDK 17
- Gradle 8.9
- Android SDK Platform 35
- Projeto Gradle em `android/`

O workflow `.github/workflows/copy-android.yml` executa `assembleDebug`, testes unitários, testes instrumentados em emulador e publica o APK como artefato temporário. Os artefatos do Actions expiram; uma release estável deverá usar um fluxo separado de assinatura e distribuição.
