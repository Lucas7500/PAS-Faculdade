# Especificação e Diagrama Arquitetural - Aplicação MVC (Cadastro de Alunos)

Este documento apresenta a especificação arquitetural completa da aplicação de Cadastro de Alunos, desenvolvida sob o padrão arquitetural **MVC (Model-View-Controller)** com **Flask** e **SQLite**.

---

## 1. Visão Geral da Arquitetura (MVC)

```mermaid
graph TD
    Client["👤 Usuário / Navegador Web"]

    subgraph EntryPoint["Ponto de Entrada (Application & Router)"]
        App["app.py<br/><i>(Instância Flask & Regras de URL)</i>"]
    end

    subgraph ControllerLayer["Camada de Controle (Controllers)"]
        subgraph StudentCtrl["controllers/student_controller.py"]
            CtrlIndex["index()<br/><i>GET /</i>"]
            CtrlAdd["add_student()<br/><i>GET /add & POST /add</i>"]
        end
    end

    subgraph ModelLayer["Camada de Modelo (Models & Domínio)"]
        subgraph StudentModule["models/student.py"]
            DBInstance["db: StudentModel<br/><i>(DAO / Acesso a Dados)</i>"]
            StudentEntity["Student<br/><i>(Entidade de Domínio)</i>"]
        end
    end

    subgraph StorageLayer["Persistência de Dados"]
        DB[("students.db<br/><i>(Banco SQLite)</i>")]
    end

    subgraph ViewLayer["Camada de Apresentação (Views / Jinja2)"]
        ViewIndex["views/index.html<br/><i>(Listagem de Alunos)</i>"]
        ViewAdd["views/add.html<br/><i>(Formulário de Cadastro)</i>"]
    end

    %% 1. Requisição do Usuário
    Client -->|"1. Requisição HTTP (GET / | GET /add | POST /add)"| App

    %% 2. Roteamento para Controllers
    App -->|"2a. Despacha GET /"| CtrlIndex
    App -->|"2b. Despacha GET/POST /add"| CtrlAdd

    %% 3. Controller interagindo com Model
    CtrlIndex -->|"3a. db.get_all_students()"| DBInstance
    CtrlAdd -->|"3b. db.add_student(name, course) [POST]"| DBInstance

    %% 4. Model interagindo com SQLite e Entidade
    DBInstance -->|"4. Executa SQL (SELECT / INSERT)"| DB
    DB -->|"5. Retorna dados brutos (Rows)"| DBInstance
    DBInstance -->|"6. Instancia objeto(s)"| StudentEntity
    DBInstance -.->|"7a. Retorna List[Student]"| CtrlIndex
    DBInstance -.->|"7b. Retorna Student criado"| CtrlAdd

    %% 8. Controller renderizando Views ou Redirecionando
    CtrlIndex -->|"8a. render_template('index.html', students)"| ViewIndex
    CtrlAdd -->|"8b. render_template('add.html') [GET]"| ViewAdd
    CtrlAdd -->|"8c. redirect(url_for('index')) [POST]"| Client

    %% 9. Resposta HTML final ao cliente
    ViewIndex -->|"9a. Resposta HTTP (HTML renderizado)"| Client
    ViewAdd -->|"9b. Resposta HTTP (HTML renderizado)"| Client

    %% Estilos visuais
    classDef clientStyle fill:#0288d1,stroke:#ffffff,stroke-width:2px,color:#ffffff;
    classDef routerStyle fill:#f57c00,stroke:#ffffff,stroke-width:2px,color:#ffffff;
    classDef controllerStyle fill:#2e7d32,stroke:#ffffff,stroke-width:2px,color:#ffffff;
    classDef modelStyle fill:#6a1b9a,stroke:#ffffff,stroke-width:2px,color:#ffffff;
    classDef viewStyle fill:#fbc02d,stroke:#000000,stroke-width:2px,color:#000000;
    classDef dbStyle fill:#4527a0,stroke:#ffffff,stroke-width:2px,color:#ffffff;

    class Client clientStyle;
    class App routerStyle;
    class CtrlIndex,CtrlAdd controllerStyle;
    class DBInstance,StudentEntity modelStyle;
    class ViewIndex,ViewAdd viewStyle;
    class DB dbStyle;
```

---

## 2. Diagramas de Sequência (Fluxos da Aplicação)

### 2.1. Fluxo de Inicialização (Bootstrap / Startup)

