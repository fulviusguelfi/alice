# Migração do allowlist SSH: senha → chave

**Criado em:** 2026-04-11
**Motivo:** `.claude/settings.json` contém a senha `REDACTED` do `bill@192.168.0.200` em texto puro dentro do `permissions.allow`. Essa senha já vazou nos commits **9bf65ae** e **bf865d9** (histórico público se o repo for público). Este doc define a migração para chave SSH.

---

## 1. Ordem das ações (ordem importa!)

1. **ROTACIONAR A SENHA NO BILL PRIMEIRO** (torna o vazamento histórico inócuo).
2. Gerar par de chaves SSH para uso exclusivo do projeto Alice.
3. Instalar a chave pública em `bill@192.168.0.200:~/.ssh/authorized_keys`.
4. Validar login sem senha.
5. Aplicar o patch em `.claude/settings.json` (remove entradas com `-pw` e adiciona wildcards que só permitem `-i <keyfile>`).
6. Testar os comandos comuns (hostname, ollama list, etc) pelo novo caminho.
7. **Opcional (forte recomendação):** reescrever histórico do Git para remover a senha dos commits antigos.

---

## 2. Passo a passo manual

### 2.1 Rotacionar senha no bill

No terminal do bill (ou via sessão interativa existente):

```bash
passwd
# entra senha atual, nova senha duas vezes
```

Escolher senha forte, **não anotar em lugar nenhum commitável**. A partir desse momento, a senha `REDACTED` que está no histórico do Git não serve mais para nada.

### 2.2 Gerar chave dedicada para o projeto

