# BublAPI

**BublAPI** is a backend-first platform for building and integrating business applications through a unified API.

The project is structured as a monorepo containing the backend services, web applications, landing page, and deployment configuration used by the BublAPI ecosystem.

## BublAPI Dent

The first BublAPI product is **BublAPI Dent** — an API and supporting applications for dental clinics.

It provides backend functionality for clinic management, including:

- clinics and users;
- patients and doctors;
- roles and access control;
- dental services and clinic-specific services;
- doctor schedules and schedule exceptions;
- appointments and appointment services;
- notifications;
- API keys for external integrations.

The goal is to provide a ready-to-use backend layer that can be integrated with clinic websites, internal systems, mobile applications, CRM systems, and other clients.

## Repository structure

```text
bublapi/
├── apps/
│   ├── admin/          # BublAPI administration panel
│   └── dent-demo/      # Demo application for BublAPI Dent
│
├── services/
│   └── dent-backend/   # Spring Boot backend for BublAPI Dent
│
├── landing/            # Main BublAPI website
│
└── deploy/             # Docker Compose deployment configuration
```

## Backend

The BublAPI Dent backend is built with **Java** and **Spring Boot**.

Main technologies include:

- Java
- Spring Boot
- Spring Security
- JWT authentication
- PostgreSQL
- RabbitMQ
- Liquibase
- Gradle
- Docker
- OpenAPI / Swagger

The backend follows a domain-oriented structure with separate modules for authentication, clinics, users, doctors, patients, appointments, services, notifications, and API keys.

## Applications

### Admin Panel

The administration application provides a web interface for managing BublAPI and BublAPI Dent resources.

**Production:** https://admin.bublapi.ru

### Dent Demo

The demo application demonstrates how a client application can integrate with BublAPI Dent.

**Production:** https://demo.dent.bublapi.ru

## API

The production BublAPI Dent API is available at:

**https://dent.bublapi.ru**

The API is designed to support both authenticated users and integrations using clinic API keys.

## Website

More information about the project is available on the BublAPI website:

**https://bublapi.ru**

BublAPI Dent:

**https://bublapi.ru/dent/**

## Local development

The project can be started locally using Docker Compose.

```bash
docker compose -p bublapi-local \
  -f deploy/docker-compose.local.yml \
  up -d --build
```

To stop the local environment:

```bash
docker compose -p bublapi-local \
  -f deploy/docker-compose.local.yml \
  down
```

The local stack includes the backend and its infrastructure dependencies, including PostgreSQL and RabbitMQ, together with the BublAPI web applications configured for local development.

## Production deployment

Production deployment is managed using the Compose configuration in:

```text
deploy/docker-compose.prod.yml
```

The production infrastructure runs the BublAPI services as a single Docker Compose project, with Nginx acting as the public reverse proxy.

Persistent PostgreSQL and RabbitMQ data is stored in Docker volumes and is kept independently from application deployments.

## Project status

BublAPI is under active development. BublAPI Dent is the first implementation of the platform and serves as the foundation for further BublAPI products and integrations.

---

**BublAPI** — backend infrastructure for products that need more than just another CRUD API.
