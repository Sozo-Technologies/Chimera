# Contributing Guide

Thank you for contributing to this project!
To keep our codebase clean, readable, and maintainable, we follow a **standardized commit format** using a custom CLI tool called `gitcon`.

---

## Commit Convention (Required)

All commits **must follow this format**:

```
<type>(<scope>): <message>
```

### Example

```
feat(auth): add login system
fix(api): handle null response
```

---

## 🧩 Allowed Commit Types

| Type       | Description                                     |
| ---------- | ----------------------------------------------- |
| `feat`     | A new feature                                   |
| `fix`      | A bug fix                                       |
| `docs`     | Documentation changes                           |
| `style`    | Code formatting (no logic changes)              |
| `refactor` | Code restructuring (no feature or bug fix)      |
| `test`     | Adding or updating tests                        |
| `chore`    | Maintenance tasks (e.g., dependencies, configs) |

❗ Any commit using an **unknown type will be rejected** by our tooling.

---

## Using the `gitcon` CLI Tool

We provide a custom CLI tool to simplify commits and enforce the format.

### Command Syntax

```
gitcon <type> <scope> "<message>"
```

---

### Examples

```
gitcon feat auth "add JWT authentication"
gitcon fix api "resolve crash on null response"
gitcon docs readme "update installation guide"
```

---

### Important Rules

* The **message must be wrapped in quotes**
* The **type must be one of the allowed values**
* The **scope should describe the area of the project** (e.g., `auth`, `api`, `ui`)

---

## Help Command

You can view available types anytime using:

```
gitcon help
```

---

## Workflow Example

1. Make your changes
2. Stage + commit using `gitcon`:

   ```
   gitcon feat ui "add dashboard layout"
   ```
3. Push your branch and create a pull request

---

## Tips

* Keep commit messages **short but descriptive**
* Use **present tense** (e.g., "add", not "added")
* Focus on **what** and **why**, not how

---

## Why This Matters

Following a consistent commit format helps:

* Generate automated changelogs
* Improve team collaboration
* Make debugging and tracking changes easier
