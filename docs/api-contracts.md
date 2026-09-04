# API Contracts

REST API contracts will be expanded during backend implementation phases.

## Health Check

- Method: `GET`
- Path: `/api/health`
- Purpose: Confirms that the backend API application is running.
- Response example:

```json
{
  "status": "UP",
  "service": "supply-chain-api"
}
```
