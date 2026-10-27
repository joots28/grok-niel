# Integrar REA ao Grok Build

Este guia configura o REA como servidor MCP do Grok Build **no computador/ambiente em que o Grok Build é executado**. Não copie o código inteiro do REA para este repositório: a integração oficial usa o pacote npm e mantém a configuração do agente separada do código do projeto.

## Pré-requisitos

- Node.js em uma versão suportada pelo REA (22.x a partir de 22.23.4, 24.x a partir de 24.22.1 ou 26.12.1+).
- npm disponível no mesmo ambiente do Grok Build.
- Acesso ao terminal desse ambiente.

## Instalação

No terminal do ambiente do Grok Build, execute:

```bash
npx rea-agents@latest setup --client grok_build
```

Leia o plano apresentado pelo instalador e aprove somente as alterações que você deseja aplicar. O REA deve registrar seu servidor MCP no arquivo de configuração do Grok Build e instalar as instruções de fluxo de trabalho correspondentes.

O Grok Build lê a configuração em `$GROK_HOME/config.toml`; se `GROK_HOME` não estiver definido, usa `~/.grok/config.toml`.

## Verificação

Depois de concluir a configuração:

1. Reinicie o Grok Build para recarregar os servidores MCP.
2. Confirme que o servidor `rea` aparece entre as integrações disponíveis.
3. Se houver problemas, consulte a documentação oficial de instalação e diagnóstico: https://github.com/morluto/rea/blob/main/docs/installation.md

## Observações

- Esta integração usa o REA oficial via npm; não duplica nem incorpora o repositório externo.
- Não adicione tokens, senhas ou outras credenciais ao repositório.
- A instalação precisa ocorrer no ambiente que executa o Grok Build. Um commit no GitHub, por si só, não instala um servidor MCP na máquina.
