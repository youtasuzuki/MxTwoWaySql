
select
	emp."id" as IdValue
	,dept."id" as IdValueOfDept
	,area."id" as IdValueOfArea
from
	"myfirstmodule$employee" emp
	left outer join "myfirstmodule$employee_department" emp_dept on emp_dept."myfirstmodule$employeeid"=emp."id"
	left outer join "myfirstmodule$department" dept on dept."id"=emp_dept."myfirstmodule$departmentid"
	left outer join "myfirstmodule$department_area" dept_area on dept_area."myfirstmodule$departmentid"=dept."id"
	left outer join  "myfirstmodule$area" area on area."id"=dept_area."myfirstmodule$areaid"
/*BEGIN*/
where
	/*IF pmb.Name != null*/
	emp."name" like /*pmb.Name*/'bbb'
	/*END*/
/*END*/
order by emp."name" desc
