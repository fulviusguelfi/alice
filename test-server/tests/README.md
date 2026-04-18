# Alice Integration Tests

Testes de integração que rodam contra o test-server local via RCON. Complementam
os testes unitários JUnit em `mod/src/test/java/`.

## Setup inicial (uma vez)

1. Edite `test-server/server.properties`:
   ```
   enable-rcon=true
   rcon.port=25575
   rcon.password=alicetest
   ```

2. Instale pytest (stdlib only para RCON):
   ```
   pip install pytest
   ```

3. Suba o test-server (`run.bat` no Windows). Aguarde a linha `Done (Xs)! For help...`
   — pode demorar ~2min com 205 mods.

## Rodar

```
cd test-server
pytest tests/ -v
```

Com servidor offline, todos os testes falham fast no fixture `rcon` (timeout
de connect). Pra configurar host/senha via env:

```
ALICE_RCON_HOST=192.168.0.225 ALICE_RCON_PASSWORD=outra pytest tests/
```

## Estrutura

- `rcon_client.py` — cliente RCON puro stdlib.
- `log_tail.py` — tail de `logs/latest.log` com `wait_for(regex)` / `ensure_absent(regex)`.
- `conftest.py` — fixtures `rcon`, `log_tail`, `clean_world`.
- `test_alice_behavior.py` — cenários de comportamento.

## Convenções

- **Testes `xfail`** documentam bugs conhecidos sem fazer a suite quebrar — ex:
  `test_alice_dives_into_water` (MovementSwim não portado). Quando a feature for
  implementada, o teste vira PASS e o `@pytest.mark.xfail` é removido.
- Cada teste usa coordenadas distantes umas das outras (100,100 / 150,100 /
  200,200) pra não conflitar construção do mundo.
- `clean_world` fixture roda `difficulty peaceful`, `time set day`, `kill` — mas
  não remove blocos que o teste colocou; cada teste coloca blocos em sua zona.

## O que AINDA precisa ser feito

- **Wire up RCON → Alice goto**: hoje os testes teleportam Alice mas não
  conseguem mandar ela navegar. Duas opções:
  1. Adicionar comando `/alice goto <x> <y> <z>` no mod (server command).
  2. Ter um jogador fake conectado que envia "alice, ir para X Y Z" no chat.
  Preferência: (1) — simples, não depende de cliente.
- **Baseline de log-assert**: `LogTail.reset_baseline()` antes da ação, não do
  teste inteiro — evitar pegar noise de outro teste concorrente.
