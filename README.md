## Login with xero

[Xero link](https://login.xero.com/identity/connect/authorize?response_type=code&client_id=E34D71D2195E4DB68159D3AF5C6B171B&redirect_uri=http://localhost:3000/api/oauth&scope=openid+profile+email+accounting.transactions+offline_access+accounting.reports.read+accounting.settings+accounting.contacts+files+assets)

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
