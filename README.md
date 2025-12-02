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
- Edit ~/.gemini/settings.json, add
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
- http://localhost:8080/mcp/streaming not working with gemini-cli
- demo https://drive.google.com/file/d/1AkfHYQQo0AHCRR3pqkGJaxsPaeNMHIb3/view?usp=sharing

# Issues
- Can't get it to work with Claude Desktop

# Status
- A proof of concept prototype, use with care.
