## Authorize the app through xero

[Login in locally](https://login.xero.com/identity/connect/authorize?response_type=code&client_id=E34D71D2195E4DB68159D3AF5C6B171B&redirect_uri=http://localhost:3000/api/oauth&scope=openid+profile+email+accounting.transactions+offline_access+accounting.reports.read+accounting.settings+accounting.contacts+files+assets)

## Set up app in xero

https://developer.xero.com/app/manage

## Env vars

Create dev-config.edn (or use dev-config.sample.edn). Add required env vars
\*Env vars are run against a malli spec. Make sure to update `xero-syncer.specs.env` when changes env vars

```bash
# Used to access the xero-syncer endpoints
export API_KEY=secret

export CLOUDAMQP_URL=amqps://guest:guest@localhost:5672

# Xero accounting specifics
export ACCOUNTING__DEFAULT_COGS_ACCOUNT=500
export ACCOUNTING__DEFAULT_SALES_ACCOUNT=400
export ACCOUNTING__SHIPPING_REVENUE_ACCOUNT=400

# Xero app details
export XERO__OAUTH_CALLBACK_URI=http://localhost:3000/api/oauth
export XERO__CLIENT_ID=XERO-CLIENT-ID
export XERO__CLIENT_SECRET=XERO-CLIENT-SECRET
export XERO__TENANT_NAME=Demo Company (US)

# Postgres details
export DB__NAME=postgres
export DB__USER=postgres
export DB__PASSWORD=postgres
export DB__HOST=localhost
export DB__PORT=5432

# Local dev settings
export DEV=true
export PORT=3000
export NREPL_PORT=7000
```

## Start docker compose

1. This will start rabbitmq and postgres

```bash
docker-compose up
```

## Restore DB

1. With DB named postgres
1. With username postgres

```bash
psql -U postgres -h localhost -p 5432 postgres < dump.sql
```

Enter in password `postgres`

## Dev

RabbitMQ portal available at: http://localhost:15672/

- Username: `guest`
- Password: `guest`

## API docs

http://localhost:3000/api/api-docs/index.html
