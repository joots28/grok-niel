# Copy — status de implementação

Atualizado em 2026-10-25.

## Confirmado

- O repositório base é um workspace Rust grande, originalmente voltado a um agente de terminal.
- A branch de trabalho é copy-foundation; a branch principal não foi alterada.
- A primeira fundação Android foi adicionada em android/ usando Kotlin e Jetpack Compose.
- O aplicativo tem interface provisória de conversa, persistência SQLite, comandos offline limitados, entrada de voz via serviço do Android e leitura opcional via TextToSpeech.
- Foi adicionado um workflow para executar testes unitários e montar o APK de debug.

## Ainda não confirmado

- O build não foi executado localmente nesta sessão. O resultado do CI precisa ser consultado antes de declarar a compilação validada.
- A interface ainda não foi comparada com o Figma específico do Copy; não declarar correspondência visual exata.
- A camada de IA Rust não está conectada à interface Android.
- Não existe integração Kotlin–Rust nem contrato de API local implementados.
- Não existe ainda inferência local, roteamento de provedores, memória de longo prazo, cofre de credenciais ou integração ampla com Android.
- Segurança, consumo de bateria, acessibilidade, compatibilidade e uso contínuo precisam de testes em aparelho real.

## Próxima sequência técnica

1. Confirmar o build e os testes pelo CI.
2. Identificar a referência exata do Figma do Copy e alinhar a interface sem redesenhá-la por conta própria.
3. Mapear a fronteira de API do runtime Rust e validar comunicação Kotlin–Rust.
4. Implementar uma solicitação real de ponta a ponta com cancelamento, timeout e tratamento de erros.
5. Adicionar memória local controlável e migrações, depois os provedores opcionais.
6. Validar microfone, TextToSpeech, ciclo de vida, bateria e acessibilidade no Android real.

A criação de arquivos e a conclusão de planejamento não significam que o produto esteja pronto para uso contínuo ou distribuição.
