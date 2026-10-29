# iDempiere MCP Skills Plugin

This repository provides an autonomous agent skills plugin for interacting with [iDempiere ERP](https://www.idempiere.org/) via the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/).

It integrates with AI coding assistants (such as Google Antigravity, Anthropic Claude, and custom MCP clients) to perform complex Application Dictionary engineering, authentication, system configuration, and 2Pack package imports.

---

## Skills Documentation

Detailed documentation for all available skills can be found in:

👉 **[skills/README.md](skills/README.md)**

### Available Skills Summary

| Skill | Description | Location |
|---|---|---|
| **`idempiere-authenticate-system`** | Authenticate to System tenant (Client 0) for dictionary maintenance | [skills/idempiere-authenticate-system](skills/idempiere-authenticate-system/SKILL.md) |
| **`idempiere-authenticate-tenant`** | Authenticate to an application tenant (e.g. GardenWorld) | [skills/idempiere-authenticate-tenant](skills/idempiere-authenticate-tenant/SKILL.md) |
| **`idempiere-create-table`** | Create a new table (`AD_Table`) with standard columns, physical DDL sync (including optional `_Trl` table), and key/unique indexes | [skills/idempiere-create-table](skills/idempiere-create-table/SKILL.md) |
| **`idempiere-create-element`** | Create a new reusable Application Dictionary element (`AD_Element`) | [skills/idempiere-create-element](skills/idempiere-create-element/SKILL.md) |
| **`idempiere-add-column-to-table`** | Add a column to an iDempiere table, sync physical DDL, and optionally index/gen model | [skills/idempiere-add-column-to-table](skills/idempiere-add-column-to-table/SKILL.md) |
| **`idempiere-add-table-index`** | Create and validate a database table index (`AD_TableIndex`) and columns (`AD_IndexColumn`) | [skills/idempiere-add-table-index](skills/idempiere-add-table-index/SKILL.md) |
| **`idempiere-generate-model`** | Generate Java model interface (`I_*`) and implementation class (`X_*`) for tables via `generatemodel` process | [skills/idempiere-generate-model](skills/idempiere-generate-model/SKILL.md) |
| **`idempiere-add-field-to-tab`** | Add a field (`AD_Field`) to an existing window tab layout | [skills/idempiere-add-field-to-tab](skills/idempiere-add-field-to-tab/SKILL.md) |
| **`idempiere-pack-in`** | Import a 2Pack archive (.zip) into iDempiere via `PackIn` process | [skills/idempiere-pack-in](skills/idempiere-pack-in/SKILL.md) |
| **`idempiere-set-centralized-id-config`** | Configure centralized dictionary sequence credentials in `AD_SysConfig` | [skills/idempiere-set-centralized-id-config](skills/idempiere-set-centralized-id-config/SKILL.md) |

---

## Configuration & Environment Variables

| Variable | Description | Default | Target Skill |
|---|---|---|---|
| `IDEMPIERE_ADMINISTRATOR_USER` | System admin username | `SuperUser` | `idempiere-authenticate-system` |
| `IDEMPIERE_ADMINISTRATOR_PASS` | System admin password | `System` | `idempiere-authenticate-system` |
| `IDEMPIERE_ADMINISTRATOR_ROLE` | System admin role ID or name | `0` | `idempiere-authenticate-system` |
| `IDEMPIERE_TENANT_USER` | Tenant username | `GardenAdmin` | `idempiere-authenticate-tenant` |
| `IDEMPIERE_TENANT_PASS` | Tenant password | `GardenAdmin` | `idempiere-authenticate-tenant` |
| `IDEMPIERE_TENANT` | Tenant identifier | `GardenWorld` | `idempiere-authenticate-tenant` |
| `IDEMPIERE_TENANT_ROLE` | Tenant role identifier | `GardenWorld Admin` | `idempiere-authenticate-tenant` |
| `IDEMPIERE_TENANT_ORG` | Tenant organization ID | `0` | `idempiere-authenticate-tenant` |
| `IDEMPIERE_TENANT_WAREHOUSE` | Tenant warehouse ID | `0` | `idempiere-authenticate-tenant` |
| `IDEMPIERE-CENTRALIZED-ID-USER` | Centralized sequence ID developer user | *(Prompted)* | `idempiere-set-centralized-id-config` |
| `IDEMPIERE-CENTRALIZED-ID-PASS` | Centralized sequence ID developer password | *(Prompted)* | `idempiere-set-centralized-id-config` |

---

## Related Projects
- **iDempiere MCP Server**: `../org.idempiere.mcp.server`
- **MCP Tools Specification**: `../MCP_TOOLS.md`
