## Authorize the app through xero

[Login in locally](https://login.xero.com/identity/connect/authorize?response_type=code&client_id=E34D71D2195E4DB68159D3AF5C6B171B&redirect_uri=http://localhost:3000/api/oauth&scope=openid+profile+email+accounting.transactions+offline_access+accounting.reports.read+accounting.settings+accounting.contacts+files+assets)

[Log in on staging](https://login.xero.com/identity/connect/authorize?response_type=code&client_id=480C874C15884CF89E8B817AD223F7F9&redirect_uri=https://xero-syncer-staging.herokuapp.com/api/oauth&scope=openid+profile+email+accounting.transactions+offline_access+accounting.reports.read+accounting.settings+accounting.contacts+files+assets)

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
