# Estoque API — Clínica (Spring Boot + RabbitMQ)

Backend do sistema de gestão de estoque da clínica.

## Stack
- Java 17 + Spring Boot 3.3 (Web, Security, Validation, Data JPA)
- H2 em arquivo (dev) — troque por PostgreSQL em prod com 2 env vars
- RabbitMQ (eventos: movimentação/lote → alerta)
- JWT (login em `/usuarios/login`)

## Como rodar

```bash
docker compose up --build     # RabbitMQ + API
# ou dev local:
docker compose up -d rabbitmq && mvn spring-boot:run
```

## Login

```bash
curl -X POST http://localhost:8080/usuarios/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"eltoncarneiro55@gmail.com","senha":"1234"}'
# -> { "token": "jwt..." }  (use nas demais rotas: Authorization: Bearer <token>)
```

## Rotas

| Método | Rota | Descrição |
|---|---|---|
| POST | `/usuarios/login` | Login → JWT (público) |
| POST | `/administrativo/funcionarios` | Cadastrar funcionário |
| GET | `/administrativo/funcionarios/lista` | Listar funcionários |
| GET/PUT/DELETE | `/administrativo/funcionarios/{id}` | Buscar/alterar/excluir |
| GET/POST | `/produtos` | Listar/cadastrar produtos |
| GET/POST | `/fornecedores` | Listar/cadastrar fornecedores |
| GET | `/estoque/lotes` | Lotes (fabricante, validade, saldo) |
| POST | `/estoque/lotes` | Cadastrar lote (gera entrada automática) |
| POST | `/estoque/movimentacoes` | Entrada/saída (valida saldo) |
| GET | `/estoque/movimentacoes` | Histórico |
| POST | `/estoque/transferencias` | Transferência entre empresas |
| GET | `/estoque/transferencias` | Lista transferências |
| PATCH | `/estoque/transferencias/{id}/concluir` | Concluir transferência |
| GET | `/estoque/alertas` | Alertas da regra dos 15 dias (persistidos) |
| GET | `/estoque/curva-abc` | Curva ABC das saídas |
| GET | `/empresas` | Empresas do grupo |

## Regra de negócio

**Alerta (15 dias):** lote entra em alerta se vence em até 15 dias OU se o saldo
atual dura 15 dias ou menos (consumo médio = saídas dos últimos 30 dias ÷ 30).
Alertas são recalculados e persistidos a cada movimentação e expostos em
`GET /estoque/alertas`.

**Curva ABC:** Pareto sobre as saídas — A = até 80% acumulado, B = até 95%, C restante.

**Validações** retornam HTTP 400 com `{ "message": "..." }` (ver `GlobalExceptionHandler`).

## RabbitMQ

`movimentacao.registrada` → recalcula alertas → `alerta.gerado` → consumer
(pronto para e-mail/push). Painel: http://localhost:15672 (guest/guest).

## Deploy no Fly.io

```bash
fly launch --no-deploy
fly secrets set JWT_SECRET="uma-chave-grande-e-aleatoria"
fly secrets set RABBITMQ_HOST=... RABBITMQ_PORT=5672 RABBITMQ_USER=... RABBITMQ_PASS=...
fly deploy
```


---

## Troubleshooting

### Erro: `null value in column "ativo" of relation "produtos" violates not-null constraint`
O banco `estoque` **já tinha uma tabela `produtos` criada por outro projeto** (com
colunas extras como `ativo`, `preco`... com `NOT NULL`). O `ddl-auto: update` só
adiciona colunas novas — **não remove/altera as antigas**. Solução: apagar as
tabelas legadas para o Hibernate recriar o schema correto:

```sql
-- conecte no banco 'estoque' e execute:
DROP TABLE IF EXISTS alertas, transferencias, movimentacoes,
  lotes, empresas, fornecedores, produtos, funcionarios, usuarios CASCADE;
```
Ou recrie o banco do zero: `DROP DATABASE estoque; CREATE DATABASE estoque;`
Depois, basta reiniciar a API (o `update` recria tudo).

### Erro: `AmqpConnectException: Connection refused` (localhost:5672)
O **RabbitMQ não está rodando**. Suba com:
```bash
docker compose up -d rabbitmq postgres   # ou tudo: docker compose up --build
```
A API sobe mesmo sem o RabbitMQ (os listeners ficam tentando reconectar), mas as
movimentações só publicam eventos com o broker no ar.