Demonstra como o servidor carrega os módulos, cria as tabelas no SQLite se não existirem e disponibiliza as rotas.

```mermaid
sequenceDiagram
    autonumber
    participant CLI as Terminal / Usuário
    participant App as app.py
    participant Controller as student_controller.py
    participant Model as models/student.py
    participant DB as SQLite (students.db)

    CLI->>App: python app.py
    App->>Controller: import index, add_student
    Controller->>Model: import db
    Model->>Model: db = StudentModel('students.db')
    activate Model
    Model->>Model: _create_table()
    Model->>DB: CREATE TABLE IF NOT EXISTS students (...)
    DB-->>Model: Tabela pronta / verificada
    deactivate Model
    App->>App: Flask(__name__, template_folder='views')
    App->>App: add_url_rule('/', 'index', ...)
    App->>App: add_url_rule('/add', 'add_student', ...)
    App->>CLI: Servidor rodando em http://127.0.0.1:5000 (debug=True)
```

---

### 2.2. Fluxo de Listagem de Alunos (`GET /`)

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuário / Navegador
    participant Router as app.py
    participant Controller as student_controller.py (index)
    participant Model as StudentModel (db)
    participant DB as SQLite (students.db)
    participant View as views/index.html

    Usuario->>Router: GET /
    Router->>Controller: index()
    Controller->>Model: get_all_students()
    Model->>DB: SELECT id, name, course FROM students
    DB-->>Model: Linhas do banco (sqlite3.Row)
    Model-->>Controller: Lista de instâncias [Student(id, name, course)]
    Controller->>View: render_template('index.html', students=students)
    View-->>Usuario: Resposta HTTP 200 (HTML com tabela renderizada)
```

---

### 2.3. Fluxo de Acesso ao Formulário de Cadastro (`GET /add`)

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuário / Navegador
    participant Router as app.py
    participant Controller as student_controller.py (add_student)
    participant View as views/add.html

    Usuario->>Router: GET /add
    Router->>Controller: add_student() (request.method == 'GET')
    Controller->>View: render_template('add.html')
    View-->>Usuario: Resposta HTTP 200 (HTML com formulário)
```

---

### 2.4. Fluxo de Cadastro de Novo Aluno (`POST /add`) — Padrão Post/Redirect/Get (PRG)

```mermaid
sequenceDiagram
    autonumber
    actor Usuario as Usuário / Navegador
    participant Router as app.py
    participant Controller as student_controller.py (add_student)
    participant Model as StudentModel (db)
    participant DB as SQLite (students.db)

    Usuario->>Router: POST /add (form-data: name, course)
    Router->>Controller: add_student() (request.method == 'POST')
    Controller->>Model: add_student(name, course)
    Model->>DB: INSERT INTO students (name, course) VALUES (?, ?)
    DB-->>Model: cursor.lastrowid
    Model-->>Controller: Instância Student(lastrowid, name, course)
    Controller-->>Usuario: Resposta HTTP 302 Redirect para '/'
    Usuario->>Router: GET / (Recarrega listagem com novo registro)
```

---

## 3. Diagrama de Classes e Módulos

```mermaid
classDiagram
    direction TB

    class Student {
        +int id
        +string name
        +string course
        +__init__(student_id: int, name: string, course: string)
    }

    class StudentModel {
        +string db_name
        +__init__(db_name: string)
        -_get_connection() sqlite3.Connection
        -_create_table() void
        +get_all_students() List~Student~
        +add_student(name: string, course: string) Student
    }

    class StudentController {
        <<module: student_controller.py>>
        +index() Response
        +add_student() Response
    }

    class AppRouter {
        <<module: app.py>>
        +app: Flask
        +run(debug: bool)
    }

    class SQLiteDatabase {
        <<database: students.db>>
        +TABLE students
    }

    StudentModel ..> Student : instancia e popula
    StudentModel --> SQLiteDatabase : executa queries SQL
    StudentController --> StudentModel : consome singleton 'db'
    StudentController ..> Student : manipula instâncias
    AppRouter --> StudentController : despacha rotas para
```

---

## 4. Diagrama do Modelo de Dados (Entidade-Relacionamento)

```mermaid
erDiagram
    STUDENTS {
        INTEGER id PK "PRIMARY KEY AUTOINCREMENT"
        TEXT name "NOT NULL"
        TEXT course "NOT NULL"
    }
```