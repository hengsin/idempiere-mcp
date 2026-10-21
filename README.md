# iDempiere MCP Server
This project implements a stateless [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server for [iDempiere ERP](https://www.idempiere.org/). It bridges the gap between AI assistants (like Google Antigravity, Anthropic Claude and Opencode) and iDempiere, enabling autonomous interaction with the ERP system to retrieve data, execute processes, and manage records.
# Supported Protocol Versions
- 2025-11-25
- 2026-07-28

# Dependency
- [iDempiere REST](https://github.com/bxservice/idempiere-rest)

# To Test from Eclipse
- Import idempiere-rest project into your Eclipse workspace.
- Import the org.idempiere.mcp.server project into your Eclipse workspace.
- Create a launch configuration from your existing launch configuration and add both idempiere-rest and org.idempiere.mcp.server project.
- Run the launch configuration.

# Authentication
- Create a new Rest Auth Token record. Copy the auto generated Token and use as HTTP Authorization Header (Bearer `<Token>`).
- Alternatively, use the create_auth_token tool to create authentication token and post as part of the request body (`params.arguments["_authorization_bearer"]`).

# Testing with https://github.com/modelcontextprotocol/inspector
- npx @modelcontextprotocol/inspector
- use http://localhost:8080/mcp/ for Stremable HTTP Transport Type
- Authentication: Enable Authorization and enter Bearer `<Token>`

# Testing with https://antigravity.google/download
- Edit ~/.gemini/config/mcp_config.json
- Streamable HTTP:
```
"mcpServers": {
  "idempiere-mcp-server": {
    "url": "http://localhost:8080/mcp/",
    "timeout": 30000,
    "trust": true,
    "headers": {
      "Authorization": "Bearer <Token>"
    }
  }
}
```
- Multiple token for different tenant:
```
"mcpServers": {
    "iDempiere-System": {
      "url": "http://localhost:8080/mcp/",
      "timeout": 30000,
      "trust": true,
      "headers": {
        "Authorization": "Bearer <Token for System Tenant>"
      }
    },
    "iDempiere-GardenWorld": {
      "url": "http://localhost:8080/mcp/",
      "timeout": 30000,
      "trust": true,
      "headers": {
        "Authorization": "Bearer <Token for GardenWorld tenant>"
      }
    }
  }
}
```
- Authentication with the create_auth_token tool:
   - Remove the "headers" property
   - Example: authenticate idempiere with GardenAdmin, GardenAdmin, clientId:GardenWorld, organizationId:HQ, roleId:GardenWorld Admin, warehouseId:HQ Warehouse
   - In subsequent request, specify using the return token as HTTP Authorization Bearer Token. This work as all method takes an optional argument `_authorization_bearer` as alternative to HTTP Authorization header.
   - Note that out of the box, you can't authenticate with SuperUser,System, clientId:0, roleId:0 due to the role type of the System Administrator role. You need to change the role type value of System Administrator role to empty or create a new System tenant role for REST API with role type = Web Service.
- Note that the trailing "/" in the URL is significant, "http://localhost:8080/mcp/" work but "http://localhost:8080/mcp" doesn't. For some reason, the servlet container will redirect (HTTP 301) "/mcp" to "/mcp/" and that breaks the initialization flow.

# Demo Video
- Search for Business Partner Contact: https://youtu.be/TyNPor3M_pY
- Run process: https://youtu.be/jFjkPRrlzLU
- Create record with Message window: https://youtu.be/dutEDBLrbNg
- Working with server jobs: https://youtu.be/d5yXvsTKSk4

# Environment Variables
- `MCP_CORS_ORIGIN`: Access-Control-Allow-Origin header value. Default: `*`.
- `MCP_HEARTBEAT_INTERVAL_MS`: Interval in milliseconds to send heartbeat (ping) messages. Default: `15000` (15 seconds).
- `MCP_THREAD_POOL_SIZE`: Size of the thread pool for handling requests. Default: `100`.

# Status
- A proof of concept prototype, use with care.

# Build
- With the following layout:
```
idempiere-workspace/
├── idempiere-rest
└── idempiere
```
- At idempiere-workspace, `git clone https://github.com/hengsin/idempiere-mcp.git`.
- At idempiere-workspace/idempiere-mcp, `mvn verify`.
- Build artifact - idempiere-workspace/idempiere-mcp/target/org.idempiere.mcp.server-1.0.0-SNAPSHOT.jar .
- For testing, install org.idempiere.mcp.server-1.0.0-SNAPSHOT.jar using OSGi console of Felix Web Console (Must have install idempiere-rest before this).