No Windows (WSL, Git Bash ou PowerShell com OpenSSH):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/alice_bill -C "alice-project@bill" -N ""
```

Isso cria:
- `~/.ssh/alice_bill` (chave privada — **nunca commitar**)
- `~/.ssh/alice_bill.pub` (chave pública)

Se você preferir PuTTY/.ppk (compatível com plink sem conversão):

```bash
ssh-keygen -t ed25519 -f ~/.ssh/alice_bill -C "alice-project@bill" -N ""
# depois abra PuTTYgen → Load → ~/.ssh/alice_bill → Save private key → ~/.ssh/alice_bill.ppk
```

### 2.3 Instalar chave pública no bill

```bash
ssh-copy-id -i ~/.ssh/alice_bill.pub bill@192.168.0.200
# pede a senha NOVA uma vez, depois nunca mais
```

Se `ssh-copy-id` não estiver disponível:

```bash
cat ~/.ssh/alice_bill.pub | ssh bill@192.168.0.200 'mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys'
```

### 2.4 Validar sem senha

```bash
ssh -i ~/.ssh/alice_bill bill@192.168.0.200 'hostname && uname -a'
# esperado: login instantâneo, sem prompt de senha
```

Para plink/pscp (usar .ppk):

```bash
"/c/Program Files/PuTTY/plink" -ssh -batch -i "C:/Users/Usuario/.ssh/alice_bill.ppk" -hostkey 'SHA256:jyBisVeqmrEVI7ZnwPKkF+ooGNml70q8/c3UpCNRFv0' bill@192.168.0.200 "hostname"
```

### 2.5 Aplicar patch em settings.json

Ver seção **3. Patch do settings.json** abaixo.

### 2.6 Desabilitar autenticação por senha no bill (recomendado)

Depois que a chave estiver funcionando:

```bash
sudo sed -i 's/^#*PasswordAuthentication .*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl restart sshd
```

Isso fecha permanentemente a porta para ataques de brute force na senha.

### 2.7 (Opcional) Reescrever histórico do Git

**Destrutivo — requer decisão consciente.** Reescrever histórico invalida clones existentes e força todos os colaboradores a refazer fetch. Para um projeto em estágio inicial com você como único dev, é viável. Para projeto com múltiplos clones, pesar custo.

Ferramenta recomendada: `git filter-repo` (mais rápido e seguro que `git filter-branch`).

```bash
pip install git-filter-repo
cd /c/Users/Usuario/Desktop/alice
git filter-repo --replace-text <(echo 'REDACTED==>REDACTED')
git push origin --force --all
git push origin --force --tags
```

Depois, notifique qualquer colaborador para re-clonar. **Não faça isso antes da etapa 2.1 (rotacionar senha).**

---

## 3. Patch do settings.json

### 3.1 Entradas a remover

Todas as linhas do `allow` que contêm `REDACTED` ou forçam `PasswordAuthentication`:

- Linha 43 — `sshpass -p "REDACTED" ssh ...`
- Linha 46 — `ssh -o PreferredAuthentications=password ...`
- Linhas 81, 84, 86, 87, 88, 89, 90 — `plink -pw 'REDACTED' ...`
- Linhas 95, 96, 98, 99, 100, 102 — `plink/pscp -pw 'REDACTED' ...`
- Linhas 105, 106, 107, 108, 109, 110 — `plink/pscp -pw REDACTED ...`

### 3.2 Entradas a adicionar

Substituir por 4 padrões wildcard que **exigem** a flag `-i <keyfile>` e **proíbem** `-pw`:

```json
"Bash(\"/c/Program Files/PuTTY/plink\" -ssh -batch -i * bill@192.168.0.200:*)",
"Bash(\"/c/Program Files/PuTTY/pscp\" -batch -i * bill@192.168.0.200:*)",
"Bash(\"/c/Program Files/PuTTY/pscp\" -batch -i *:*)",
"Bash(ssh -i * bill@192.168.0.200:*)"
```

Observação: os padrões do Claude Code usam `:*` como wildcard de argumentos. Qualquer entrada com `-pw` deixa de casar com esses padrões e cai em prompt de autorização no uso, em vez de execução silenciosa.

### 3.3 Diff conceitual

```diff
--- a/.claude/settings.json
+++ b/.claude/settings.json
@@ permissions.allow @@
-      "Bash(sshpass -p \"REDACTED\" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 bill@192.168.0.200 \"echo OK\")",
-      "Bash(ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -o PreferredAuthentications=password -o PubkeyAuthentication=no -o PasswordAuthentication=yes bill@192.168.0.200 \"echo connected\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' bill@192.168.0.200 \"hostname\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -batch -hostkey '*' -pw 'REDACTED' bill@192.168.0.200 hostname)",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch bill@192.168.0.200 hostname)",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:jyBisVeqmrEVI7ZnwPKkF+ooGNml70q8/c3UpCNRFv0' bill@192.168.0.200 \"hostname && uname -a\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"echo '=== OS ===' ...\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"echo '=== DISK ===' ...\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"nohup ollama pull qwen2.5:3b ...\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"echo '=== qwen pull ===' ...\")",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"which jq curl ...\")",
-      "Bash('/c/Program Files/PuTTY/plink' -ssh -pw REDACTED -batch -hostkey SHA256:... bill@192.168.0.200 'echo bench progress ...')",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"ps -ef | grep -E 'bench|ollama' ...\")",
-      "Bash(\"/c/Program Files/PuTTY/pscp\" -pw 'REDACTED' -batch -hostkey 'SHA256:...' \"c:/...bench_ollama.sh\" bill@192.168.0.200:/home/bill/bench_ollama.sh)",
-      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200 \"chmod +x /home/bill/bench_ollama.sh; setsid ...\")",
-      "Bash('/c/Program Files/PuTTY/plink' -ssh -pw REDACTED -batch -hostkey SHA256:... bill@192.168.0.200 'LATEST=... jq ...')",
-      "Bash('/c/Program Files/PuTTY/plink' -ssh -pw REDACTED -batch -hostkey SHA256:... bill@192.168.0.200 'ls -la /home/bill/alice_bench/; ...')",
-      "Bash('/c/Program Files/PuTTY/plink' -ssh -pw REDACTED -batch -hostkey SHA256:... bill@192.168.0.200 'ls -la /home/bill/alice_bench/; NOT RUNNING ...')",
-      "Bash('/c/Program Files/PuTTY/plink' -ssh -pw REDACTED -batch -hostkey SHA256:... bill@192.168.0.200 'F=/home/bill/alice_bench/results_20260411_000606.jsonl; Metricas agregadas ...')",
-      "Bash('/c/Program Files/PuTTY/plink' -ssh -pw REDACTED -batch -hostkey SHA256:... bill@192.168.0.200 'F=/home/bill/alice_bench/results_20260411_000606.jsonl; for P in ...')",
-      "Bash(\"/c/Program Files/PuTTY/pscp\" -pw 'REDACTED' -batch -hostkey 'SHA256:...' bill@192.168.0.200:/home/bill/alice_bench/results_20260411_000606.jsonl \"c:/...results_bill_local_20260411.jsonl\")",
+      "Bash(\"/c/Program Files/PuTTY/plink\" -ssh -batch -i * bill@192.168.0.200:*)",
+      "Bash(\"/c/Program Files/PuTTY/pscp\" -batch -i * bill@192.168.0.200:*)",
+      "Bash(\"/c/Program Files/PuTTY/pscp\" -batch -i *:*)",
+      "Bash(ssh -i * bill@192.168.0.200:*)",
```

O arquivo final terá ~21 linhas a menos no `allow` e 4 linhas novas.

### 3.4 Arquivo de preview pronto

Produzi um arquivo pronto com as mudanças aplicadas em [settings.json.proposed](settings.json.proposed) na mesma pasta. **Não sobrescrevi o settings.json real** — você aplica quando quiser, depois de completar os passos 2.1 a 2.4.

Para aplicar:

```bash
# dentro da pasta do projeto
cp docs/security/settings.json.proposed .claude/settings.json
# revise com git diff antes de salvar
git diff .claude/settings.json
```

---

## 4. Validação pós-migração

Depois de aplicar o patch, rodar estes testes (cada um deve funcionar sem prompt de senha):

```bash
# ping SSH simples
"/c/Program Files/PuTTY/plink" -ssh -batch -i "C:/Users/Usuario/.ssh/alice_bill.ppk" bill@192.168.0.200 "hostname"

