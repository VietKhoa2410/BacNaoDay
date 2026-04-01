# Structure Requirements

## 1. Project Goal
Build a basic, maintainable web application with a clear separation of frontend, backend, and shared resources.

## 2. Folder Structure
```text
BacNaoDay/
├── frontend/
│   ├── public/
│   │   └── favicon.ico
│   └── src/
│       ├── assets/
│       ├── components/
│       ├── pages/
│       ├── services/
│       ├── styles/
│       ├── App.js
│       └── main.js
├── backend/
│   ├── src/
│   │   ├── config/
│   │   ├── controllers/
│   │   ├── repositories/
│   │   │   ├──implements   
│   │   ├── models/
│   │   ├── routes/
│   │   ├── services/
│   │   │   ├──implements   
│   │   ├── app.js
│   │   └── server.js
│   ├── tests/
│   └── package.json
├── docs
│   ├── features
├── .env.example
├── .gitignore
├── README.md
└── docker-compose.yml
```

