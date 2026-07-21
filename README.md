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
- It is recommended to organize files by creating subfolders based on module names and further categorizing them by function.  
- Ensure the file encoding is UTF-8 (without BOM).  
- Prefix filenames with "INS_", "SEL_", "UPD_", or "DEL_" to correspond with CRUD operations.  
- Due to Mendix's architecture regarding automatic ID generation, only association entities can be inserted using this function.  
- In Linux-based environments such as MxCloud, the file system is case-sensitive; therefore, exercise caution when specifying folder and file names.  
A simple sample is included in the `_Sample` folder, so we recommend checking it first.

# Typical usage scenario

# Dependencies

#Restrictions