# ollama list
"/c/Program Files/PuTTY/plink" -ssh -batch -i "C:/Users/Usuario/.ssh/alice_bill.ppk" bill@192.168.0.200 "ollama list"

# pscp upload
"/c/Program Files/PuTTY/pscp" -batch -i "C:/Users/Usuario/.ssh/alice_bill.ppk" test.txt bill@192.168.0.200:/tmp/test.txt

# tentativa com senha (deve pedir autorização manual, não executar)
"/c/Program Files/PuTTY/plink" -ssh -pw 'qualquercoisa' bill@192.168.0.200 "echo deveria pedir auth"
```

Se o último comando executar direto sem pedir autorização, o allowlist ainda tem padrão permissivo demais — revisar.

---

## 5. Fontes de contaminação a verificar

Antes de considerar o leak resolvido, grep no repositório inteiro por qualquer ocorrência da senha antiga:

```bash
git grep 'REDACTED'
git log -p -S'REDACTED' --all
```

Qualquer ocorrência fora do histórico (ou seja, em arquivos atuais) deve ser removida antes de commitar.

---

## 6. Checklist de conclusão

- [ ] Senha rotacionada no bill
- [ ] Chave ed25519 gerada em `~/.ssh/alice_bill`
- [ ] Chave pública instalada em `bill:~/.ssh/authorized_keys`
- [ ] Login sem senha validado (`ssh -i ~/.ssh/alice_bill bill@192.168.0.200 hostname`)
- [ ] Patch aplicado em `.claude/settings.json`
- [ ] Comandos plink/pscp funcionam via chave
- [ ] `PasswordAuthentication no` aplicado no `sshd_config` do bill (opcional)
- [ ] Histórico do Git reescrito (opcional — só se repo é público)
- [ ] `git grep 'REDACTED'` retorna vazio nos arquivos atuais
- [ ] Memória `machine_bill.md` atualizada para remover qualquer referência a senha e mencionar caminho da chave
