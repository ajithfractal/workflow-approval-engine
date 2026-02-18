# External User API Contract

## Overview

This document defines the contract that external applications must implement to provide user data to the workflow engine. The workflow engine will call this API to fetch users for approver selection.

## Endpoint

**Base URL:** Provided during application registration in `apiEndpoints.userApi`

**Full Endpoint:** `GET {apiEndpoints.userApi}/users`

### Example
If `apiEndpoints.userApi` is `https://app1.example.com/api`, the full endpoint would be:
```
GET https://app1.example.com/api/users
```

## Authentication

The workflow engine will send the API key (provided during registration) as a Bearer token in the Authorization header:

```
Authorization: Bearer {apiKey}
```

## Query Parameters

| Parameter | Type   | Required | Description                                    |
|-----------|--------|----------|------------------------------------------------|
| `search`  | String | No       | Search by name or email (partial match)       |
| `role`    | String | No       | Filter users by role                          |
| `limit`   | Integer| No       | Maximum number of results (default: 100)      |

### Examples

```
GET /users?search=john
GET /users?role=Manager&limit=50
GET /users?search=doe&role=Analyst
```

## Response Format

### Success Response (200 OK)

```json
{
  "users": [
    {
      "user_id": "user@example.com",
      "name": "John Doe",
      "email": "user@example.com",
      "role": "Manager",
      "manager_id": "manager@example.com"
    },
    {
      "user_id": "jane@example.com",
      "name": "Jane Smith",
      "email": "jane@example.com",
      "role": "Analyst",
      "manager_email": "user@example.com"
    }
  ],
  "total": 50
}
```

### Response Fields

| Field         | Type   | Required | Description                                    |
|---------------|--------|----------|------------------------------------------------|
| `users`       | Array  | Yes      | List of user objects                           |
| `users[].user_id` | String | Yes  | Unique user identifier (email address)         |
| `users[].name`    | String | Yes  | User's display name                            |
| `users[].email`   | String | Yes  | User's email address                           |
| `users[].role`    | String | No   | User's role/title                              |
| `users[].manager_id` | String | No | Manager's user ID (email address, for delegation) |
| `total`      | Integer| No   | Total number of users matching criteria        |

### Error Responses

#### 401 Unauthorized
```json
{
  "error": "Invalid API key"
}
```

#### 400 Bad Request
```json
{
  "error": "Invalid request parameters"
}
```

#### 500 Internal Server Error
```json
{
  "error": "Internal server error"
}
```

## Field Requirements

### user_id
- **Type:** String (email format)
- **Required:** Yes
- **Description:** Unique identifier for the user. Must be an email address.
- **Example:** `"user@example.com"`

### name
- **Type:** String
- **Required:** Yes
- **Description:** User's display name for UI presentation.
- **Example:** `"John Doe"`

### email
- **Type:** String (email format)
- **Required:** Yes
- **Description:** User's email address. Should match `user_id`.
- **Example:** `"user@example.com"`

### role
- **Type:** String
- **Required:** No
- **Description:** User's role or job title.
- **Example:** `"Manager"`, `"Analyst"`, `"Director"`

### manager_id
- **Type:** String (email format)
- **Required:** No
- **Description:** User ID (email address) of the user's manager. Used for delegation and manager chain resolution.
- **Example:** `"manager@example.com"`

## Implementation Notes

1. **Email as Unique ID:** The `user_id` field must be an email address and serves as the unique identifier.

2. **Search Functionality:** The `search` parameter should search across both `name` and `email` fields (partial match).

3. **Role Filtering:** The `role` parameter should perform exact or case-insensitive matching.

4. **Pagination:** If `limit` is not provided, return up to 100 results. If more results exist, the `total` field should reflect the actual count.

5. **Empty Results:** Return an empty array if no users match the criteria:
   ```json
   {
     "users": [],
     "total": 0
   }
   ```

6. **Performance:** The API should respond within 2 seconds for typical queries.

## Example Implementations

### Node.js/Express Example

```javascript
app.get('/api/users', authenticateApiKey, async (req, res) => {
  const { search, role, limit = 100 } = req.query;
  
  let query = User.find();
  
  if (search) {
    query = query.or([
      { name: { $regex: search, $options: 'i' } },
      { email: { $regex: search, $options: 'i' } }
    ]);
  }
  
  if (role) {
    query = query.where({ role: role });
  }
  
  const users = await query.limit(parseInt(limit));
  const total = await User.countDocuments(query.getQuery());
  
  res.json({
    users: users.map(u => ({
      user_id: u.email,
      name: u.fullName,
      email: u.email,
      role: u.role,
      manager_email: u.manager?.email
    })),
    total
  });
});
```

### Java/Spring Boot Example

```java
@GetMapping("/users")
public ResponseEntity<ExternalUserResponse> getUsers(
    @RequestParam(required = false) String search,
    @RequestParam(required = false) String role,
    @RequestParam(defaultValue = "100") int limit) {
    
    List<User> users = userService.findUsers(search, role, limit);
    
    ExternalUserResponse response = ExternalUserResponse.builder()
        .users(users.stream().map(this::toExternalUser).collect(Collectors.toList()))
        .total(userService.countUsers(search, role))
        .build();
        
    return ResponseEntity.ok(response);
}
```

## Testing

External applications should test their implementation with:

1. **Basic Request:**
   ```
   GET /users
   Authorization: Bearer {apiKey}
   ```

2. **Search Query:**
   ```
   GET /users?search=john
   Authorization: Bearer {apiKey}
   ```

3. **Role Filter:**
   ```
   GET /users?role=Manager
   Authorization: Bearer {apiKey}
   ```

4. **Combined Filters:**
   ```
   GET /users?search=doe&role=Analyst&limit=10
   Authorization: Bearer {apiKey}
   ```

5. **Invalid API Key:**
   ```
   GET /users
   Authorization: Bearer invalid-key
   ```
   Should return 401 Unauthorized

## Support

For questions or clarifications about this contract, please contact the workflow engine team.
