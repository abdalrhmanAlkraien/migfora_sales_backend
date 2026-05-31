Companies
POST   /api/v1/companies              create
GET    /api/v1/companies              list (search, filter, pageable)
GET    /api/v1/companies/{id}         get by id
PATCH  /api/v1/companies/{id}         update
DELETE /api/v1/companies/{id}         delete (admin only)

Contacts
POST   /api/v1/contacts               create
GET    /api/v1/contacts/company/{id}  list by company
GET    /api/v1/contacts/{id}          get by id
PATCH  /api/v1/contacts/{id}          update
DELETE /api/v1/contacts/{id}          delete (admin only)

Investigations
POST   /api/v1/investigations              trigger
GET    /api/v1/investigations/company/{id} list by company
GET    /api/v1/investigations/{id}         full result
DELETE /api/v1/investigations/{id}         delete (admin only)

Reports
POST   /api/v1/reports                    generate
GET    /api/v1/reports/company/{id}        list by company
GET    /api/v1/reports/{id}               get with download url
DELETE /api/v1/reports/{id}               delete (admin only)

Auth
POST   /api/v1/auth/users             create sales user (admin only)