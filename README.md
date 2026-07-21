# Description
This module is the SQL version of the existing SaferOQL module. Since it operates directly on SQL, you can utilize the full range of SQL functionality. While you are not constrained by OQL-specific limitations or dialects, please note that you must explicitly write JOINs for association tables.
The 2WaySQL conversion mechanism utilizes the runtime of DBFlute, a Japanese open-source project. Consequently, the prefix "pmb." is mandatory for parameters within the 2WaySQL.
It looks like this:
>select
>	"name","lastlogin","blocked","active","isanonymous"
>from
>	"system$user"
>/*BEGIN*/
>where
>	/*IF pmb.Name != null*/
>	"name" like  '%' || /*pmb.Name*/'Admin' || '%'
>	/*END*/
>	/*IF pmb.Blocked != null*/
>	and "blocked" = /*pmb.Blocked*/'false'
>	/*END*/
>	/*IF pmb.Active != null*/
>	and "active" = /*pmb.Active*/'true'
>	/*END*/
>	/*IF pmb.IsAnonymous != null*/
>	and "isanonymous" = /*pmb.IsAnonymous*/'false'
>	/*END*/
>/*END*/


# Typical usage scenario

# Dependencies

#Restrictions
