#curl -u Admin:admin1234 -X PUT --data-binary "@practices.xml" -H "Content-Type: application/xml" http://localhost:9090/rest/wikis/xwiki/spaces/Main/pages/Practices

#curl -u Admin:admin1234 -X PUT --data-binary "@create-rest-spec.xml" -H "Content-Type: application/xml" http://localhost:9090/rest/wikis/xwiki/spaces/Main/pages/rest-spec

curl -u Admin:admin1234 -X PUT --data-binary "@create-business-spec.xml" -H "Content-Type: application/xml" http://localhost:9090/rest/wikis/xwiki/spaces/Main/pages/business-spec