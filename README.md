# Description
This module is the SQL version of the existing SaferOQL module. Since it operates directly on SQL, you can utilize the full range of SQL functionality. While you are not constrained by OQL-specific limitations or dialects, please note that you must explicitly write JOINs for association tables.  

The 2WaySQL conversion mechanism utilizes the runtime of DBFlute, a Japanese open-source project. Consequently, the prefix "pmb." is mandatory for parameters within the 2WaySQL.
It looks like this:
```
select
	"name","lastlogin","blocked","active","isanonymous"
from
	"system$user"
/*BEGIN*/
where
	/*IF pmb.Name != null*/
	"name" like  '%' || /*pmb.Name*/'Admin' || '%'
	/*END*/
	/*IF pmb.Blocked != null*/
	and "blocked" = /*pmb.Blocked*/'false'
	/*END*/
	/*IF pmb.Active != null*/
	and "active" = /*pmb.Active*/'true'
	/*END*/
	/*IF pmb.IsAnonymous != null*/
	and "isanonymous" = /*pmb.IsAnonymous*/'false'
	/*END*/
/*END*/
```
- For configure parameters such as 'pmb.Name' you can use parameter-adding actions like AddStringParameter.
- It is recommended to organize files by creating subfolders based on module names and further categorizing them by function.  
- Ensure the file encoding is UTF-8 (without BOM).  
- Prefix filenames with "INS_", "SEL_", "UPD_", or "DEL_" to correspond with CRUD operations.  
- Due to Mendix's architecture regarding automatic ID generation, only association entities can be inserted using this function.  
- In Linux-based environments such as MxCloud, the file system is case-sensitive; therefore, exercise caution when specifying folder and file names.
- In this module, the DBFlute runtime is used solely to parse 2WaySQL and convert it into standard SQL; therefore, other DBFlute features are not supported.
- Transactions for access to the internal database use the microflow's context.


You can access not only the Mendix app's internal database but also external databases.
To access an external database, you must register the external data source using the `RegisterExternalDataSource` action within the `AfterStartUp` event when the app starts.
Then, specify the external data source directive within the 2WaySQL as shown below.
```
-- @YourExtDataSourceName@ 
-- ^^^^^^^^^^^^^^^^^^^^^^^ It's the external data source directive
select
 name, description
from
 your_external_table
```
Note:
- Transaction control defaults to auto-commit when accessing external data sources. If you need to use transactions for external database access, you can control the start and end/abend of the transaction from a microflow using the actions located in the `JavaActions/Transaction` folder.
- The JDBC driver to be used must be downloaded using Studio Pro's Java dependency definitions.

A simple sample is included in the `_Sample` folder, so we recommend checking it first.

# Advanced usages
- 'in' clause with AddListParameter action
```
select
	"name","address","birthdate","id" as IdValue
from
	"yourModule$employee"
/*BEGIN*/
where
    -- Before calling the RetrieveByTwoWaySql action,
    --  use the AddListParameter action to set a list containing three TwoWaySQL.StringValue objects -with the values ​​'Liam', 'Noah', and 'James'—under the name 'NameList'.
	/*IF pmb.NameList != null*/
	"name" in /*pmb.NameList*/('Liam','Noah','James')
	/*END*/
/*END*/
```
- Perform a batch insert using the 2Way 'FOR-NEXT-END' construct
```
insert into employee_on_external_db
(
    name,
    address
)
values
-- Before calling UpdateByTwoWaySql,
--  use the AddListParameter action to set the list of employee entities (from your application) that you wish to insert in bulk, under the name "EmployeeList".
-- Since there is a limit on the length of SQL statements in the DBMS, it is recommended to split the data into appropriate batch sizes for execution.
-- You cannot insert data into the Mendix internal database.
/*FOR pmb.EmployeeList*/
/*NEXT ','*/( 
    /*#current.name*/'test_name',
    /*#current.address*/'test_address'
)
/*END*/

```

# Restrictions
- To maintain simplicity, the current internal implementation for creating a DataSource supports only the standard, classic method using a username and password with `HikariDataSource`. However, since the public `twowaysql.integration.putExtDataSource(name, dataSource)` method allows you to configure a data source externally, you can set up your own data source —such as one authenticated via mTLS/TCPO, Auth 2.0, etc— from outside the library. Please refer to the sample implementation of RegisterOracleMtlsExternalDataSource.

# Dependencies
- dbflute-runtime
- commons-csv
- HikariCP
