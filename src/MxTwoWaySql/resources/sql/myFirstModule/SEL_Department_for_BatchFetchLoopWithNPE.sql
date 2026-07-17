
select
	emp."id" as IdValue
	,dept."name" as DeptName
from
	"myfirstmodule$employee" emp
	left outer join "myfirstmodule$employee_department" emp_dept on emp_dept."myfirstmodule$employeeid"=emp."id"
	left outer join "myfirstmodule$department" dept on dept."id"=emp_dept."myfirstmodule$departmentid"
/*BEGIN*/
where
	/*IF pmb.IdList != null*/
	emp."id" in /*pmb.IdList*/(12103423998568208,12103423998568207)
	/*END*/
/*END*/
