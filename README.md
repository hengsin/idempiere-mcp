# iDempiere MCP Server
This project implements a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server for [iDempiere ERP](https://www.idempiere.org/). It bridges the gap between AI assistants (like Google Gemini and Anthropic Claude) and iDempiere, enabling autonomous interaction with the ERP system to retrieve data, execute processes, and manage records.

# Dependency
- [iDempiere REST](https://github.com/bxservice/idempiere-rest)

# To Test from Eclipse
- import idempiere-rest project into your Eclipse workspace.
- Import the org.idempiere.mcp.server project into your Eclipse workspace.
- Create a launch configuration from your existing launch configuration and add both idempiere-rest and org.idempiere.mcp.server project.
- Run the launch configuration.

# Testing
- Create a new Rest Auth Token record. Copy the auto generated Token.

# Testing with https://github.com/modelcontextprotocol/inspector
- npx @modelcontextprotocol/inspector
- use http://localhost:8080/mcp/sse for SSE Transport Type
- use http://localhost:8080/mcp/streaming for Stremable HTTP Transport Type
- Authentication: Enable Authorization and enter Bearer `<Token>`

# Testing with https://github.com/google-gemini/gemini-cli
- Edit ~/.gemini/settings.json
- SSE:
```
"mcpServers": {
  "iDempiere": {
    "url": "http://localhost:8080/mcp/sse",
    "timeout": 30000,
    "trust": true,
    "headers": {
      "Authorization": "Bearer <Token>"
    }
  }
}
```
- Streamable HTTP:
```
"mcpServers": {
  "iDempiere": {
    "httpUrl": "http://localhost:8080/mcp/streaming",
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
      "httpUrl": "http://localhost:8080/mcp/streaming",
      "timeout": 30000,
      "trust": true,
      "headers": {
        "Authorization": "Bearer <Token for System Tenant>"
      }
    },
    "iDempiere-GardenWorld": {
      "httpUrl": "http://localhost:8080/mcp/streaming",
      "timeout": 30000,
      "trust": true,
      "headers": {
        "Authorization": "Bearer <Token for GardenWorld tenant>"
      }
    }
  },
  "mcp": {
    "excluded":["iDempiere-System"]
  }
}
```
- With multiple token, you can only use one at a time and use "excluded" to disable the other. You need to restart Gemini CLI after changing the "excluded" value in settings.json file.
- Install https://github.com/castle-studio-work/geminicli-manage-addon for a command line tool to manage multiple MCP Server instance for Gemini CLI

# Demo Video
- Search for Business Partner Contact: https://youtu.be/TyNPor3M_pY
- Run process: https://youtu.be/jFjkPRrlzLU
- Create record with Message window: https://youtu.be/dutEDBLrbNg
- Working with server jobs: https://youtu.be/d5yXvsTKSk4

# Environment Variables
- `IDEMPIERE_API_URL`: The base URL for the iDempiere REST API. Default: `http://localhost:8080/api/v1`
- `MCP_CLEANUP_INTERVAL_MINUTES`: Interval in minutes to clean up expired sessions.
- `MCP_CLEANUP_INTERVAL_MS`: Interval in milliseconds to clean up expired sessions. Default: `600000` (10 minutes).
- `MCP_CORS_ORIGIN`: Access-Control-Allow-Origin header value. Default: `*`.
- `MCP_HEARTBEAT_INTERVAL_MS`: Interval in milliseconds to send heartbeat (ping) messages. Default: `15000` (15 seconds).
- `MCP_PROTOCOL_VERSION`: The version of the Model Context Protocol supported. Default: `2025-06-18`.
- `MCP_STREAMING_SESSION_TTL_MINUTES`: Time-to-live for streaming sessions in minutes.
- `MCP_STREAMING_SESSION_TTL_MS`: Time-to-live for streaming sessions in milliseconds. Default: `1800000` (30 minutes).
- `MCP_THREAD_POOL_SIZE`: Size of the thread pool for handling requests. Default: `100`.

# Issues
- Can't get it to work with Claude Desktop

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
