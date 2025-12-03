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

# Demo Video
- Searc for Business Partner Contact: https://youtu.be/TyNPor3M_pY
- Run process: https://youtu.be/jFjkPRrlzLU
- Create record with Message window: https://youtu.be/dutEDBLrbNg
- Working with server jobs: https://youtu.be/d5yXvsTKSk4

# Issues
- Can't get it to work with Claude Desktop

# Status
- A proof of concept prototype, use with care.
