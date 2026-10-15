# TripNest

A real-time collaborative travel planning and expense management platform that enables groups to organize trips, coordinate itineraries, manage shared expenses, settle debts, and collaborate seamlessly in real time.

## Overview

TripNest is designed for travelers who plan and manage trips together. The platform combines itinerary planning, real-time collaboration, expense tracking, debt settlement, document sharing, media management, and role-based access control into a unified workspace.

The system focuses on distributed system concepts including:

- Real-time collaboration using WebSockets
- Multi-user itinerary synchronization
- Atomic financial transactions
- Debt simplification algorithms
- Role-based access control (RBAC)
- Secure cloud file storage
- Notification broadcasting
- Distributed caching

---

## Key Features

### Authentication & Security

- JWT-based authentication
- Refresh token support
- Google OAuth 2.0 login
- BCrypt password hashing
- Role-based authorization

### Trip Management

- Create and manage trips
- Invite members using secure links
- Multiple member roles
- Ownership transfer support
- Collaborative workspace management

### Real-Time Collaboration

- Live itinerary updates
- Instant activity synchronization
- Real-time chat
- WebSocket/STOMP communication
- Multi-user conflict handling

### Itinerary Planning

- Day-wise trip planning
- Activity scheduling
- Drag-and-drop reordering
- Optimistic locking for concurrent updates
- LexoRank-based activity ordering

### Expense Management

- Shared expense tracking
- Expense splitting
- Settlement recording
- Financial ledger management

### Debt Simplification

- Automatic debt optimization
- Reduced transaction settlements
- Balance calculation engine
- Group expense reconciliation

### Cloud Storage

- Secure AWS S3 integration
- Travel document management
- Media gallery support
- Pre-signed upload URLs
- Personal travel wallet

### Notifications

- Real-time notifications
- Event broadcasting
- User-targeted messaging
- Persistent notification history

---

## System Architecture

### Backend

- Java 21
- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- Spring WebSocket
- STOMP Messaging

### Database

- PostgreSQL 15+
- Flyway Migrations

### Caching

- Redis

### Storage

- AWS S3

### Frontend

- React 18
- Tailwind CSS
- TanStack Query

---

## Architecture Highlights

### Real-Time Synchronization

TripNest uses STOMP over WebSockets to synchronize trip activities, chat messages, notifications, and itinerary updates across connected clients.

### Financial Integrity

The expense subsystem maintains transactional consistency for expense creation, settlement recording, and balance computation.

### Contextual RBAC

Permissions are enforced at the trip level with three roles:

| Role | Permissions |
|--------|------------|
| Owner | Full control |
| Admin | Management operations |
| Member | Collaboration actions |

### Distributed Design Patterns

- Write-Then-Broadcast Notifications
- Optimistic Locking
- Idempotent Operations
- Pre-Signed Upload Strategy
- Cache-Aside Pattern
- Event-Driven Updates

---

## Project Structure

```text
tripnest/
├── backend/
│   ├── auth/
│   ├── trips/
│   ├── itinerary/
│   ├── expenses/
│   ├── settlements/
│   ├── notifications/
│   ├── media/
│   └── websocket/
│
├── frontend/
│   ├── components/
│   ├── pages/
│   ├── services/
│   ├── hooks/
│   └── store/
│
└── database/
    ├── migrations/
    └── seed/
```

---

## Learning Outcomes

This project demonstrates:

- Distributed systems concepts
- Full-stack application design
- Database modeling
- WebSocket communication
- Cloud storage integration
- Authentication & authorization
- Financial transaction handling
- Concurrent data management
- System architecture design

---

## Future Enhancements

- AI-powered trip recommendations
- Offline synchronization
- Mobile applications
- Advanced analytics dashboard
- Travel booking integrations
- Smart budget prediction

---

## Installation

### Backend

```bash
git clone https://github.com/yourusername/tripnest.git
cd tripnest/backend
```

Configure:

```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=

jwt.secret=

aws.accessKey=
aws.secretKey=

redis.host=
redis.port=
```

Run:

```bash
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

---

## License

MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software.
